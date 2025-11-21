package com.example.aginavigation.utils

import android.content.Context
import com.google.android.gms.maps.model.LatLng
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.InputStream

/**
 * Utility class to parse KML files and extract route coordinates
 */
class KmlParser {

    data class KmlRoute(
        val name: String,
        val description: String,
        val coordinates: List<LatLng>,
        val placemarks: List<KmlPlacemark> = emptyList()
    )

    data class KmlPlacemark(
        val name: String,
        val description: String,
        val coordinate: LatLng
    )

    /**
     * Parse KML file from assets folder
     */
    fun parseKmlFromAssets(context: Context, fileName: String): KmlRoute? {
        return try {
            val inputStream = context.assets.open(fileName)
            parseKmlFromStream(inputStream)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Parse KML file from raw resources
     */
    fun parseKmlFromRaw(context: Context, resourceId: Int): KmlRoute? {
        return try {
            val inputStream = context.resources.openRawResource(resourceId)
            parseKmlFromStream(inputStream)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Parse KML from InputStream
     */
    private fun parseKmlFromStream(inputStream: InputStream): KmlRoute? {
        val factory = XmlPullParserFactory.newInstance()
        factory.isNamespaceAware = true
        val parser = factory.newPullParser()
        parser.setInput(inputStream, null)

        var routeName = ""
        var routeDescription = ""
        val coordinates = mutableListOf<LatLng>()
        val placemarks = mutableListOf<KmlPlacemark>()

        var currentPlacemarkName = ""
        var currentPlacemarkDesc = ""
        var insidePlacemark = false
        var insideLineString = false
        var insidePoint = false

        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    when (parser.name) {
                        "Placemark" -> {
                            insidePlacemark = true
                            currentPlacemarkName = ""
                            currentPlacemarkDesc = ""
                        }
                        "LineString" -> insideLineString = true
                        "Point" -> insidePoint = true
                        "name" -> {
                            parser.next()
                            if (parser.eventType == XmlPullParser.TEXT) {
                                val text = parser.text
                                if (insidePlacemark && currentPlacemarkName.isEmpty()) {
                                    currentPlacemarkName = text
                                } else if (routeName.isEmpty()) {
                                    routeName = text
                                }
                            }
                        }
                        "description" -> {
                            parser.next()
                            if (parser.eventType == XmlPullParser.TEXT) {
                                val text = parser.text
                                if (insidePlacemark && currentPlacemarkDesc.isEmpty()) {
                                    currentPlacemarkDesc = text
                                } else if (routeDescription.isEmpty()) {
                                    routeDescription = text
                                }
                            }
                        }
                        "coordinates" -> {
                            parser.next()
                            if (parser.eventType == XmlPullParser.TEXT) {
                                val coordText = parser.text.trim()

                                if (insideLineString) {
                                    // Parse LineString coordinates (full route)
                                    coordinates.addAll(parseCoordinates(coordText))
                                } else if (insidePoint) {
                                    // Parse Point coordinates (single placemark)
                                    val point = parseCoordinates(coordText).firstOrNull()
                                    if (point != null && currentPlacemarkName.isNotEmpty()) {
                                        placemarks.add(
                                            KmlPlacemark(
                                                currentPlacemarkName,
                                                currentPlacemarkDesc,
                                                point
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                XmlPullParser.END_TAG -> {
                    when (parser.name) {
                        "Placemark" -> insidePlacemark = false
                        "LineString" -> insideLineString = false
                        "Point" -> insidePoint = false
                    }
                }
            }
            eventType = parser.next()
        }

        inputStream.close()

        return if (coordinates.isNotEmpty()) {
            KmlRoute(
                name = routeName.ifEmpty { "Unnamed Route" },
                description = routeDescription.ifEmpty { "No description" },
                coordinates = coordinates,
                placemarks = placemarks
            )
        } else {
            null
        }
    }

    /**
     * Parse coordinate string from KML
     * Format: "lng,lat,alt lng,lat,alt ..." or "lng,lat lng,lat ..."
     */
    private fun parseCoordinates(coordText: String): List<LatLng> {
        val coordinates = mutableListOf<LatLng>()

        // Split by whitespace or newlines
        val coordPairs = coordText.trim()
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }

        for (pair in coordPairs) {
            val parts = pair.split(",")
            if (parts.size >= 2) {
                try {
                    val lng = parts[0].trim().toDouble()
                    val lat = parts[1].trim().toDouble()
                    // Altitude (parts[2]) is ignored
                    coordinates.add(LatLng(lat, lng))
                } catch (e: NumberFormatException) {
                    // Skip invalid coordinates
                    continue
                }
            }
        }

        return coordinates
    }

    /**
     * Simplify coordinates using Douglas-Peucker algorithm
     * Useful for reducing the number of points while maintaining shape
     */
    fun simplifyCoordinates(
        coordinates: List<LatLng>,
        tolerance: Double = 0.0001 // ~11 meters
    ): List<LatLng> {
        if (coordinates.size <= 2) return coordinates

        return douglasPeucker(coordinates, tolerance)
    }

    private fun douglasPeucker(points: List<LatLng>, tolerance: Double): List<LatLng> {
        if (points.size <= 2) return points

        // Find the point with maximum distance from line segment
        var maxDistance = 0.0
        var maxIndex = 0
        val end = points.size - 1

        for (i in 1 until end) {
            val distance = perpendicularDistance(points[i], points[0], points[end])
            if (distance > maxDistance) {
                maxDistance = distance
                maxIndex = i
            }
        }

        // If max distance is greater than tolerance, recursively simplify
        return if (maxDistance > tolerance) {
            val left = douglasPeucker(points.subList(0, maxIndex + 1), tolerance)
            val right = douglasPeucker(points.subList(maxIndex, points.size), tolerance)
            left.dropLast(1) + right
        } else {
            listOf(points[0], points[end])
        }
    }

    private fun perpendicularDistance(point: LatLng, lineStart: LatLng, lineEnd: LatLng): Double {
        val x0 = point.latitude
        val y0 = point.longitude
        val x1 = lineStart.latitude
        val y1 = lineStart.longitude
        val x2 = lineEnd.latitude
        val y2 = lineEnd.longitude

        val numerator = Math.abs(
            (y2 - y1) * x0 - (x2 - x1) * y0 + x2 * y1 - y2 * x1
        )
        val denominator = Math.sqrt(
            (y2 - y1) * (y2 - y1) + (x2 - x1) * (x2 - x1)
        )

        return if (denominator == 0.0) 0.0 else numerator / denominator
    }
}