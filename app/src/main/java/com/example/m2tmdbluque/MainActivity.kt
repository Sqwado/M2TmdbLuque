package com.example.m2tmdbluque

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.RecyclerView
import com.example.m2tmdbluque.api.TmdbClient
import com.example.m2tmdbluque.databinding.ActivityMainBinding
import com.example.m2tmdbluque.model.Person
import com.example.m2tmdbluque.model.PopularPeopleResponse
import com.example.m2tmdbluque.ui.ImagePrefetcher
import com.example.m2tmdbluque.ui.PersonAdapter
import com.example.m2tmdbluque.util.NetworkMonitor
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.IOException
import java.net.UnknownHostException

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var networkMonitor: NetworkMonitor
    private val personAdapter = PersonAdapter { person ->
        startActivity(PersonDetailActivity.newIntent(this, person))
    }
    private val people = mutableListOf<Person>()

    private var currentPage = 0
    private var totalPages = Int.MAX_VALUE
    private var totalResults = 0
    private var isLoading = false
    private var isPrefetching = false
    private var hasMore = true
    private var searchQuery = ""
    private var isSearchMode = false
    private var isOnline = true
    private var pendingRetryPage: Int? = null
    private var activeCall: Call<PopularPeopleResponse>? = null
    private var prefetchCall: Call<PopularPeopleResponse>? = null
    /** Page déjà téléchargée, prête à être injectée sans spinner. */
    private var prefetchedPage: PopularPeopleResponse? = null
    private val prefetchThreshold by lazy { resources.getInteger(R.integer.prefetch_threshold) }
    private val scrollTopThreshold by lazy { resources.getInteger(R.integer.scroll_top_threshold) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        networkMonitor = NetworkMonitor(this)

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout(),
            )
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }

        setupRecyclerView()
        setupSearch()
        setupScrollToTop()
        setupOfflineBanner()
        setupNetworkMonitor()
        loadPeople(page = 1)
    }

    override fun onDestroy() {
        networkMonitor.stop()
        cancelAllCalls()
        super.onDestroy()
    }

    private fun setupRecyclerView() {
        val spanCount = resources.getInteger(R.integer.list_span_count)
        val layoutManager = GridLayoutManager(this, spanCount)
        binding.recyclerPeople.layoutManager = layoutManager
        binding.recyclerPeople.adapter = personAdapter

        binding.recyclerPeople.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (dy != 0 && binding.editSearch.hasFocus()) hideKeyboard()

                binding.fabScrollTop.isVisible =
                    layoutManager.findFirstVisibleItemPosition() > scrollTopThreshold

                if (dy <= 0 || !hasMore) return

                val lastVisible = layoutManager.findLastVisibleItemPosition()
                if (lastVisible >= layoutManager.itemCount - prefetchThreshold) {
                    consumeNextPage()
                }
            }
        })
    }

    private fun setupSearch() {
        binding.editSearch.doAfterTextChanged { text ->
            if (text?.toString()?.trim().isNullOrEmpty() && isSearchMode) {
                resetToPopular()
            }
        }

        binding.editSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId != EditorInfo.IME_ACTION_SEARCH) return@setOnEditorActionListener false
            hideKeyboard()
            val query = binding.editSearch.text?.toString()?.trim().orEmpty()
            if (query.isNotEmpty()) startSearch(query) else resetToPopular()
            true
        }
    }

    private fun hideKeyboard() {
        val focus = currentFocus ?: binding.editSearch
        focus.clearFocus()
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(focus.windowToken, 0)
    }

    private fun setupScrollToTop() {
        binding.fabScrollTop.setOnClickListener { scrollToTop() }
    }

    private fun setupOfflineBanner() {
        binding.buttonRetry.setOnClickListener { retryPendingOrReload() }
    }

    private fun setupNetworkMonitor() {
        networkMonitor.start { online ->
            runOnUiThread {
                val wasOffline = !isOnline
                isOnline = online
                updateOfflineBanner()
                if (online && wasOffline) {
                    retryPendingOrReload()
                }
            }
        }
    }

    private fun retryPendingOrReload() {
        if (!networkMonitor.isOnline()) {
            updateOfflineBanner()
            return
        }
        hideOfflineBanner()
        val page = pendingRetryPage
        pendingRetryPage = null
        when {
            page != null -> loadPeople(page)
            people.isEmpty() -> loadPeople(page = 1)
            hasMore && prefetchedPage == null && !isPrefetching -> prefetchNextPage()
        }
    }

    private fun updateOfflineBanner() {
        val show = !isOnline || pendingRetryPage != null
        binding.bannerOffline.isVisible = show
        binding.textOfflineBanner.text = when {
            !isOnline && people.isNotEmpty() -> getString(R.string.error_offline)
            !isOnline -> getString(R.string.error_offline_empty)
            else -> getString(R.string.error_network)
        }
    }

    private fun hideOfflineBanner() {
        if (isOnline) {
            binding.bannerOffline.isVisible = false
            pendingRetryPage = null
        }
    }

    /** Retour en haut à durée fixe (~400 ms), indépendante de la taille de la liste. */
    private fun scrollToTop() {
        val recycler = binding.recyclerPeople
        val layoutManager = recycler.layoutManager as? LinearLayoutManager ?: return
        val firstVisible = layoutManager.findFirstVisibleItemPosition()
        if (firstVisible <= 0) return

        // Saut immédiat près du sommet : le smooth scroll reste toujours court
        if (firstVisible > SCROLL_TOP_NEAR_ITEMS) {
            layoutManager.scrollToPositionWithOffset(SCROLL_TOP_NEAR_ITEMS, 0)
        }

        recycler.post {
            val scroller = object : LinearSmoothScroller(this) {
                override fun getVerticalSnapPreference(): Int = SNAP_TO_START

                override fun calculateTimeForScrolling(dx: Int): Int =
                    minOf(super.calculateTimeForScrolling(dx), 50)

                override fun calculateTimeForDeceleration(dx: Int): Int = SCROLL_TOP_DURATION_MS
            }.apply { targetPosition = 0 }
            layoutManager.startSmoothScroll(scroller)
        }
    }

    private fun startSearch(query: String) {
        searchQuery = query
        isSearchMode = true
        resetPaginationAndReload()
    }

    private fun resetToPopular() {
        searchQuery = ""
        isSearchMode = false
        resetPaginationAndReload()
    }

    private fun resetPaginationAndReload() {
        cancelAllCalls()
        currentPage = 0
        totalPages = Int.MAX_VALUE
        hasMore = true
        prefetchedPage = null
        people.clear()
        personAdapter.submitList(emptyList())
        loadPeople(page = 1)
    }

    private fun cancelAllCalls() {
        activeCall?.cancel()
        activeCall = null
        prefetchCall?.cancel()
        prefetchCall = null
        isLoading = false
        isPrefetching = false
    }

    /** Utilise la page préchargée si dispo, sinon charge (spinner seulement si rien en attente). */
    private fun consumeNextPage() {
        if (isLoading) return

        val buffered = prefetchedPage
        if (buffered != null) {
            prefetchedPage = null
            appendPage(buffered, isFirstPage = false)
            prefetchNextPage()
            return
        }

        if (isPrefetching) return

        loadPeople(page = currentPage + 1)
    }

    private fun loadPeople(page: Int) {
        if (isLoading || page > totalPages) return

        if (!networkMonitor.isOnline()) {
            pendingRetryPage = page
            handleNetworkFailure(isFirstPage = page == 1, offline = true)
            return
        }

        isLoading = true
        val isFirstPage = page == 1
        showLoading(isFirstPage)

        activeCall?.cancel()
        val call = createPeopleCall(page)
        activeCall = call

        call.enqueue(object : Callback<PopularPeopleResponse> {
            override fun onResponse(
                call: Call<PopularPeopleResponse>,
                response: Response<PopularPeopleResponse>,
            ) {
                if (isFinishing || call.isCanceled) return

                isLoading = false
                showLoading(false)

                if (!response.isSuccessful) {
                    pendingRetryPage = page
                    showError(getString(R.string.error_http, response.code()), isFirstPage)
                    updateOfflineBanner()
                    return
                }

                val body = response.body()
                if (body == null) {
                    showError(getString(R.string.error_empty), isFirstPage)
                    return
                }

                pendingRetryPage = null
                hideOfflineBanner()
                appendPage(body, isFirstPage)
                prefetchNextPage()
            }

            override fun onFailure(call: Call<PopularPeopleResponse>, t: Throwable) {
                if (isFinishing || call.isCanceled) return
                isLoading = false
                showLoading(false)
                Log.e(TAG, "Échec du chargement page $page", t)
                pendingRetryPage = page
                handleNetworkFailure(isFirstPage, offline = isConnectivityFailure(t))
            }
        })
    }

    private fun prefetchNextPage() {
        val nextPage = currentPage + 1
        if (!hasMore || nextPage > totalPages || isPrefetching || prefetchedPage != null) return
        if (!networkMonitor.isOnline()) return

        isPrefetching = true
        prefetchCall?.cancel()
        val call = createPeopleCall(nextPage)
        prefetchCall = call

        call.enqueue(object : Callback<PopularPeopleResponse> {
            override fun onResponse(
                call: Call<PopularPeopleResponse>,
                response: Response<PopularPeopleResponse>,
            ) {
                if (isFinishing || call.isCanceled) return
                isPrefetching = false
                prefetchCall = null

                if (!response.isSuccessful) {
                    Log.w(TAG, "Préchargement page $nextPage échoué HTTP ${response.code()}")
                    return
                }
                val body = response.body() ?: return
                prefetchedPage = body
                ImagePrefetcher.prefetchProfiles(body.results)
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "Page $nextPage préchargée (${body.results.size} éléments)")
                }
            }

            override fun onFailure(call: Call<PopularPeopleResponse>, t: Throwable) {
                if (isFinishing || call.isCanceled) return
                isPrefetching = false
                prefetchCall = null
                Log.w(TAG, "Préchargement page $nextPage échoué", t)
                // Silencieux : pas de toast, la page se rechargera au scroll / retour réseau
            }
        })
    }

    private fun isConnectivityFailure(t: Throwable): Boolean =
        t is UnknownHostException ||
            t is IOException ||
            t.cause is UnknownHostException ||
            t.cause is IOException

    private fun handleNetworkFailure(isFirstPage: Boolean, offline: Boolean) {
        isOnline = networkMonitor.isOnline()
        updateOfflineBanner()
        if (isFirstPage && people.isEmpty()) {
            binding.textError.text = getString(
                if (offline || !isOnline) R.string.error_offline_empty else R.string.error_network,
            )
            binding.textError.isVisible = true
            binding.recyclerPeople.isVisible = false
            binding.progressBar.isVisible = false
        }
        // Avec des données déjà affichées : liste conservée + bandeau (pas de toast technique)
    }

    private fun createPeopleCall(page: Int): Call<PopularPeopleResponse> =
        if (isSearchMode) {
            TmdbClient.api.searchPeople(query = searchQuery, page = page)
        } else {
            TmdbClient.api.getPopularPeople(page = page)
        }

    private fun appendPage(body: PopularPeopleResponse, isFirstPage: Boolean) {
        currentPage = body.page
        totalPages = body.totalPages
        totalResults = body.totalResults
        hasMore = currentPage < totalPages

        if (isFirstPage) people.clear()
        people.addAll(body.results)
        personAdapter.submitList(people.toList())
        ImagePrefetcher.prefetchProfiles(body.results)
        updateStats()

        if (BuildConfig.DEBUG) {
            Log.d(TAG, "Page $currentPage : ${people.size}/$totalResults")
            body.results.forEach { person ->
                Log.d(
                    TAG,
                    "Personne : ${person.name} | ${person.knownForDepartment} | ${person.popularity}",
                )
            }
        }

        binding.textError.isVisible = people.isEmpty()
        if (people.isEmpty()) {
            binding.textError.text = getString(R.string.error_empty)
        } else {
            binding.recyclerPeople.isVisible = true
        }
    }

    private fun updateStats() {
        binding.textStats.text = getString(R.string.people_stats, people.size, totalResults)
    }

    private fun showLoading(initial: Boolean) {
        if (initial) {
            binding.progressBar.isVisible = true
            binding.progressBarMore.isVisible = false
            binding.recyclerPeople.isVisible = false
            binding.textError.visibility = View.GONE
        } else {
            binding.progressBar.isVisible = false
            binding.progressBarMore.isVisible = isLoading && people.isNotEmpty()
            if (people.isNotEmpty()) {
                binding.recyclerPeople.isVisible = true
            }
        }
    }

    private fun showError(message: String, isFirstPage: Boolean) {
        if (isFirstPage && people.isEmpty()) {
            binding.textError.text = message
            binding.textError.isVisible = true
            binding.recyclerPeople.isVisible = false
        } else {
            updateOfflineBanner()
        }
    }

    companion object {
        private const val TAG = "MainActivity"
        private const val SCROLL_TOP_NEAR_ITEMS = 10
        private const val SCROLL_TOP_DURATION_MS = 400
    }
}
