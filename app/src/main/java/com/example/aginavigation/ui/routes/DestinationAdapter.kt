package com.example.aginavigation.ui.routes

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.aginavigation.R

class DestinationAdapter(
    private var destinations: List<Destination>,
    private val onDestinationClick: (Destination) -> Unit
) : RecyclerView.Adapter<DestinationAdapter.DestinationViewHolder>() {

    class DestinationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val destinationName: TextView = itemView.findViewById(R.id.tvDestinationName)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DestinationViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_destination, parent, false)
        return DestinationViewHolder(view)
    }

    override fun onBindViewHolder(holder: DestinationViewHolder, position: Int) {
        val destination = destinations[position]
        holder.destinationName.text = destination.name

        holder.itemView.setOnClickListener {
            onDestinationClick(destination)
        }
    }

    override fun getItemCount(): Int = destinations.size

    fun updateDestinations(newDestinations: List<Destination>) {
        destinations = newDestinations
        notifyDataSetChanged()
    }
}