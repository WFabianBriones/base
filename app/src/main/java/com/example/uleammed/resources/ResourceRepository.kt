package com.example.uleammed.resources

import android.content.Context
import android.content.SharedPreferences
import com.google.firebase.auth.FirebaseAuth
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Repository para gestión de recursos
 *
 * Por ahora usa datos locales (SharedPreferences + datos de muestra)
 * Fácilmente escalable a Firebase Firestore en el futuro
 */
class ResourceRepository(private val context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        "resource_data",
        Context.MODE_PRIVATE
    )
    private val gson = Gson()
    private val auth = FirebaseAuth.getInstance()

    companion object {
        private const val KEY_FAVORITES = "favorites"
        private const val KEY_READING_HISTORY = "reading_history"
        private const val KEY_STATS = "stats"
        private const val TAG = "ResourceRepository"
    }

    /**
     * Obtener todos los recursos (datos de muestra por ahora)
     */
    suspend fun getAllResources(): Result<List<ResourceItem>> = withContext(Dispatchers.IO) {
        return@withContext try {
            // En producción, esto vendría de Firebase Firestore
            val resources = SampleResourceData.getSampleResources()

            // Cargar estado de favoritos del usuario
            val favorites = getFavoriteIds()
            val updatedResources = resources.map { resource ->
                resource.copy(isFavorite = favorites.contains(resource.id))
            }

            Result.success(updatedResources)
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error loading resources", e)
            Result.failure(e)
        }
    }

    /**
     * Buscar recursos con filtros
     */
    suspend fun searchResources(filter: ResourceFilter): Result<List<ResourceItem>> = withContext(Dispatchers.IO) {
        return@withContext try {
            val allResources = getAllResources().getOrThrow()

            val filtered = allResources.filter { resource ->
                // Filtro de búsqueda de texto
                val matchesSearch = if (filter.searchQuery.isBlank()) {
                    true
                } else {
                    resource.title.contains(filter.searchQuery, ignoreCase = true) ||
                            resource.summary.contains(filter.searchQuery, ignoreCase = true) ||
                            resource.tags.any { it.contains(filter.searchQuery, ignoreCase = true) }
                }

                // Filtro de tipo
                val matchesType = filter.types.isEmpty() || filter.types.contains(resource.type)

                // Filtro de categoría
                val matchesCategory = filter.categories.isEmpty() || filter.categories.contains(resource.category)

                // Filtro de dificultad
                val matchesDifficulty = filter.difficulties.isEmpty() || filter.difficulties.contains(resource.difficulty)

                // Filtro de peer-reviewed
                val matchesPeerReviewed = !filter.peerReviewedOnly || resource.isPeerReviewed

                // Filtro de favoritos
                val matchesFavorites = !filter.favoritesOnly || resource.isFavorite

                // Filtro de video
                val matchesVideo = !filter.hasVideo || resource.videoUrl != null

                // Filtro de descarga
                val matchesDownload = !filter.hasDownload || resource.pdfUrl != null

                matchesSearch && matchesType && matchesCategory && matchesDifficulty &&
                        matchesPeerReviewed && matchesFavorites && matchesVideo && matchesDownload
            }

            Result.success(filtered)
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error searching resources", e)
            Result.failure(e)
        }
    }

    /**
     * Obtener recurso por ID
     */
    suspend fun getResourceById(id: String): Result<ResourceItem> = withContext(Dispatchers.IO) {
        return@withContext try {
            val resources = getAllResources().getOrThrow()
            val resource = resources.find { it.id == id }
                ?: throw IllegalArgumentException("Resource not found: $id")

            Result.success(resource)
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error getting resource by ID", e)
            Result.failure(e)
        }
    }

    /**
     * Obtener recursos por categoría
     */
    suspend fun getResourcesByCategory(category: ResourceCategory): Result<List<ResourceItem>> {
        return searchResources(ResourceFilter(categories = setOf(category)))
    }

    /**
     * Obtener recursos relacionados
     */
    suspend fun getRelatedResources(resourceId: String): Result<List<ResourceItem>> = withContext(Dispatchers.IO) {
        return@withContext try {
            val resource = getResourceById(resourceId).getOrThrow()
            val allResources = getAllResources().getOrThrow()

            val related = allResources.filter {
                it.id in resource.relatedResourceIds ||
                        it.category == resource.category && it.id != resourceId
            }.take(5)

            Result.success(related)
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error getting related resources", e)
            Result.failure(e)
        }
    }

    /**
     * Toggle favorito
     */
    suspend fun toggleFavorite(resourceId: String): Result<Unit> = withContext(Dispatchers.IO) {
        return@withContext try {
            val userId = auth.currentUser?.uid ?: "guest"
            val favorites = getFavoriteIds(userId).toMutableSet()

            if (favorites.contains(resourceId)) {
                favorites.remove(resourceId)
            } else {
                favorites.add(resourceId)
            }

            saveFavoriteIds(userId, favorites)

            android.util.Log.d(TAG, "Favorite toggled: $resourceId, isFavorite: ${favorites.contains(resourceId)}")
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error toggling favorite", e)
            Result.failure(e)
        }
    }

    /**
     * Obtener favoritos del usuario
     */
    suspend fun getFavorites(): Result<List<ResourceItem>> = withContext(Dispatchers.IO) {
        return@withContext try {
            val favoriteIds = getFavoriteIds()
            val allResources = getAllResources().getOrThrow()

            val favorites = allResources.filter { it.id in favoriteIds }

            Result.success(favorites)
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error getting favorites", e)
            Result.failure(e)
        }
    }

    /**
     * Registrar lectura
     */
    suspend fun trackReading(resourceId: String, progress: Float = 1.0f): Result<Unit> = withContext(Dispatchers.IO) {
        return@withContext try {
            val userId = auth.currentUser?.uid ?: "guest"
            val history = getReadingHistory(userId).toMutableList()

            // Actualizar o agregar registro
            val existingIndex = history.indexOfFirst { it.resourceId == resourceId }
            val newRecord = ReadingHistory(
                resourceId = resourceId,
                timestamp = System.currentTimeMillis(),
                progress = progress,
                completedReading = progress >= 1.0f
            )

            if (existingIndex >= 0) {
                history[existingIndex] = newRecord
            } else {
                history.add(0, newRecord)
            }

            // Mantener solo los últimos 100 registros
            if (history.size > 100) {
                history.subList(100, history.size).clear()
            }

            saveReadingHistory(userId, history)

            // Actualizar estadísticas
            updateStats(userId)

            android.util.Log.d(TAG, "Reading tracked: $resourceId, progress: $progress")
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error tracking reading", e)
            Result.failure(e)
        }
    }

    /**
     * Obtener historial de lectura
     */
    suspend fun getReadingHistoryList(): Result<List<ReadingHistory>> = withContext(Dispatchers.IO) {
        return@withContext try {
            val userId = auth.currentUser?.uid ?: "guest"
            val history = getReadingHistory(userId)
            Result.success(history)
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error getting reading history", e)
            Result.failure(e)
        }
    }

    /**
     * Obtener estadísticas del usuario
     */
    suspend fun getUserStats(): Result<ResourceStats> = withContext(Dispatchers.IO) {
        return@withContext try {
            val userId = auth.currentUser?.uid ?: "guest"
            val json = prefs.getString("${KEY_STATS}_$userId", null)

            val stats = if (json != null) {
                gson.fromJson(json, ResourceStats::class.java)
            } else {
                ResourceStats()
            }

            Result.success(stats)
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error getting stats", e)
            Result.failure(e)
        }
    }

    /**
     * Obtener ejercicios de muestra
     */
    suspend fun getExercises(): Result<List<ExerciseResource>> = withContext(Dispatchers.IO) {
        return@withContext try {
            val exercises = SampleResourceData.getSampleExercises()
            Result.success(exercises)
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error getting exercises", e)
            Result.failure(e)
        }
    }

    /**
     * Obtener FAQs
     */
    suspend fun getFAQs(category: ResourceCategory? = null): Result<List<FAQItem>> = withContext(Dispatchers.IO) {
        return@withContext try {
            val faqs = SampleResourceData.getSampleFAQs()

            val filtered = if (category != null) {
                faqs.filter { it.category == category }
            } else {
                faqs
            }

            Result.success(filtered)
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error getting FAQs", e)
            Result.failure(e)
        }
    }

    // ==================== PRIVATE HELPERS ====================

    private fun getFavoriteIds(userId: String = auth.currentUser?.uid ?: "guest"): Set<String> {
        val json = prefs.getString("${KEY_FAVORITES}_$userId", null)
        return if (json != null) {
            val type = object : TypeToken<Set<String>>() {}.type
            gson.fromJson(json, type)
        } else {
            emptySet()
        }
    }

    private fun saveFavoriteIds(userId: String, favorites: Set<String>) {
        val json = gson.toJson(favorites)
        prefs.edit().putString("${KEY_FAVORITES}_$userId", json).apply()
    }

    private fun getReadingHistory(userId: String): List<ReadingHistory> {
        val json = prefs.getString("${KEY_READING_HISTORY}_$userId", null)
        return if (json != null) {
            val type = object : TypeToken<List<ReadingHistory>>() {}.type
            gson.fromJson(json, type)
        } else {
            emptyList()
        }
    }

    private fun saveReadingHistory(userId: String, history: List<ReadingHistory>) {
        val json = gson.toJson(history)
        prefs.edit().putString("${KEY_READING_HISTORY}_$userId", json).apply()
    }

    private fun updateStats(userId: String) {
        val history = getReadingHistory(userId)
        val favorites = getFavoriteIds(userId)

        // Calcular estadísticas
        val totalRead = history.count { it.completedReading }
        val totalSaved = favorites.size

        // Calcular categoría favorita
        val resources = runCatching {
            kotlinx.coroutines.runBlocking {
                getAllResources().getOrThrow()
            }
        }.getOrNull() ?: emptyList()

        val readResourceIds = history.filter { it.completedReading }.map { it.resourceId }
        val readCategories = resources.filter { it.id in readResourceIds }.map { it.category }
        val favoriteCategory = readCategories.groupingBy { it }.eachCount().maxByOrNull { it.value }?.key

        val stats = ResourceStats(
            totalRead = totalRead,
            totalSaved = totalSaved,
            favoriteCategory = favoriteCategory
        )

        val json = gson.toJson(stats)
        prefs.edit().putString("${KEY_STATS}_$userId", json).apply()
    }
}