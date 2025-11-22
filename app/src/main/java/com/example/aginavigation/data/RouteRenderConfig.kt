package com.example.aginavigation.data

import com.google.android.gms.maps.model.BitmapDescriptorFactory

/**
 * Centralized rendering configuration for routes.
 * Edit these values to change how all route previews/fullscreen maps are drawn.
 *
 * All routes are now loaded from the database - no hardcoded data.
 */
object RouteRenderConfig {
    // Marker display settings
    var showMarkers: Boolean = false

    enum class MarkerMode {
        NONE,        // no markers
        START_END,   // only start and end
        ALL_NUMBERED // show all stops (numbered)
    }

    var markerMode: MarkerMode = MarkerMode.NONE

    // Polyline styling
    var polylineColorHex: String = "#E64A19"
    var polylineWidth: Float = 12f

    // Directional arrows to show route flow
    var showDirectionArrows: Boolean = true
    var arrowSpacingMeters: Int = 100 // Distance between arrows in meters

    // Default hues for default Google markers
    var startMarkerHue: Float = BitmapDescriptorFactory.HUE_AZURE
    var endMarkerHue: Float = BitmapDescriptorFactory.HUE_GREEN
    var intermediateMarkerHue: Float = BitmapDescriptorFactory.HUE_ORANGE

    // Custom marker drawables (null = use default colored markers)
    var startMarkerResId: Int? = null
    var endMarkerResId: Int? = null
    var intermediateMarkerResId: Int? = null

    /**
     * Get category-specific styling
     * Can be expanded to support different colors per route category
     */
    fun getPolylineColorForCategory(category: String): String {
        return when (category.lowercase()) {
            "jeepney" -> "#E64A19"  // Deep Orange
            "inter-city" -> "#1976D2" // Blue
            "tourist" -> "#388E3C"    // Green
            else -> polylineColorHex
        }
    }
}