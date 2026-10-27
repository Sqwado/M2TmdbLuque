package com.example.m2tmdbluque.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.m2tmdbluque.R
import com.example.m2tmdbluque.api.TmdbClient
import com.example.m2tmdbluque.databinding.ItemPersonBinding
import com.example.m2tmdbluque.model.Person
import com.squareup.picasso.Picasso

class PersonAdapter(
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
        return PersonViewHolder(binding, onPersonClick)
    }

    override fun onBindViewHolder(holder: PersonViewHolder, position: Int) {
        holder.bind(getItem(position), maxPopularity)
    }

    override fun onViewRecycled(holder: PersonViewHolder) {
        Picasso.get().cancelRequest(holder.binding.imageProfile)
        super.onViewRecycled(holder)
    }

    class PersonViewHolder(
        val binding: ItemPersonBinding,
        private val onPersonClick: (Person) -> Unit,
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(person: Person, maxPopularity: Double) {
            val context = binding.root.context
            binding.person = person
            ScoreRating.bindGauge(binding.scoreGauge, context, person.popularity, maxPopularity)
            binding.textKnownFor.text = person.knownForSummary(context)
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
        }
    }

    private companion object DiffCallback : DiffUtil.ItemCallback<Person>() {
        override fun areItemsTheSame(oldItem: Person, newItem: Person): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: Person, newItem: Person): Boolean =
            oldItem == newItem
    }
}
