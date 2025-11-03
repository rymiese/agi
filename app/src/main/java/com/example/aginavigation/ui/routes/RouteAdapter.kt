package com.example.aginavigation.ui.routes

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.aginavigation.R

class RouteAdapter(
    private var routes: List<Route>,
    private val onRouteClick: (Route) -> Unit
) : RecyclerView.Adapter<RouteAdapter.RouteViewHolder>() {

    class RouteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvTitle: TextView = itemView.findViewById(R.id.tvRouteTitle)
        val tvFare: TextView = itemView.findViewById(R.id.tvRouteFare)
        val tvSummary: TextView = itemView.findViewById(R.id.tvRouteSummary)
        val tvCategory: TextView = itemView.findViewById(R.id.tvCategoryPill)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RouteViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_route, parent, false)
        return RouteViewHolder(view)
    }

    override fun onBindViewHolder(holder: RouteViewHolder, position: Int) {
        val route = routes[position]
        holder.tvTitle.text = route.title
        holder.tvFare.text = route.fareText
        holder.tvSummary.text = route.summary

        // Extract route variant (A, B, etc.) from title
        holder.tvCategory.text = when {
            route.title.contains("(A)", ignoreCase = true) -> "Route A"
            route.title.contains("(B)", ignoreCase = true) -> "Route B"
            route.title.contains("(C)", ignoreCase = true) -> "Route C"
            else -> "Route"
        }

        holder.itemView.setOnClickListener { onRouteClick(route) }
    }

    override fun getItemCount(): Int = routes.size

    fun updateRoutes(newRoutes: List<Route>) {
        routes = newRoutes
        notifyDataSetChanged()
    }
}
