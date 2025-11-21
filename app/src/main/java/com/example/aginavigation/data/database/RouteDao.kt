package com.example.aginavigation.data.database

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface RouteDao {
    @Query("SELECT * FROM routes ORDER BY title ASC")
    fun getAllRoutes(): LiveData<List<RouteEntity>>

    @Query("SELECT * FROM routes WHERE category = :category")
    fun getRoutesByCategory(category: String): List<RouteEntity>

    @Query("SELECT * FROM routes WHERE id = :id")
    suspend fun getRouteById(id: Int): RouteEntity?

    @Query("SELECT * FROM routes WHERE title LIKE '%' || :query || '%' OR summary LIKE '%' || :query || '%'")
    fun searchRoutes(query: String): LiveData<List<RouteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoute(route: RouteEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(routes: List<RouteEntity>)

    @Delete
    suspend fun deleteRoute(route: RouteEntity)

    @Query("DELETE FROM routes")
    suspend fun deleteAll()
}

@Dao
interface RouteStopDao {
    @Query("SELECT * FROM route_stops WHERE routeId = :routeId ORDER BY stopOrder ASC")
    fun getStopsForRoute(routeId: Int): LiveData<List<RouteStopEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStop(stop: RouteStopEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(stops: List<RouteStopEntity>)
}

@Dao
interface DestinationDao {
    @Query("SELECT * FROM destinations ORDER BY isPopular DESC, name ASC")
    fun getAllDestinations(): LiveData<List<DestinationEntity>>

    @Query("SELECT * FROM destinations WHERE isPopular = 1")
    fun getPopularDestinations(): LiveData<List<DestinationEntity>>

    @Query("SELECT * FROM destinations WHERE name LIKE '%' || :query || '%'")
    fun searchDestinations(query: String): LiveData<List<DestinationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDestination(destination: DestinationEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(destinations: List<DestinationEntity>)
    
    @Query("DELETE FROM destinations")
    suspend fun deleteAll()
}

@Dao
interface FavoriteRouteDao {
    @Query("SELECT * FROM favorite_routes ORDER BY timestamp DESC")
    fun getAllFavorites(): LiveData<List<FavoriteRouteEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_routes WHERE routeId = :routeId)")
    fun isFavorite(routeId: Int): LiveData<Boolean>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addFavorite(favorite: FavoriteRouteEntity)

    @Query("DELETE FROM favorite_routes WHERE routeId = :routeId")
    suspend fun removeFavorite(routeId: Int)
}

@Dao
interface SearchHistoryDao {
    @Query("SELECT * FROM search_history ORDER BY timestamp DESC LIMIT 10")
    fun getRecentSearches(): LiveData<List<SearchHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addSearch(search: SearchHistoryEntity)

    @Query("DELETE FROM search_history")
    suspend fun clearHistory()
}
