package com.example.aginavigation.ui.routes

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.LinearLayout
import android.widget.FrameLayout
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.aginavigation.R
import com.example.aginavigation.data.RouteRenderConfig
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import androidx.core.graphics.toColorInt

class RouteDetailFragment : Fragment(), OnMapReadyCallback {

    private var googleMap: GoogleMap? = null
    private var routePoints: ArrayList<LatLng>? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_route_detail, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        val btnBack: ImageButton = view.findViewById(R.id.btnBack)
        btnBack.setOnClickListener { findNavController().navigateUp() }

        val btnStart: Button = view.findViewById(R.id.btnStartNavigation)
        btnStart.setOnClickListener {
            // Placeholder - start navigation later
            findNavController().navigateUp()
        }

        // Retrieve route points if provided
        @Suppress("DEPRECATION")
        routePoints = arguments?.getParcelableArrayList("route_points")

        val btnViewFull: Button? = view.findViewById(R.id.btnViewFullMap)
        btnViewFull?.setOnClickListener {
            // Open the fullscreen MapFragment and pass the same points
            val bundle = Bundle().apply { putParcelableArrayList("route_points", routePoints) }
            findNavController().navigate(R.id.navigation_map, bundle)
        }

        // Retrieve and display route name, summary and fare
        val routeName = arguments?.getString("destinationName")
        val routeSummary = arguments?.getString("routeSummary")
        val routeFare = arguments?.getString("routeFare")

        // Update the UI with route information
        view.findViewById<TextView>(R.id.tvRouteName)?.text = routeName ?: "Route Details"
        view.findViewById<TextView>(R.id.tvSummary)?.text = routeSummary ?: "No description available"
        view.findViewById<TextView>(R.id.tvFareValue)?.text = routeFare ?: "N/A"

        // Retrieve and display route detail info if available
        @Suppress("DEPRECATION")
        val routeInfo = arguments?.getSerializable("route_detail_info") as? RouteDetailInfo
        if (routeInfo != null) {
            populateRouteDetails(view, routeInfo)
        }

        // Insert a SupportMapFragment into the map_preview container
        val existing = childFragmentManager.findFragmentById(R.id.map_preview)
        if (existing == null) {
            val mapFragment = SupportMapFragment.newInstance()
            childFragmentManager.beginTransaction()
                .replace(R.id.map_preview, mapFragment)
                .commitNowAllowingStateLoss()
            mapFragment.getMapAsync(this)
        } else if (existing is SupportMapFragment) {
            (existing as SupportMapFragment).getMapAsync(this)
        }
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        routePoints?.let { points ->
            if (points.isNotEmpty()) {
                drawRoute(points)
                return
            }
        }

