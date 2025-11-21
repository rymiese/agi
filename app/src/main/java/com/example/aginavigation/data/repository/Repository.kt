package com.example.aginavigation.data.repository

import androidx.lifecycle.LiveData
import com.example.aginavigation.data.database.DestinationDao
import com.example.aginavigation.data.database.DestinationEntity
import com.example.aginavigation.data.database.FavoriteRouteDao
import com.example.aginavigation.data.database.FavoriteRouteEntity
import com.example.aginavigation.data.database.RouteDao
import com.example.aginavigation.data.database.RouteEntity
import com.example.aginavigation.data.database.RouteStopDao
import com.example.aginavigation.data.database.RouteStopEntity
import com.example.aginavigation.data.database.SearchHistoryDao
import com.example.aginavigation.data.database.SearchHistoryEntity

class RouteRepository(private val routeDao: RouteDao, private val routeStopDao: RouteStopDao) {
    val allRoutes: LiveData<List<RouteEntity>> = routeDao.getAllRoutes()

    fun getRoutesByCategory(category: String): List<RouteEntity> {
        return routeDao.getRoutesByCategory(category)
    }

    suspend fun getRouteById(id: Int): RouteEntity? {
        return routeDao.getRouteById(id)
    }

    fun searchRoutes(query: String): LiveData<List<RouteEntity>> {
        return routeDao.searchRoutes(query)
    }

    suspend fun insertRoute(route: RouteEntity) {
        routeDao.insertRoute(route)
    }

    fun getStopsForRoute(routeId: Int): LiveData<List<RouteStopEntity>> {
        return routeStopDao.getStopsForRoute(routeId)
    }
}

class DestinationRepository(private val destinationDao: DestinationDao) {
    val allDestinations: LiveData<List<DestinationEntity>> = destinationDao.getAllDestinations()
    val popularDestinations: LiveData<List<DestinationEntity>> = destinationDao.getPopularDestinations()

    fun searchDestinations(query: String): LiveData<List<DestinationEntity>> {
        return destinationDao.searchDestinations(query)
    }

    suspend fun insertDestination(destination: DestinationEntity) {
        destinationDao.insertDestination(destination)
    }
}

class FavoriteRepository(private val favoriteRouteDao: FavoriteRouteDao) {
    val allFavorites: LiveData<List<FavoriteRouteEntity>> = favoriteRouteDao.getAllFavorites()

    fun isFavorite(routeId: Int): LiveData<Boolean> {
        return favoriteRouteDao.isFavorite(routeId)
    }

    suspend fun addFavorite(routeId: Int) {
        favoriteRouteDao.addFavorite(FavoriteRouteEntity(routeId))
    }

    suspend fun removeFavorite(routeId: Int) {
        favoriteRouteDao.removeFavorite(routeId)
    }
}

class SearchHistoryRepository(private val searchHistoryDao: SearchHistoryDao) {
    val recentSearches: LiveData<List<SearchHistoryEntity>> = searchHistoryDao.getRecentSearches()

    suspend fun addSearch(query: String) {
        searchHistoryDao.addSearch(SearchHistoryEntity(query = query))
    }

    suspend fun clearHistory() {
        searchHistoryDao.clearHistory()
    }
}
