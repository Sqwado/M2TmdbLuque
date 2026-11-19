package com.example.m2tmdbluque

import android.hardware.Sensor
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.m2tmdbluque.databinding.SensorItemBinding

class SensorListAdapter(
    private val sensors: ArrayList<Sensor>,
    private val sensorItemClickListener: OnSensorItemClickListener,
) : RecyclerView.Adapter<SensorListAdapter.SensorItemViewHolder>() {

    var selectedPosition: Int = RecyclerView.NO_POSITION
        set(value) {
            val previous = field
            field = value
            if (previous != RecyclerView.NO_POSITION) notifyItemChanged(previous)
            if (value != RecyclerView.NO_POSITION) notifyItemChanged(value)
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SensorItemViewHolder {
        val binding = SensorItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return SensorItemViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SensorItemViewHolder, position: Int) {
        holder.binding.sensorItem = sensors[position]
        holder.binding.sensorItemView.isSelected = position == selectedPosition
        holder.binding.executePendingBindings()
    }

    override fun getItemCount(): Int = sensors.size

    inner class SensorItemViewHolder(
        val binding: SensorItemBinding,
    ) : RecyclerView.ViewHolder(binding.root) {
        init {
            binding.sensorItemView.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    sensorItemClickListener.onSensorItemClicked(position)
                }
            }
        }
    }
}