        // default behavior if no points provided
        val default = LatLng(13.1362, 123.7380)
        googleMap?.addMarker(com.google.android.gms.maps.model.MarkerOptions().position(default).title("Marker"))
        googleMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(default, 14f))
    }

    private fun populateRouteDetails(view: View, routeInfo: RouteDetailInfo) {
        val container = view.findViewById<ViewGroup>(R.id.routeStopsContainer) ?: return
        container.removeAllViews()


        // Add start stop
        addRouteStopView(container, routeInfo.start, isStart = true, isEnd = false)

        // Add intermediate stops
        routeInfo.stops.forEach { stop ->
            addRouteStopView(container, stop, isStart = false, isEnd = false)
        }

        // Add end stop
        addRouteStopView(container, routeInfo.end, isStart = false, isEnd = true)
    }

    private fun addRouteStopView(container: ViewGroup, stop: RouteStop, isStart: Boolean, isEnd: Boolean) {
        val context = requireContext()
        val stopView = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = ViewGroup.MarginLayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = if (isEnd) 4 else 16 // Better spacing between stops
                topMargin = if (isStart) 4 else 0
            }
        }

        // Dot container with connecting line
        val dotContainer = FrameLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(40, ViewGroup.LayoutParams.MATCH_PARENT).apply {
                setMargins(0, 0, 20, 0) // More spacing from dot to text
            }
        }

        // Vertical connecting line (only if not the last stop)
        if (!isEnd) {
            val line = View(context).apply {
                setBackgroundColor("#3A3D4E".toColorInt())
                layoutParams = FrameLayout.LayoutParams(3, ViewGroup.LayoutParams.MATCH_PARENT).apply {
                    gravity = android.view.Gravity.CENTER_HORIZONTAL
                    topMargin = 24 // Start line below the dot
                }
            }
            dotContainer.addView(line)
        }

        // Dot indicator (green for start, red for end, blue for others)
        val dot = View(context).apply {
            val color = when {
                isStart -> "#00E676".toColorInt()
                isEnd -> "#FF5252".toColorInt()
                else -> "#4A9FF5".toColorInt()
            }
            setBackgroundColor(color)
            layoutParams = FrameLayout.LayoutParams(14, 14).apply { // Slightly larger dots
                gravity = android.view.Gravity.CENTER_HORIZONTAL or android.view.Gravity.TOP
                topMargin = 6
            }
            // Make it circular
            outlineProvider = object : android.view.ViewOutlineProvider() {
                override fun getOutline(view: View, outline: android.graphics.Outline) {
                    outline.setOval(0, 0, view.width, view.height)
                }
            }
            clipToOutline = true
        }

        dotContainer.addView(dot)

        // Text container
        val textContainer = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                topMargin = 4 // Align text with dot
            }
        }

        val nameText = TextView(context).apply {
            text = stop.name
            setTextColor(android.graphics.Color.WHITE)
            textSize = 16f // Slightly larger for readability
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setPadding(0, 2, 0, 2)
        }

        textContainer.addView(nameText)

        // ETA badge if present
        if (stop.etaMinutes != null) {
            val etaBadge = TextView(context).apply {
                text = "${stop.etaMinutes} min"
                setTextColor("#8E9AAF".toColorInt())
                textSize = 13f
                setPadding(14, 6, 14, 6)
                background = android.graphics.drawable.GradientDrawable().apply {
                    setColor("#2A2D3E".toColorInt())
                    cornerRadius = 18f
                }
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    gravity = android.view.Gravity.CENTER_VERTICAL
                    setMargins(12, 0, 0, 0) // Left margin for spacing
                }
            }
            stopView.addView(dotContainer)
            stopView.addView(textContainer)
            stopView.addView(etaBadge)
        } else {
            stopView.addView(dotContainer)
            stopView.addView(textContainer)
        }

        container.addView(stopView)
    }

    private fun drawRoute(points: List<LatLng>) {
        val map = googleMap ?: return

        // clear previous overlays to avoid duplication when opening repeatedly
        map.clear()

        // main route polyline using centralized config
        val polylineOptions = PolylineOptions()
            .addAll(points)
            .color(RouteRenderConfig.polylineColorHex.toColorInt())
            .width(RouteRenderConfig.polylineWidth)
            .geodesic(true)

        map.addPolyline(polylineOptions)

        // Add directional arrow markers along the route if enabled
        if (RouteRenderConfig.showDirectionArrows && points.size > 1) {
            addDirectionalArrows(map, points)
        }

        // Optionally add markers depending on the central config
        if (RouteRenderConfig.showMarkers) {
            when (RouteRenderConfig.markerMode) {
                RouteRenderConfig.MarkerMode.NONE -> {
                    // nothing
                }
                RouteRenderConfig.MarkerMode.START_END -> {
                    val start = points.first()
                    val end = points.last()
                    if (RouteRenderConfig.startMarkerResId != null) {
                        map.addMarker(
                            MarkerOptions().position(start).title("Start")
                                .icon(com.google.android.gms.maps.model.BitmapDescriptorFactory.fromResource(RouteRenderConfig.startMarkerResId!!))
                        )
                    } else {
                        map.addMarker(
                            MarkerOptions().position(start).title("Start")
                                .icon(BitmapDescriptorFactory.defaultMarker(RouteRenderConfig.startMarkerHue))
                        )
                    }

                    if (RouteRenderConfig.endMarkerResId != null) {
                        map.addMarker(
                            MarkerOptions().position(end).title("End")
                                .icon(com.google.android.gms.maps.model.BitmapDescriptorFactory.fromResource(RouteRenderConfig.endMarkerResId!!))
                        )
                    } else {
                        map.addMarker(
                            MarkerOptions().position(end).title("End")
                                .icon(BitmapDescriptorFactory.defaultMarker(RouteRenderConfig.endMarkerHue))
                        )
                    }
                }
                RouteRenderConfig.MarkerMode.ALL_NUMBERED -> {
                    // Add markers for all stops (simple colored markers)
                    for (i in points.indices) {
                        val pos = points[i]
                        if (RouteRenderConfig.intermediateMarkerResId != null) {
                            map.addMarker(
                                MarkerOptions().position(pos).title("Stop ${i + 1}")
                                    .icon(com.google.android.gms.maps.model.BitmapDescriptorFactory.fromResource(RouteRenderConfig.intermediateMarkerResId!!))
                            )
                        } else {
                            // use hue: start gets start hue, end gets end hue, intermediates get intermediateHue
                            val hue = when (i) {
                                0 -> RouteRenderConfig.startMarkerHue
                                points.size - 1 -> RouteRenderConfig.endMarkerHue
                                else -> RouteRenderConfig.intermediateMarkerHue
                            }
                            map.addMarker(
                                MarkerOptions().position(pos).title("Stop ${i + 1}")
                                    .icon(BitmapDescriptorFactory.defaultMarker(hue))
                            )
                        }
                    }
                }
            }
        }

        // Build camera bounds so the whole route fits nicely with padding
        val boundsBuilder = LatLngBounds.builder()
        points.forEach { boundsBuilder.include(it) }
        val bounds = boundsBuilder.build()
        // Slightly larger padding for the small preview card so markers and route aren't cut off
        map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 160))

        // Improve map UI for preview: hide map toolbar and allow gestures
        map.uiSettings.isMapToolbarEnabled = false
        map.uiSettings.setAllGesturesEnabled(true)
    }

    private fun addDirectionalArrows(map: GoogleMap, points: List<LatLng>) {
        // Add arrow markers at regular intervals to show direction
        val arrowInterval = points.size / 15 // Show about 15 arrows along the route
        if (arrowInterval < 2) return

        // Create a larger, more prominent arrow icon
        val arrowIcon = createArrowBitmap()

        for (i in arrowInterval until points.size step arrowInterval) {
            if (i >= points.size - 1) break

            val point = points[i]
            val nextPoint = points[i + 1]

            // Calculate bearing (direction) between consecutive points
            val bearing = calculateBearing(point, nextPoint)

            // Create a larger arrow marker rotated in the direction of travel
            val markerOptions = MarkerOptions()
                .position(point)
                .rotation(bearing)
                .anchor(0.5f, 0.5f)
                .flat(true)
                .icon(arrowIcon)
                .zIndex(10f) // Place arrows above the route line

            map.addMarker(markerOptions)
        }
    }

    private fun createArrowBitmap(): com.google.android.gms.maps.model.BitmapDescriptor {
        val size = 100
        val bitmap = android.graphics.Bitmap.createBitmap(size, size, android.graphics.Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)

        val paint = android.graphics.Paint().apply {
            isAntiAlias = true
            style = android.graphics.Paint.Style.FILL
        }

        // Draw arrow body (white/light interior)
        paint.color = android.graphics.Color.parseColor("#FFFFFF")
        val bodyPath = android.graphics.Path().apply {
            moveTo(size / 2f, size * 0.25f)
            lineTo(size * 0.65f, size * 0.5f)
            lineTo(size * 0.55f, size * 0.5f)
            lineTo(size * 0.55f, size * 0.75f)
            lineTo(size * 0.45f, size * 0.75f)
            lineTo(size * 0.45f, size * 0.5f)
            lineTo(size * 0.35f, size * 0.5f)
            close()
        }
        canvas.drawPath(bodyPath, paint)

        // Draw arrow tip (orange)
        paint.color = android.graphics.Color.parseColor("#FF9800")
        val tipPath = android.graphics.Path().apply {
            moveTo(size / 2f, size * 0.25f)
            lineTo(size * 0.65f, size * 0.5f)
            lineTo(size * 0.35f, size * 0.5f)
            close()
        }
        canvas.drawPath(tipPath, paint)

        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }

    private fun calculateBearing(start: LatLng, end: LatLng): Float {
        val lat1 = Math.toRadians(start.latitude)
        val lat2 = Math.toRadians(end.latitude)
        val lonDiff = Math.toRadians(end.longitude - start.longitude)

        val y = Math.sin(lonDiff) * Math.cos(lat2)
        val x = Math.cos(lat1) * Math.sin(lat2) - Math.sin(lat1) * Math.cos(lat2) * Math.cos(lonDiff)

        val bearing = Math.toDegrees(Math.atan2(y, x))
        return ((bearing + 360) % 360).toFloat()
    }
}
