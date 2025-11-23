package com.example.aginavigation.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.aginavigation.utils.KmlParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

// --- KML Import Constants ---
private const val SHARED_PREFS_NAME = "kml_import_prefs"
private const val DB_VERSION_KEY = "db_version"
private const val CURRENT_DB_VERSION = 12 // Increment when routes/destinations change

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
         * Called only when the database is first created
         */
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    populateDatabase(
                        database.routeDao(),
                        database.destinationDao()
                    )
                }
            }
        }

        /**
         * Called every time the database is opened
         */
        override fun onOpen(db: SupportSQLiteDatabase) {
            super.onOpen(db)
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    checkAndUpdateData(
                        database.routeDao(),
                        database.destinationDao()
                    )
                }
            }
        }

        /**
         * Check if data needs updating based on version
         */
        private suspend fun checkAndUpdateData(
            routeDao: RouteDao,
            destinationDao: DestinationDao
        ) {
            val prefs = context.getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE)
            val savedVersion = prefs.getInt(DB_VERSION_KEY, 0)

            if (savedVersion < CURRENT_DB_VERSION) {
                android.util.Log.d(
                    "AppDatabase",
                    "Database version outdated ($savedVersion < $CURRENT_DB_VERSION). Updating data..."
                )

                // Clear existing data
                routeDao.deleteAll()
                destinationDao.deleteAll()

                // Repopulate with fresh data
                populateDatabase(routeDao, destinationDao)

                // Update version
                prefs.edit()
                    .putInt(DB_VERSION_KEY, CURRENT_DB_VERSION)
                    .apply()

                android.util.Log.d("AppDatabase", "Database updated to version $CURRENT_DB_VERSION")
            }
        }

        /**
         * Populate database with all routes and destinations from KML and hardcoded data
         */
        private suspend fun populateDatabase(
            routeDao: RouteDao,
            destinationDao: DestinationDao
        ) {
            // Import all routes from KML files
            importAllKmlRoutes(routeDao)

            // Populate destinations
            populateDestinations(destinationDao)
        }

        /**
         * Import all KML routes with complete configuration
         */
        private suspend fun importAllKmlRoutes(routeDao: RouteDao) {
            val parser = KmlParser()
            val gson = com.google.gson.Gson()

            data class RouteConfig(
                val fileName: String,
                val routeId: Int,
                val category: String,
                val fareMin: Double,
                val fareMax: Double,
                val customSummary: String? = null,
                val customTitle: String? = null
            )

            // ⚠️ COMPLETE LIST OF ALL KML ROUTES ⚠️
            // Add new routes here to include them in the app
            val allKmlConfigs = listOf(
                // Routes 1-3: Original circular Daraga-Legazpi routes
                RouteConfig(
                    fileName = "kml/route_1_daraga-legazpi (A).kml",
                    routeId = 1,
                    category = "Jeepney",
                    fareMin = 13.0,
                    fareMax = 15.0,
                    customTitle = "Daraga - Legazpi City (A)",
                    customSummary = "Circular jeepney route between Daraga and Legazpi City via Washington Drive, returning via Old Albay."

                ),
                RouteConfig(
                    fileName = "kml/route_2_daraga-legazpi (B).kml",
                    routeId = 2,
                    category = "Jeepney",
                    fareMin = 13.0,
                    fareMax = 15.0,
                    customTitle = "Daraga - Legazpi City (B)",
                    customSummary = "Circular jeepney route between Daraga and Legazpi City via Old Albay, returning via Washington Drive (clockwise variant)."
                ),
                RouteConfig(
                    fileName = "kml/route_3_ligao-legazpi.kml",
                    routeId = 3,
                    category = "Jeepney",
                    fareMin = 13.0,
                    fareMax = 15.0,
                    customTitle = "Ligao - Legazpi",
                    customSummary = "Direct route connecting Ligao and Legazpi."
                ),
                RouteConfig(
                    fileName = "kml/route_4_malabog-legazpi.kml",
                    routeId = 4,
                    category = "Jeepney",
                    fareMin = 13.0,
                    fareMax = 15.0,
                    customTitle = "Malabog - Legazpi",
                    customSummary = "Direct route connecting Malabog to Legazpi City via the main highway."
                ),
                RouteConfig(
                    fileName = "kml/route_5_camalig-legazpi.kml",
                    routeId = 5,
                    category = "Jeepney",
                    fareMin = 18.0,
                    fareMax = 22.0,
                    customTitle = "Camalig - Legazpi",
                    customSummary = "Long distance route from Camalig to Legazpi with multiple stops along the way."
                ),
                // Routes 6-10: Additional routes (from document)
                RouteConfig(
                    fileName = "kml/route_6_loop 1.kml",
                    routeId = 6,
                    category = "Jeepney",
                    fareMin = 13.0,
                    fareMax = 15.0,
                    customTitle = "Loop Route 1",
                    customSummary = "City loop serving major commercial and educational institutions."
                ),
                RouteConfig(
                    fileName = "kml/route_7_loop 2.kml",
                    routeId = 7,
                    category = "Jeepney",
                    fareMin = 13.0,
                    fareMax = 15.0,
                    customTitle = "Loop Route 2",
                    customSummary = "Alternative city loop covering residential and business districts."
                ),
                RouteConfig(
                    fileName = "kml/route_8_santo domingo.kml",
                    routeId = 8,
                    category = "Jeepney",
                    fareMin = 20.0,
                    fareMax = 25.0,
                    customTitle = "Santo Domingo Route",
                    customSummary = "Route connecting Legazpi to Santo Domingo municipality."
                ),
                RouteConfig(
                    fileName = "kml/route_9_oas-legazpi.kml",
                    routeId = 9,
                    category = "Jeepney",
                    fareMin = 25.0,
                    fareMax = 30.0,
                    customTitle = "Oas - Legazpi",
                    customSummary = "Long-distance route connecting Oas municipality to Legazpi City."
                ),
                RouteConfig(
                    fileName = "kml/route_10_polangui-legazpi.kml",
                    routeId = 10,
                    category = "Jeepney",
                    fareMin = 30.0,
                    fareMax = 35.0,
                    customTitle = "Polangui - Legazpi",
                    customSummary = "Extended route from Polangui to Legazpi City via major towns."
                ),
                RouteConfig(
                    fileName = "kml/route_11_guinobatan-legazpi.kml",
                    routeId = 11,
                    category = "Jeepney",
                    fareMin = 30.0,
                    fareMax = 35.0,
                    customTitle = "Guinobatan - Legazpi",
                    customSummary = "Extended route from Polangui to Legazpi City via major towns."
                )
            )

            // Import each route
            allKmlConfigs.forEach { config ->
                try {
                    val kmlRoute = parser.parseKmlFromAssets(context, config.fileName)
                    if (kmlRoute != null) {
                        // Simplify coordinates for better performance
                        val simplifiedCoords = parser.simplifyCoordinates(kmlRoute.coordinates)

                        // Convert LatLng to simple coordinate objects for proper JSON serialization
                        val coordinatePoints = simplifiedCoords.map { latLng ->
                            mapOf("latitude" to latLng.latitude, "longitude" to latLng.longitude)
                        }

                        // Use custom values if provided, otherwise use KML data
                        val routeTitle = config.customTitle ?: kmlRoute.name
                        val routeSummary = config.customSummary ?: kmlRoute.description

                        val newRoute = RouteEntity(
                            id = config.routeId,
                            title = routeTitle,
                            fareMin = config.fareMin,
                            fareMax = config.fareMax,
                            summary = routeSummary,
                            category = config.category,
                            coordinates = gson.toJson(coordinatePoints)
                        )

                        routeDao.insertRoute(newRoute)

                        android.util.Log.d(
                            "AppDatabase",
                            "✓ Imported route ${config.routeId}: $routeTitle " +
                                    "(${simplifiedCoords.size} points, ₱${config.fareMin}-₱${config.fareMax})"
                        )

                    } else {
                        android.util.Log.w(
                            "AppDatabase",
                            "✗ Could not parse KML file: ${config.fileName}"
                        )
                    }
                } catch (e: Exception) {
                    android.util.Log.e(
                        "AppDatabase",
                        "✗ Error importing ${config.fileName}: ${e.message}",
                        e
                    )
                }
            }
        }

        /**
         * Populate destination points of interest
         */
        private suspend fun populateDestinations(destinationDao: DestinationDao) {
            val destinations = listOf(
                DestinationEntity(
                    name = "Cagsawa Ruins",
                    description = "Historic ruins with views of Mayon Volcano",
                    category = "Tourist",
                    latitude = 13.256,
                    longitude = 123.684,
                    isPopular = true
                ),
                DestinationEntity(
                    name = "Pacific Mall Legazpi",
                    description = "Major shopping center in downtown Legazpi",
                    category = "Shopping",
                    latitude = 13.1395,
                    longitude = 123.7350,
                    isPopular = true
                ),
                DestinationEntity(
                    name = "Embarcadero de Legazpi",
                    description = "Waterfront lifestyle hub with restaurants and shops",
                    category = "Lifestyle",
                    latitude = 13.1445,
                    longitude = 123.7425,
                    isPopular = true
                ),
                DestinationEntity(
                    name = "Bicol University",
                    description = "Premier state university in the Bicol Region",
                    category = "Education",
                    latitude = 13.1389,
                    longitude = 123.7297,
                    isPopular = true
                ),
                DestinationEntity(
                    name = "Mayon Volcano Natural Park",
                    description = "Active volcano and natural park",
                    category = "Nature",
                    latitude = 13.257,
                    longitude = 123.685,
                    isPopular = true
                ),
                DestinationEntity(
                    name = "Legazpi Boulevard",
                    description = "Scenic coastal road and promenade",
                    category = "Tourist",
                    latitude = 13.1470,
                    longitude = 123.7480,
                    isPopular = true
                ),
                DestinationEntity(
                    name = "Daraga Church (Our Lady of the Gate)",
                    description = "Historic baroque church with panoramic views",
                    category = "Religious",
                    latitude = 13.1548,
                    longitude = 123.7138,
                    isPopular = true
                ),
                DestinationEntity(
                    name = "Ligñon Hill Nature Park",
                    description = "Nature park with hiking trails and viewpoints",
                    category = "Nature",
                    latitude = 13.1612,
                    longitude = 123.7289,
                    isPopular = true
                ),
                DestinationEntity(
                    name = "Quitinday Hills",
                    description = "Green hills offering panoramic views",
                    category = "Nature",
                    latitude = 13.1755,
                    longitude = 123.6505,
                    isPopular = true
                ),
                DestinationEntity(
                    name = "Albay Park and Wildlife",
                    description = "Wildlife sanctuary and zoological park",
                    category = "Nature",
                    latitude = 13.1587,
                    longitude = 123.7341,
                    isPopular = true
                ),
                DestinationEntity(
                    name = "LCC Legazpi",
                    description = "Legazpi City Colleges - Main Campus",
                    category = "Education",
                    latitude = 13.1448,
                    longitude = 123.7385,
                    isPopular = false
                ),
                DestinationEntity(
                    name = "LCC Daraga",
                    description = "Legazpi City Colleges - Daraga Campus",
                    category = "Education",
                    latitude = 13.1482,
                    longitude = 123.7118,
                    isPopular = false
                ),
                DestinationEntity(
                    name = "Ayala Malls Legazpi",
                    description = "Modern shopping mall complex",
                    category = "Shopping",
                    latitude = 13.1425,
                    longitude = 123.7395,
                    isPopular = true
                ),
                DestinationEntity(
                    name = "Legazpi City Grand Terminal",
                    description = "Main jeepney and bus terminal",
                    category = "Transport",
                    latitude = 13.1437,
                    longitude = 123.7459,
                    isPopular = false
                )
            )

            destinationDao.insertAll(destinations)

            android.util.Log.d(
                "AppDatabase",
                "✓ Populated ${destinations.size} destinations"
            )
        }
    }
}