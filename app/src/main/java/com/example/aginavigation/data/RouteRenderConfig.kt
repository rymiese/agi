package com.example.aginavigation.data

import com.google.android.gms.maps.model.BitmapDescriptorFactory

/**
 * Centralized rendering configuration for routes.
 * Edit these values to change how all route previews/fullscreen maps are drawn in one place.
 */
object RouteRenderConfig {
    // If false, no markers are added (only the polyline will be drawn).
    // Set to true to enable markers according to markerMode.
    var showMarkers: Boolean = false

    // Which markers to draw when showMarkers == true
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

    // Default hues for default Google markers (used when not using custom icons)
    var startMarkerHue: Float = BitmapDescriptorFactory.HUE_AZURE
    var endMarkerHue: Float = BitmapDescriptorFactory.HUE_GREEN
    var intermediateMarkerHue: Float = BitmapDescriptorFactory.HUE_ORANGE

    // If you want to use custom drawable resources for start/end markers, set these to resource ids.
    // Leave as null to use default colored markers.
    var startMarkerResId: Int? = null
    var endMarkerResId: Int? = null
    var intermediateMarkerResId: Int? = null
}

