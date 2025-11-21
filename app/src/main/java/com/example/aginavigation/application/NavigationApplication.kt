package com.example.aginavigation.application

import android.app.Application
import com.example.aginavigation.data.database.AppDatabase
import com.example.aginavigation.data.repository.DestinationRepository
import com.example.aginavigation.data.repository.FavoriteRepository
import com.example.aginavigation.data.repository.RouteRepository
import com.example.aginavigation.data.repository.SearchHistoryRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

class NavigationApplication : Application() {
    val applicationScope = CoroutineScope(SupervisorJob())

    val database by lazy { AppDatabase.getDatabase(this, applicationScope) }
    val routeRepository by lazy { RouteRepository(database.routeDao(), database.routeStopDao()) }
    val destinationRepository by lazy { DestinationRepository(database.destinationDao()) }
    val favoriteRepository by lazy { FavoriteRepository(database.favoriteRouteDao()) }
    val searchHistoryRepository by lazy { SearchHistoryRepository(database.searchHistoryDao()) }
}
