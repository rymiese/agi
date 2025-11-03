package com.example.aginavigation.ui.routes

import java.io.Serializable

// Simple serializable route detail model so we can pass structured route details in a Bundle.
data class RouteStop(
    val name: String,
    val description: String,
    val etaMinutes: Int? = null
) : Serializable

data class RouteDetailInfo(
    val start: RouteStop,
    val stops: List<RouteStop>,
    val end: RouteStop
) : Serializable

object RouteInfoProvider {
    fun getRouteInfo(routeId: Int): RouteDetailInfo? {
        return when (routeId) {
            2 -> RouteDetailInfo(
                start = RouteStop("LCC Daraga", "Legazpi City Colleges - Daraga Campus", etaMinutes = null),
                stops = listOf(
                    RouteStop("Bicol University West (Main Campus)", "University campus", etaMinutes = 3),
                    RouteStop("St. Gregory the Great Cathedral", "Historic cathedral", etaMinutes = 8),
                    RouteStop("Robinsons Supermarket", "Shopping center", etaMinutes = 12),
                    RouteStop("Saint Agnes Academy", "Educational institution", etaMinutes = 15),
                    RouteStop("LCC Legazpi", "Legazpi City Colleges - Main Campus", etaMinutes = 18),
                    RouteStop("Ayala Malls Legazpi", "Major shopping mall", etaMinutes = 22),
                    RouteStop("Pacific Mall", "Shopping center", etaMinutes = 25),
                    RouteStop("Yashano Mall Enterprises", "Commercial center", etaMinutes = 28),
                    RouteStop("UST Legazpi Inc. Hospital", "Medical facility", etaMinutes = 32),
                    RouteStop("Bagumbayan Central School", "Public school", etaMinutes = 36),
                    RouteStop("St. Gregory the Great Cathedral", "Historic cathedral (return)", etaMinutes = 40),
                    RouteStop("Bicol University West (Main Campus)", "University campus (return)", etaMinutes = 44)
                ),
                end = RouteStop("LCC Daraga", "Legazpi City Colleges - Daraga Campus", etaMinutes = 48)
            )
            3 -> RouteDetailInfo(
                start = RouteStop("Starts: Daraga Center", "Town plaza", etaMinutes = null),
                stops = listOf(
                    RouteStop("Main Junction", "Main intersection", etaMinutes = 6),
                    RouteStop("Central Plaza", "Local community", etaMinutes = 12),
                    RouteStop("Market District", "Agricultural area", etaMinutes = 18)
                ),
                end = RouteStop("Ends: Legazpi Terminal", "End point", etaMinutes = 25)
            )
            else -> null
        }
    }
}

