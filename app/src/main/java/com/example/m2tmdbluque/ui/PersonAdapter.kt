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

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PersonViewHolder {
        val binding = ItemPersonBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PersonViewHolder(binding, onPersonClick)
    }

    override fun onBindViewHolder(holder: PersonViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    override fun getItemCount(): Int = currentList.size

    class PersonViewHolder(
        private val binding: ItemPersonBinding,
        private val onPersonClick: (Person) -> Unit,
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(person: Person) {
            val context = binding.root.context
            binding.person = person
            binding.textKnownFor.text = context.getString(
                R.string.person_known_for,
                person.knownForSummary(context),
            )
            val imageUrl = TmdbClient.profileImageUrl(person.profilePath)
            if (imageUrl.isNullOrBlank()) {
                binding.imageProfile.setImageResource(R.drawable.bg_placeholder_person)
            } else {
                Picasso.get()
                    .load(imageUrl)
                    .placeholder(R.drawable.bg_placeholder_person)
                    .error(R.drawable.bg_placeholder_person)
                    .fit()
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
