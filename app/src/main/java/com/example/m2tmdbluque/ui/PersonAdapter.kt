package com.example.m2tmdbluque.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.m2tmdbluque.R
import com.example.m2tmdbluque.api.TmdbClient
import com.example.m2tmdbluque.databinding.ItemPersonBinding
import com.example.m2tmdbluque.model.Person
import com.squareup.picasso.Picasso

class PersonAdapter(
    private val host: FragmentActivity,
    private val onPersonClick: (Person) -> Unit,
) : ListAdapter<Person, PersonAdapter.PersonViewHolder>(DiffCallback) {

    /** Snapshot synchrone (ListAdapter.currentList est mis à jour en différé). */
    private var items: List<Person> = emptyList()

    /** Score maximal courant de la liste (référence 100 % pour la jauge). */
    var maxPopularity: Double = 1.0
        private set

    val size: Int get() = items.size
    val isEmpty: Boolean get() = items.isEmpty()
    val isNotEmpty: Boolean get() = items.isNotEmpty()

    fun replaceAll(people: List<Person>) {
        items = people
        submitList(items)
    }

    fun append(people: List<Person>) {
        items = items + people
        submitList(items)
    }

    fun clear() {
        items = emptyList()
        submitList(items)
    }

    override fun submitList(list: List<Person>?) {
        val previousMax = maxPopularity
        maxPopularity = list?.maxOfOrNull { it.popularity }?.coerceAtLeast(1.0) ?: 1.0
        val maxChanged = maxPopularity != previousMax
        super.submitList(list) {
            if (maxChanged && itemCount > 0) {
                notifyItemRangeChanged(0, itemCount)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PersonViewHolder {
        val binding = ItemPersonBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        // ID unique par ViewHolder : obligatoire pour FragmentContainerView dans une RecyclerView
        binding.socialBarContainer.id = View.generateViewId()
        // Pas de restauration d'état : les IDs générés ne survivent pas à une recreation
        binding.socialBarContainer.isSaveEnabled = false
        return PersonViewHolder(binding, host.supportFragmentManager, onPersonClick)
    }

    override fun onBindViewHolder(holder: PersonViewHolder, position: Int) {
        holder.bind(getItem(position), maxPopularity)
    }

    override fun onViewRecycled(holder: PersonViewHolder) {
        Picasso.get().cancelRequest(holder.binding.imageProfile)
        holder.detachSocialBar()
        super.onViewRecycled(holder)
    }

    override fun onViewAttachedToWindow(holder: PersonViewHolder) {
        super.onViewAttachedToWindow(holder)
        // Le container n'est trouvable par le FragmentManager qu'une fois dans la hiérarchie
        holder.attachSocialBarIfNeeded()
    }

    /** À appeler au retour sur l'écran (les items visibles ne repassent pas forcément par attach). */
    fun reattachVisibleSocialBars(recyclerView: RecyclerView) {
        for (i in 0 until recyclerView.childCount) {
            val child = recyclerView.getChildAt(i) ?: continue
            val holder = recyclerView.getChildViewHolder(child) as? PersonViewHolder ?: continue
            holder.attachSocialBarIfNeeded()
        }
    }

    class PersonViewHolder(
        val binding: ItemPersonBinding,
        private val fragmentManager: FragmentManager,
        private val onPersonClick: (Person) -> Unit,
    ) : RecyclerView.ViewHolder(binding.root) {

        private var boundPersonId: Int = NO_PERSON
        private var boundPersonName: String = ""

        fun bind(person: Person, maxPopularity: Double) {
            val context = binding.root.context
            boundPersonId = person.id
            boundPersonName = person.name
            binding.person = person
            val percent = ScoreRating.popularityPercent(person.popularity, maxPopularity)
            binding.scoreMax = maxPopularity.toFloat()
            binding.scoreLabel = ScoreRating.getRating(context, percent)
            binding.scoreColor = ScoreRating.getScoreColor(context, percent)
            Picasso.get().cancelRequest(binding.imageProfile)
            val imageUrl = TmdbClient.profileImageUrl(person.profilePath)
            if (imageUrl == null) {
                binding.imageProfile.setImageResource(R.drawable.bg_no_person_photo)
                binding.imageProfile.contentDescription =
                    context.getString(R.string.person_photo_missing)
            } else {
                binding.imageProfile.contentDescription =
                    context.getString(R.string.person_photo)
                Picasso.get()
                    .load(imageUrl)
                    .placeholder(R.drawable.bg_placeholder_person)
                    .error(R.drawable.bg_no_person_photo)
                    .resize(ImagePrefetcher.PROFILE_WIDTH, ImagePrefetcher.PROFILE_HEIGHT)
                    .centerCrop()
                    .into(binding.imageProfile)
            }
            binding.executePendingBindings()
            binding.root.setOnClickListener { onPersonClick(person) }

            if (binding.socialBarContainer.isAttachedToWindow) {
                attachSocialBarIfNeeded()
            }
        }

        fun attachSocialBarIfNeeded() {
            if (boundPersonId == NO_PERSON) return
            if (!binding.socialBarContainer.isAttachedToWindow) return
            // Après onSaveInstanceState (ex. ouverture du détail), réessayer au prochain frame
            if (fragmentManager.isStateSaved) {
                binding.socialBarContainer.post { attachSocialBarIfNeeded() }
                return
            }

            val containerId = binding.socialBarContainer.id
            val tag = socialBarTag(containerId)
            val existing = fragmentManager.findFragmentByTag(tag) as? SocialBarFragment
            if (existing != null && existing.isAdded && existing.personId == boundPersonId) return

            fragmentManager.beginTransaction()
                .setReorderingAllowed(true)
                .replace(
                    containerId,
                    SocialBarFragment.newInstance(boundPersonId, boundPersonName),
                    tag,
                )
                .commitNowAllowingStateLoss()
        }

        fun detachSocialBar() {
            if (fragmentManager.isDestroyed) return
            val tag = socialBarTag(binding.socialBarContainer.id)
            val existing = fragmentManager.findFragmentByTag(tag) ?: return
            if (fragmentManager.isStateSaved) {
                fragmentManager.beginTransaction()
                    .remove(existing)
                    .commitAllowingStateLoss()
            } else {
                fragmentManager.beginTransaction()
                    .remove(existing)
                    .commitNowAllowingStateLoss()
            }
        }

        private fun socialBarTag(containerId: Int): String = "social_bar_$containerId"
    }

    private companion object {
        const val NO_PERSON = -1

        val DiffCallback = object : DiffUtil.ItemCallback<Person>() {
            override fun areItemsTheSame(oldItem: Person, newItem: Person): Boolean =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: Person, newItem: Person): Boolean =
                oldItem == newItem
        }
    }
}
