package com.example.aginavigation.ui.map

import android.os.Bundle
import android.view.View
import androidx.core.graphics.toColorInt
import androidx.fragment.app.Fragment
import com.example.aginavigation.R
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions

// This class correctly implements OnMapReadyCallback to get the GoogleMap object.
class MapFragment : Fragment(R.layout.fragment_map), OnMapReadyCallback {

    private lateinit var googleMap: GoogleMap
    private var routePoints: ArrayList<LatLng>? = null

    // This is where you initialize the map fragment.
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        // Retrieve route points if passed from RoutesFragment
        @Suppress("DEPRECATION")
        routePoints = arguments?.getParcelableArrayList("route_points")

        // The SupportMapFragment is a container for the map. We find it by its ID.
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment

        // Request the map; onMapReady will be called when it's loaded.
        mapFragment.getMapAsync(this)
    }

    /**
     * This method is called when the map is ready to be used.
     * You can now add markers, move the camera, and customize the map.
     */
    override fun onMapReady(map: GoogleMap) {
        googleMap = map

        // If route points were provided, draw them; otherwise show a default marker.
        routePoints?.let { points ->
            if (points.isNotEmpty()) {
                drawRoute(points)
                return
            }
        }

        // Fallback example: Add a marker for Legazpi and zoom the camera in on it.
        val legazpi = LatLng(13.1362, 123.7380)
        googleMap.addMarker(MarkerOptions().position(legazpi).title("Marker in Legazpi"))
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(legazpi, 14f))
    }

    private fun drawRoute(points: List<LatLng>) {
        if (points.isEmpty()) return

        // Draw the polyline
        googleMap.addPolyline(
            PolylineOptions()
                .addAll(points)
                .color("#FF5722".toColorInt())
                .width(8f)
                .geodesic(true)
        )

        // Add directional arrows to show route flow
        addDirectionalArrows(googleMap, points)

        // Build bounds to fit the entire route on screen
        val boundsBuilder = LatLngBounds.builder()
        points.forEach { boundsBuilder.include(it) }
        val bounds = boundsBuilder.build()

        // Animate camera to fit route with padding
        googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 120))
    }

    private fun addDirectionalArrows(map: GoogleMap, points: List<LatLng>) {
        // Add arrow markers at regular intervals to show direction
        val arrowInterval = points.size / 18 // Show about 18 arrows along the route for full map
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
        // Create a sleek, skinny gradient arrow
        val size = 90
        val bitmap = android.graphics.Bitmap.createBitmap(size, size, android.graphics.Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)

        // Create linear gradient from reddish-orange at tip to deeper reddish-orange at bottom
        val gradient = android.graphics.LinearGradient(
            size / 2f, size * 0.05f,  // Start at arrow tip
            size / 2f, size * 0.95f,  // End at arrow bottom
            android.graphics.Color.parseColor("#FF5722"),  // Bright reddish-orange at tip
            android.graphics.Color.parseColor("#BF360C"),  // Deep reddish-orange at bottom
            android.graphics.Shader.TileMode.CLAMP
        )

        val paint = android.graphics.Paint().apply {
            shader = gradient
            style = android.graphics.Paint.Style.FILL
            isAntiAlias = true
        }

        // Draw very skinny arrow pointing up (will be rotated by marker rotation)
        val path = android.graphics.Path().apply {
            moveTo(size / 2f, size * 0.05f) // Arrow tip (sharp point)
            lineTo(size * 0.68f, size * 0.4f) // Right point (sharp head)
            lineTo(size * 0.52f, size * 0.4f) // Right shaft start (very skinny)
            lineTo(size * 0.52f, size * 0.95f) // Right bottom (long)
            lineTo(size * 0.48f, size * 0.95f) // Left bottom (very skinny shaft)
            lineTo(size * 0.48f, size * 0.4f) // Left shaft start
            lineTo(size * 0.32f, size * 0.4f) // Left point
            close()
        }

        // Draw gradient arrow
        canvas.drawPath(path, paint)

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
