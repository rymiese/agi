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
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.navigation.findNavController
import com.example.aginavigation.R
import com.example.aginavigation.application.NavigationApplication
import com.example.aginavigation.data.RouteData
import com.example.aginavigation.data.database.DestinationEntity
import com.example.aginavigation.data.database.RouteEntity
import com.google.android.gms.maps.model.LatLng
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.ArrayList

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

    private val routeViewModel: RouteViewModel by viewModels {
        val app = requireActivity().application as NavigationApplication
        RouteViewModelFactory(
            app.routeRepository,
            app.destinationRepository,
            app.favoriteRepository,
            app.searchHistoryRepository
        )
    }

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
        setupAdapters()
        setupRecyclerView()
        setupSearchFunctionality()
        setupClickListeners(view)

        observeViewModels()

        // cache header/filter and segmented views
        locationContainerView = view.findViewById(R.id.locationContainer)
        headerContainerView = view.findViewById(R.id.headerContainer)
        tvRoutesHeaderView = view.findViewById(R.id.tvRoutesHeader)
        tvRoutesCountView = view.findViewById(R.id.tvRoutesCount)
        filterRowView = view.findViewById(R.id.filterRow)
        segSearchView = view.findViewById(R.id.seg_search)
        segAllRoutesView = view.findViewById(R.id.seg_all_routes)

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

    private fun setupAdapters() {
        destinationAdapter = DestinationAdapter(emptyList()) { destination ->
            onDestinationSelected(destination)
        }
        routeAdapter = RouteAdapter(emptyList()) { route ->
            onRouteSelected(route)
        }
    }

    private fun setupRecyclerView() {
        rvDestinations.layoutManager = LinearLayoutManager(requireContext())
        // Adapter set in updateUiForMode
    }

    private fun observeViewModels() {
        routeViewModel.allDestinations.observe(viewLifecycleOwner) { destinations ->
            destinationAdapter.updateDestinations(destinations)
        }

        routeViewModel.allRoutes.observe(viewLifecycleOwner) { routes ->
            routeAdapter.updateRoutes(routes)
            (tvRoutesCountView as? TextView)?.text = getString(R.string.routes_count, routes.size)
        }
    }

    private fun setupSearchFunctionality() {
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                val query = s.toString()
                if (showingRoutes) {
                    routeViewModel.searchRoutes(query).observe(viewLifecycleOwner) { routes ->
                        routeAdapter.updateRoutes(routes)
                    }
                } else {
                    routeViewModel.searchDestinations(query).observe(viewLifecycleOwner) { destinations ->
                        destinationAdapter.updateDestinations(destinations)
                    }
                }
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
            updateUiForMode()
        }

        segAllRoutesView?.setOnClickListener {
            showingRoutes = true
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

    private fun onDestinationSelected(destination: DestinationEntity) {
        Toast.makeText(requireContext(), "Selected: ${destination.name}", Toast.LENGTH_SHORT).show()
    }

    private fun onRouteSelected(route: RouteEntity) {
        // Save current state before navigating
        arguments = (arguments ?: Bundle()).apply {
            putBoolean(KEY_SHOWING_ROUTES, showingRoutes)
        }

        // Load coordinates from JSON string in database or fall back to RouteData
        val routePoints: ArrayList<LatLng> = try {
            val type = object : TypeToken<List<LatLng>>() {}.type
            val points: List<LatLng> = Gson().fromJson(route.coordinates, type)
            ArrayList(points)
        } catch (e: Exception) {
             // Fallback to static data if JSON parsing fails or is empty
             ArrayList(RouteData.getRoutePoints(route.id))
        }

        val bundle = Bundle().apply {
            putParcelableArrayList("route_points", routePoints)
            putString("destinationName", route.title)
            putString("routeSummary", route.summary)
            // Construct fare text from min/max
            putString("routeFare", "₱${route.fareMin} - ₱${route.fareMax}")
            putInt("routeStops", route.stops)
        }

        // Open the Route Details screen (with embedded small map) using the activity NavController
        requireActivity().findNavController(R.id.nav_host_fragment_activity_main)
            .navigate(R.id.navigation_route_detail, bundle)
    }
}
