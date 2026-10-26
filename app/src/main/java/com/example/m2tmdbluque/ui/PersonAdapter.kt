package com.example.m2tmdbluque.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.m2tmdbluque.R
import com.example.m2tmdbluque.api.TmdbClient
import com.example.m2tmdbluque.databinding.ItemPersonBinding
import com.example.m2tmdbluque.model.Person

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

    class PersonViewHolder(
        private val binding: ItemPersonBinding,
        private val onPersonClick: (Person) -> Unit,
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(person: Person) {
            binding.person = person
            binding.executePendingBindings()

            Glide.with(binding.imageProfile)
                .load(TmdbClient.profileImageUrl(person.profilePath))
                .placeholder(R.drawable.bg_placeholder_person)
                .error(R.drawable.bg_placeholder_person)
                .centerCrop()
                .into(binding.imageProfile)

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
