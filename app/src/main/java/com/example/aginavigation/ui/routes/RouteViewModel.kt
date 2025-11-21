package com.example.aginavigation.ui.routes

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.aginavigation.data.database.DestinationEntity
import com.example.aginavigation.data.database.RouteEntity
import com.example.aginavigation.data.repository.DestinationRepository
import com.example.aginavigation.data.repository.FavoriteRepository
import com.example.aginavigation.data.repository.RouteRepository
import com.example.aginavigation.data.repository.SearchHistoryRepository
import kotlinx.coroutines.launch

class RouteViewModel(
    private val routeRepository: RouteRepository,
    private val destinationRepository: DestinationRepository,
    private val favoriteRepository: FavoriteRepository,
    private val searchHistoryRepository: SearchHistoryRepository
) : ViewModel() {

    val allRoutes: LiveData<List<RouteEntity>> = routeRepository.allRoutes
    val allDestinations: LiveData<List<DestinationEntity>> = destinationRepository.allDestinations
    val popularDestinations: LiveData<List<DestinationEntity>> = destinationRepository.popularDestinations

    fun searchRoutes(query: String): LiveData<List<RouteEntity>> {
        return routeRepository.searchRoutes(query)
    }

    fun searchDestinations(query: String): LiveData<List<DestinationEntity>> {
        return destinationRepository.searchDestinations(query)
    }
    
    fun addToSearchHistory(query: String) {
        viewModelScope.launch {
            searchHistoryRepository.addSearch(query)
        }
    }

    fun toggleFavorite(routeId: Int) {
        viewModelScope.launch {
            // Check if favorite, then toggle. Logic simplified for example.
            // Ideally we observe isFavorite(routeId) in UI.
            // Here we might need to check current state or just expose add/remove methods.
        }
    }
    
    fun addFavorite(routeId: Int) {
        viewModelScope.launch {
            favoriteRepository.addFavorite(routeId)
        }
    }
    
    fun removeFavorite(routeId: Int) {
        viewModelScope.launch {
            favoriteRepository.removeFavorite(routeId)
        }
    }
}

class RouteViewModelFactory(
    private val routeRepository: RouteRepository,
    private val destinationRepository: DestinationRepository,
    private val favoriteRepository: FavoriteRepository,
    private val searchHistoryRepository: SearchHistoryRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RouteViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return RouteViewModel(
                routeRepository,
                destinationRepository,
                favoriteRepository,
                searchHistoryRepository
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
