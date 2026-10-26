package com.example.m2tmdbluque

import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.m2tmdbluque.api.TmdbClient
import com.example.m2tmdbluque.databinding.ActivityMainBinding
import com.example.m2tmdbluque.model.Person
import com.example.m2tmdbluque.model.PopularPeopleResponse
import com.example.m2tmdbluque.ui.PersonAdapter
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val personAdapter = PersonAdapter { person ->
        startActivity(PersonDetailActivity.newIntent(this, person))
    }
    private val people = mutableListOf<Person>()

    private var currentPage = 0
    private var totalPages = Int.MAX_VALUE
    private var totalResults = 0
    private var isLoading = false
    private var hasMore = true
    private var searchQuery = ""
    private var isSearchMode = false
    private var activeCall: Call<PopularPeopleResponse>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupRecyclerView()
        setupSearch()
        setupScrollToTop()
        loadPeople(page = 1)
    }

    override fun onDestroy() {
        activeCall?.cancel()
        activeCall = null
        super.onDestroy()
    }

    private fun setupRecyclerView() {
        val layoutManager = LinearLayoutManager(this)
        binding.recyclerPeople.layoutManager = layoutManager
        binding.recyclerPeople.adapter = personAdapter

        binding.recyclerPeople.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                binding.fabScrollTop.isVisible =
                    layoutManager.findFirstVisibleItemPosition() > SCROLL_TOP_THRESHOLD

                if (dy <= 0 || isLoading || !hasMore) return

                val lastVisible = layoutManager.findLastVisibleItemPosition()
                if (lastVisible >= layoutManager.itemCount - PREFETCH_THRESHOLD) {
                    loadPeople(page = currentPage + 1)
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
            val query = binding.editSearch.text?.toString()?.trim().orEmpty()
            if (query.isNotEmpty()) startSearch(query) else resetToPopular()
            true
        }
    }

    private fun setupScrollToTop() {
        binding.fabScrollTop.setOnClickListener {
            binding.recyclerPeople.smoothScrollToPosition(0)
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
        currentPage = 0
        totalPages = Int.MAX_VALUE
        hasMore = true
        people.clear()
        personAdapter.submitList(emptyList())
        loadPeople(page = 1)
    }

    private fun loadPeople(page: Int) {
        if (isLoading || page > totalPages) return

        isLoading = true
        val isFirstPage = page == 1
        showLoading(isFirstPage)

        activeCall?.cancel()
        val call = if (isSearchMode) {
            TmdbClient.api.searchPeople(query = searchQuery, page = page)
        } else {
            TmdbClient.api.getPopularPeople(page = page)
        }
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
                    showError(getString(R.string.error_http, response.code()), isFirstPage)
                    return
                }

                val body = response.body()
                if (body == null) {
                    showError(getString(R.string.error_empty), isFirstPage)
                    return
                }

                currentPage = body.page
                totalPages = body.totalPages
                totalResults = body.totalResults
                hasMore = currentPage < totalPages

                if (isFirstPage) people.clear()
                people.addAll(body.results)
                personAdapter.submitList(people.toList())
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

            override fun onFailure(call: Call<PopularPeopleResponse>, t: Throwable) {
                if (isFinishing || call.isCanceled) return
                isLoading = false
                showLoading(false)
                Log.e(TAG, "Échec du chargement page $page", t)
                showError(
                    getString(R.string.error_network, t.message ?: "inconnue"),
                    isFirstPage,
                )
            }
        })
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
        if (isFirstPage) {
            binding.textError.text = message
            binding.textError.isVisible = true
            binding.recyclerPeople.isVisible = false
        }
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    companion object {
        private const val TAG = "MainActivity"
        private const val PREFETCH_THRESHOLD = 5
        private const val SCROLL_TOP_THRESHOLD = 8
    }
}
