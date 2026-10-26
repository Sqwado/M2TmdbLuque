package com.example.m2tmdbluque.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.m2tmdbluque.R
import com.example.m2tmdbluque.databinding.ItemKnownForBinding
import com.example.m2tmdbluque.model.KnownFor

class KnownForAdapter(
    private val items: List<KnownFor>,
) : RecyclerView.Adapter<KnownForAdapter.KnownForViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): KnownForViewHolder {
        val binding = ItemKnownForBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return KnownForViewHolder(binding)
    }

    override fun onBindViewHolder(holder: KnownForViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    class KnownForViewHolder(
        private val binding: ItemKnownForBinding,
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(knownFor: KnownFor) {
            binding.knownFor = knownFor
            binding.textMeta.text = binding.root.context.getString(
                R.string.known_for_item,
                knownFor.mediaType ?: "—",
                knownFor.displayDate(),
                knownFor.voteAverage ?: 0.0,
            )
            binding.executePendingBindings()
        }
    }
}
