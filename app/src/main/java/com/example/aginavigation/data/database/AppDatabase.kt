package com.example.aginavigation.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.aginavigation.data.RouteData
import com.example.aginavigation.utils.KmlParser
import com.google.android.gms.maps.model.LatLng
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

// --- KML Import Constants ---
private const val SHARED_PREFS_NAME = "kml_import_prefs"
private const val LAST_IMPORTED_ROUTE_ID_KEY = "last_imported_route_id"

@Database(
    entities = [
        RouteEntity::class,
        RouteStopEntity::class,
        DestinationEntity::class,
        FavoriteRouteEntity::class,
        SearchHistoryEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun routeDao(): RouteDao
    abstract fun routeStopDao(): RouteStopDao
    abstract fun destinationDao(): DestinationDao
    abstract fun favoriteRouteDao(): FavoriteRouteDao
    abstract fun searchHistoryDao(): SearchHistoryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "agi_navigation_database"
                )
                    .allowMainThreadQueries()
                    .addCallback(AppDatabaseCallback(context.applicationContext, scope))
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class AppDatabaseCallback(
        private val context: Context,
        private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {

        /**
         * Called only when the database is first created. Used for initial hardcoded data.
         */
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    populateDatabase(database.routeDao(), database.destinationDao())
                }
            }
        }

        /**
         * Called every time the database is opened. Used for checking and importing new KML files.
         */
        override fun onOpen(db: SupportSQLiteDatabase) {
            super.onOpen(db)
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    checkForNewKmlRoutes(database.routeDao())
                }
            }
        }

        // --- Initial Population (Runs only on onCreate) ---

        suspend fun populateDatabase(routeDao: RouteDao, destinationDao: DestinationDao) {
            // Populate Routes (existing hardcoded routes 1-3)
            val gson = Gson()

            // Route 1: Daraga - Legazpi City (A)
            val route1Points = RouteData.getRoutePoints(1)
            val route1 = RouteEntity(
                id = 1,
                title = "Daraga - Legazpi City (A)",
                fareMin = 13.0,
                fareMax = 15.0,
                summary = "A jeepney route between Daraga and Legazpi City via Washington Drive, returning via Old Albay.",
                category = "Jeepney",
                stops = 5,
                coordinates = gson.toJson(route1Points)
            )
            routeDao.insertRoute(route1)

            // Route 2: Daraga - Legazpi City (B)
            val route2Points = RouteData.getRoutePoints(2)
            val route2 = RouteEntity(
                id = 2,
                title = "Daraga - Legazpi City (B)",
                fareMin = 13.0,
                fareMax = 15.0,
                summary = "A jeepney route between Daraga and Legazpi City via Old Albay, returning via Washington Drive.",
                category = "Jeepney",
                stops = 5,
                coordinates = gson.toJson(route2Points)
            )
            routeDao.insertRoute(route2)

            // Route 3
            val route3Points = RouteData.getRoutePoints(3)
            if (route3Points.isNotEmpty()) {
                val route3 = RouteEntity(
                    id = 3,
                    title = "Legazpi - Daraga Loop",
                    fareMin = 13.0,
                    fareMax = 15.0,
                    summary = "Loop route connecting Legazpi and Daraga.",
                    category = "Jeepney",
                    stops = 10,
                    coordinates = gson.toJson(route3Points)
                )
                routeDao.insertRoute(route3)
            }

            // Populate Destinations
            val destinations = listOf(
                DestinationEntity(name = "Cagsawa Ruins", description = "Historic ruins", category = "Tourist", latitude = 13.14, longitude = 123.71, isPopular = true),
                DestinationEntity(name = "Pacific Mall Legazpi", description = "Shopping mall", category = "Shopping", latitude = 13.14, longitude = 123.73, isPopular = true),
                DestinationEntity(name = "Embarcadero de Legazpi", description = "Waterfront lifestyle hub", category = "Lifestyle", latitude = 13.14, longitude = 123.74, isPopular = true),
                DestinationEntity(name = "Bicol University", description = "Premier university", category = "Education", latitude = 13.14, longitude = 123.72, isPopular = true),
                DestinationEntity(name = "Mayon Volcano Natural Park", description = "Natural park", category = "Nature", latitude = 13.25, longitude = 123.68, isPopular = true),
                DestinationEntity(name = "Legazpi Boulevard", description = "Coastal road", category = "Tourist", latitude = 13.15, longitude = 123.75, isPopular = true),
                DestinationEntity(name = "Daraga Church", description = "Historic church", category = "Religious", latitude = 13.15, longitude = 123.71, isPopular = true),
                DestinationEntity(name = "Ligñon Hill", description = "Nature park", category = "Nature", latitude = 13.15, longitude = 123.72, isPopular = true),
                DestinationEntity(name = "Quitinday Hills", description = "Hills", category = "Nature", latitude = 13.18, longitude = 123.65, isPopular = true),
                DestinationEntity(name = "Albay Park and Wildlife", description = "Wildlife park", category = "Nature", latitude = 13.16, longitude = 123.73, isPopular = true)
            )
            destinationDao.insertAll(destinations)
        }

        // --- KML Import Logic (Runs on onOpen) ---

        /**
         * Checks SharedPreferences for the last imported route ID and calls the import function
         * to process any newer KML files.
         */
        private suspend fun checkForNewKmlRoutes(routeDao: RouteDao) {
            val prefs = context.getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE)
            // Default to 4, as IDs 1-3 are hardcoded.
            val lastImportedId = prefs.getInt(LAST_IMPORTED_ROUTE_ID_KEY, 4)

            importKmlRoutes(routeDao, lastImportedId)
        }

        /**
         * Imports KML routes from assets that have an ID greater than or equal to the startingRouteId.
         * The list of KML configurations must be maintained here.
         */
        private suspend fun importKmlRoutes(routeDao: RouteDao, startingRouteId: Int) {
            val parser = KmlParser()
            val gson = Gson()

            // ⚠️ MAINTAIN THIS LIST OF ALL KML CONFIGS ⚠️
            val allKmlConfigs = listOf(
                Pair("kml/route_4_malabog-legazpi route.kml", 4),
                Pair("kml/route_5_camalig-legazpi.kml", 5),
                // Add new KML routes below this line with a progressively increasing, unique ID
                // Pair("kml/route_6_new_route.kml", 6),
            )

            var maxImportedId = startingRouteId

            // Filter to only import routes with an ID >= the starting ID to catch the first run
            // and any subsequent new files.
            allKmlConfigs.filter { it.second >= startingRouteId }.forEach { (fileName, routeId) ->
                try {
                    val kmlRoute = parser.parseKmlFromAssets(context, fileName)
                    if (kmlRoute != null) {
                        // Optionally simplify coordinates before saving
                        val simplifiedCoords = parser.simplifyCoordinates(kmlRoute.coordinates)

                        val newRoute = RouteEntity(
                            id = routeId,
                            title = kmlRoute.name,
                            fareMin = 15.0,
                            fareMax = 20.0,
                            summary = kmlRoute.description.ifEmpty { "Imported route from $fileName" },
                            category = "Inter-City",
                            stops = kmlRoute.placemarks.size,
                            coordinates = gson.toJson(simplifiedCoords)
                        )
                        // Use insertRoute to handle both inserts (new) and replacements (updates)
                        routeDao.insertRoute(newRoute)

                        // Update the highest successfully imported ID
                        if (routeId > maxImportedId) {
                            maxImportedId = routeId
                        }
                        android.util.Log.d("AppDatabase", "Successfully imported KML route $routeId: ${kmlRoute.name}")
                    } else {
                        android.util.Log.w("AppDatabase", "Could not parse KML file: $fileName")
                    }
                } catch (e: Exception) {
                    android.util.Log.e("AppDatabase", "Error importing KML file $fileName", e)
                }
            }

            // Save the highest imported ID back to SharedPreferences
            if (maxImportedId > startingRouteId) {
                context.getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE)
                    .edit()
                    .putInt(LAST_IMPORTED_ROUTE_ID_KEY, maxImportedId)
                    .apply()
            }
        }
    }
}