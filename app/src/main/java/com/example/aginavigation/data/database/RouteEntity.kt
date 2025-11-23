package com.example.aginavigation.data.database

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import com.google.android.gms.maps.model.LatLng
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

@Entity(tableName = "routes")
data class RouteEntity(
    @PrimaryKey val id: Int,
    val title: String,
    val fareMin: Double,
    val fareMax: Double,
    val summary: String,
    val category: String,
    val coordinates: String, // JSON string of List<LatLng>
    val isActive: Boolean = true
)

@Entity(
    tableName = "route_stops",
    foreignKeys = [
        ForeignKey(
            entity = RouteEntity::class,
            parentColumns = ["id"],
            childColumns = ["routeId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("routeId")]
)
data class RouteStopEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val routeId: Int,
    val stopOrder: Int,
    val name: String,
    val description: String,
    val latitude: Double,
    val longitude: Double,
    val etaMinutes: Int?,
    val isStart: Boolean = false,
    val isEnd: Boolean = false
)

@Entity(tableName = "destinations")
data class DestinationEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val description: String,
    val category: String,
    val latitude: Double,
    val longitude: Double,
    val isPopular: Boolean = false,
    val imageUrl: String? = null
)

@Entity(tableName = "favorite_routes")
data class FavoriteRouteEntity(
    @PrimaryKey val routeId: Int,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "search_history")
data class SearchHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val query: String,
    val timestamp: Long = System.currentTimeMillis()
)

class Converters {
    private val gson = Gson()

    @TypeConverter
    fun fromLatLngList(value: List<LatLng>?): String? {
        return value?.let { gson.toJson(it) }
    }

    @TypeConverter
    fun toLatLngList(value: String?): List<LatLng>? {
        return value?.let {
            val type = object : TypeToken<List<LatLng>>() {}.type
            gson.fromJson(it, type)
        }
    }
}
