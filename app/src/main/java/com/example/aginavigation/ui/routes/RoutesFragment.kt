package com.example.aginavigation.ui.routes

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.navigation.findNavController
import com.example.aginavigation.R
import com.example.aginavigation.data.RouteData
import com.google.android.gms.maps.model.LatLng

class RoutesFragment : Fragment() {

    private lateinit var etSearch: EditText
    private lateinit var tvChange: TextView
    private lateinit var rvDestinations: RecyclerView
    private lateinit var destinationAdapter: DestinationAdapter
    private lateinit var routeAdapter: RouteAdapter

    // Cached views for header/filter and segmented control
    private var locationContainerView: View? = null
    private var headerContainerView: View? = null
    private var tvRoutesHeaderView: View? = null
    private var tvRoutesCountView: View? = null
    private var filterRowView: View? = null
    private var segSearchView: View? = null
    private var segAllRoutesView: View? = null

    private val popularDestinations = listOf(
        Destination(1, "Cagsawa Ruins"),
        Destination(2, "Pacific Mall Legazpi"),
        Destination(3, "Embarcadero de Legazpi"),
        Destination(4, "Bicol University"),
        Destination(5, "Mayon Volcano Natural Park"),
        Destination(6, "Legazpi Boulevard"),
        Destination(7, "Daraga Church"),
        Destination(8, "Ligñon Hill"),
        Destination(9, "Quitinday Hills"),
        Destination(10, "Albay Park and Wildlife")
    )

    private val allRoutes = listOf(
        Route(1, "Daraga - Legazpi City (A)", "₱13 - ₱15", "A jeepney route between Daraga and Legazpi City via Washington Drive, returning via Old Albay.", 5),
        Route(2, "Daraga - Legazpi City (B)", "₱13 - ₱15", "A jeepney route between Daraga and Legazpi City via Old Albay, returning via Washington Drive.", 5)
    )

    private var filteredDestinations = popularDestinations.toMutableList()
    private var showingRoutes = false

    companion object {
        private const val KEY_SHOWING_ROUTES = "showing_routes"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_routes, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews(view)
        setupRecyclerView()
        setupSearchFunctionality()
        setupClickListeners(view)

        // cache header/filter and segmented views
        locationContainerView = view.findViewById(R.id.locationContainer)
        headerContainerView = view.findViewById(R.id.headerContainer)
        tvRoutesHeaderView = view.findViewById(R.id.tvRoutesHeader)
        tvRoutesCountView = view.findViewById(R.id.tvRoutesCount)
        filterRowView = view.findViewById(R.id.filterRow)
        segSearchView = view.findViewById(R.id.seg_search)
        segAllRoutesView = view.findViewById(R.id.seg_all_routes)

        // set routes count
        (tvRoutesCountView as? TextView)?.text = getString(R.string.routes_count, allRoutes.size)

        // Restore saved state from arguments (persists across navigation) or savedInstanceState or default to Search mode
        showingRoutes = arguments?.getBoolean(KEY_SHOWING_ROUTES, false)
            ?: savedInstanceState?.getBoolean(KEY_SHOWING_ROUTES, false)
            ?: false

        // Update UI based on restored/default state
        updateUiForMode()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        // Save the current tab state
        outState.putBoolean(KEY_SHOWING_ROUTES, showingRoutes)
    }

    override fun onResume() {
        super.onResume()
        // re-apply UI mode in case the fragment was recreated or resumed
        updateUiForMode()
    }

    private fun initViews(view: View) {
        etSearch = view.findViewById(R.id.etSearch)
        tvChange = view.findViewById(R.id.tvChange)
        rvDestinations = view.findViewById(R.id.rvDestinations)
    }

    private fun setupRecyclerView() {
        destinationAdapter = DestinationAdapter(filteredDestinations) { destination ->
            onDestinationSelected(destination)
        }

        routeAdapter = RouteAdapter(allRoutes) { route ->
            onRouteSelected(route)
        }

        rvDestinations.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = destinationAdapter
        }
    }

    private fun setupSearchFunctionality() {
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                filterDestinations(s.toString())
            }
        })
    }

    private fun setupClickListeners(view: View) {
        tvChange.setOnClickListener {
            Toast.makeText(requireContext(), "Change location clicked", Toast.LENGTH_SHORT).show()
        }

        // Segmented control toggles
        segSearchView = view.findViewById(R.id.seg_search)
        segAllRoutesView = view.findViewById(R.id.seg_all_routes)

        segSearchView?.setOnClickListener {
            showingRoutes = false
            rvDestinations.adapter = destinationAdapter
            updateUiForMode()
        }

        segAllRoutesView?.setOnClickListener {
            showingRoutes = true
            rvDestinations.adapter = routeAdapter
            updateUiForMode()
        }
    }

    private fun updateUiForMode() {
        // Toggle visibility of location vs routes header
        if (showingRoutes) {
            // Show "All Jeepney Routes" section, hide location selector
            locationContainerView?.visibility = View.GONE
            headerContainerView?.visibility = View.VISIBLE
            etSearch.isEnabled = false

            // ensure RecyclerView is showing routes
            rvDestinations.adapter = routeAdapter

            // update segmented visuals
            segSearchView?.isSelected = false
            segAllRoutesView?.isSelected = true
        } else {
            // Show location selector, hide routes header
            locationContainerView?.visibility = View.VISIBLE
            headerContainerView?.visibility = View.GONE
            etSearch.isEnabled = true

            // ensure RecyclerView is showing destinations
            rvDestinations.adapter = destinationAdapter

            segSearchView?.isSelected = true
            segAllRoutesView?.isSelected = false
        }
    }

    private fun filterDestinations(query: String) {
        if (showingRoutes) return // don't filter destinations when showing routes

        filteredDestinations.clear()

        if (query.isEmpty()) {
            filteredDestinations.addAll(popularDestinations)
        } else {
            filteredDestinations.addAll(
                popularDestinations.filter { destination ->
                    destination.name.contains(query, ignoreCase = true)
                }
            )
        }

        destinationAdapter.updateDestinations(filteredDestinations)
    }

    private fun onDestinationSelected(destination: Destination) {
        Toast.makeText(requireContext(), "Selected: ${destination.name}", Toast.LENGTH_SHORT).show()
    }

    private fun onRouteSelected(route: Route) {
        // Save current state before navigating
        arguments = (arguments ?: Bundle()).apply {
            putBoolean(KEY_SHOWING_ROUTES, showingRoutes)
        }

        // Load coordinates from centralized RouteData
        val routePoints = ArrayList(RouteData.getRoutePoints(route.id))

        val bundle = Bundle().apply {
            putParcelableArrayList("route_points", routePoints)
            putString("destinationName", route.title)
            putString("routeSummary", route.summary)
            putString("routeFare", route.fareText)
            putInt("routeStops", route.stops)
            // attach structured route detail info if available
            val info = RouteInfoProvider.getRouteInfo(route.id)
            if (info != null) putSerializable("route_detail_info", info)
        }

        // Open the Route Details screen (with embedded small map) using the activity NavController
        requireActivity().findNavController(R.id.nav_host_fragment_activity_main)
            .navigate(R.id.navigation_route_detail, bundle)
    }
}