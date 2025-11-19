package com.example.uleammed.resources

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * ViewModel para gestión de recursos
 */
class ResourceViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ResourceRepository(application)

    // Estado de recursos
    private val _resources = MutableStateFlow<List<ResourceItem>>(emptyList())
    val resources: StateFlow<List<ResourceItem>> = _resources.asStateFlow()

    private val _filteredResources = MutableStateFlow<List<ResourceItem>>(emptyList())
    val filteredResources: StateFlow<List<ResourceItem>> = _filteredResources.asStateFlow()

    private val _state = MutableStateFlow<ResourceState>(ResourceState.Idle)
    val state: StateFlow<ResourceState> = _state.asStateFlow()

    // Filtros activos
    private val _currentFilter = MutableStateFlow(ResourceFilter())
    val currentFilter: StateFlow<ResourceFilter> = _currentFilter.asStateFlow()

    // Favoritos
    private val _favorites = MutableStateFlow<List<ResourceItem>>(emptyList())
    val favorites: StateFlow<List<ResourceItem>> = _favorites.asStateFlow()

    // Historial
    private val _readingHistory = MutableStateFlow<List<ReadingHistory>>(emptyList())
    val readingHistory: StateFlow<List<ReadingHistory>> = _readingHistory.asStateFlow()

    // Estadísticas
    private val _stats = MutableStateFlow(ResourceStats())
    val stats: StateFlow<ResourceStats> = _stats.asStateFlow()

    // Ejercicios
    private val _exercises = MutableStateFlow<List<ExerciseResource>>(emptyList())
    val exercises: StateFlow<List<ExerciseResource>> = _exercises.asStateFlow()

    // FAQs
    private val _faqs = MutableStateFlow<List<FAQItem>>(emptyList())
    val faqs: StateFlow<List<FAQItem>> = _faqs.asStateFlow()

    // Categoría seleccionada
    private val _selectedCategory = MutableStateFlow<ResourceCategory?>(null)
    val selectedCategory: StateFlow<ResourceCategory?> = _selectedCategory.asStateFlow()

    // Error
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    companion object {
        private const val TAG = "ResourceViewModel"
    }

    init {
        loadAllResources()
        loadFavorites()
        loadReadingHistory()
        loadStats()
        loadExercises()
        loadFAQs()
    }

    /**
     * Cargar todos los recursos
     */
    fun loadAllResources() {
        viewModelScope.launch {
            try {
                _state.value = ResourceState.Loading

                val result = withContext(Dispatchers.IO) {
                    repository.getAllResources()
                }

                result.onSuccess { resourceList ->
                    _resources.value = resourceList
                    _filteredResources.value = resourceList
                    _state.value = ResourceState.Success(resourceList)

                    android.util.Log.d(TAG, "✅ Recursos cargados: ${resourceList.size}")
                }.onFailure { exception ->
                    _state.value = ResourceState.Error(exception.message ?: "Error desconocido")
                    _error.value = exception.message

                    android.util.Log.e(TAG, "❌ Error cargando recursos", exception)
                }
            } catch (e: Exception) {
                _state.value = ResourceState.Error(e.message ?: "Error desconocido")
                _error.value = e.message

                android.util.Log.e(TAG, "❌ Error en loadAllResources", e)
            }
        }
    }

    /**
     * Aplicar filtros
     */
    fun applyFilter(filter: ResourceFilter) {
        viewModelScope.launch {
            try {
                _currentFilter.value = filter

                if (!filter.isActive()) {
                    // Sin filtros, mostrar todos
                    _filteredResources.value = _resources.value
                } else {
                    // Aplicar filtros
                    val result = withContext(Dispatchers.IO) {
                        repository.searchResources(filter)
                    }

                    result.onSuccess { filtered ->
                        _filteredResources.value = filtered

                        android.util.Log.d(TAG, "✅ Filtros aplicados: ${filtered.size} resultados")
                    }.onFailure { exception ->
                        _error.value = exception.message

                        android.util.Log.e(TAG, "❌ Error aplicando filtros", exception)
                    }
                }
            } catch (e: Exception) {
                _error.value = e.message

                android.util.Log.e(TAG, "❌ Error en applyFilter", e)
            }
        }
    }

    /**
     * Buscar por texto
     */
    fun search(query: String) {
        val newFilter = _currentFilter.value.copy(searchQuery = query)
        applyFilter(newFilter)
    }

    /**
     * Filtrar por categoría
     */
    fun filterByCategory(category: ResourceCategory?) {
        _selectedCategory.value = category

        val newFilter = if (category != null) {
            _currentFilter.value.copy(categories = setOf(category))
        } else {
            _currentFilter.value.copy(categories = emptySet())
        }

        applyFilter(newFilter)
    }

    /**
     * Filtrar por tipo
     */
    fun filterByType(type: ResourceType) {
        val currentTypes = _currentFilter.value.types.toMutableSet()

        if (currentTypes.contains(type)) {
            currentTypes.remove(type)
        } else {
            currentTypes.add(type)
        }

        val newFilter = _currentFilter.value.copy(types = currentTypes)
        applyFilter(newFilter)
    }

    /**
     * Toggle filtro de favoritos
     */
    fun toggleFavoritesFilter() {
        val newFilter = _currentFilter.value.copy(
            favoritesOnly = !_currentFilter.value.favoritesOnly
        )
        applyFilter(newFilter)
    }

    /**
     * Limpiar filtros
     */
    fun clearFilters() {
        applyFilter(ResourceFilter())
    }

    /**
     * Toggle favorito de un recurso
     */
    fun toggleFavorite(resourceId: String) {
        viewModelScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    repository.toggleFavorite(resourceId)
                }

                result.onSuccess {
                    // Recargar recursos y favoritos
                    loadAllResources()
                    loadFavorites()

                    android.util.Log.d(TAG, "✅ Favorito actualizado: $resourceId")
                }.onFailure { exception ->
                    _error.value = exception.message

                    android.util.Log.e(TAG, "❌ Error toggle favorito", exception)
                }
            } catch (e: Exception) {
                _error.value = e.message

                android.util.Log.e(TAG, "❌ Error en toggleFavorite", e)
            }
        }
    }

    /**
     * Cargar favoritos
     */
    private fun loadFavorites() {
        viewModelScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    repository.getFavorites()
                }

                result.onSuccess { favoriteList ->
                    _favorites.value = favoriteList

                    android.util.Log.d(TAG, "✅ Favoritos cargados: ${favoriteList.size}")
                }.onFailure { exception ->
                    android.util.Log.e(TAG, "❌ Error cargando favoritos", exception)
                }
            } catch (e: Exception) {
                android.util.Log.e(TAG, "❌ Error en loadFavorites", e)
            }
        }
    }

    /**
     * Registrar lectura
     */
    fun trackReading(resourceId: String, progress: Float = 1.0f) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    repository.trackReading(resourceId, progress)
                }

                // Recargar historial y estadísticas
                loadReadingHistory()
                loadStats()

                android.util.Log.d(TAG, "✅ Lectura registrada: $resourceId")
            } catch (e: Exception) {
                android.util.Log.e(TAG, "❌ Error en trackReading", e)
            }
        }
    }

    /**
     * Cargar historial de lectura
     */
    private fun loadReadingHistory() {
        viewModelScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    repository.getReadingHistoryList()
                }

                result.onSuccess { history ->
                    _readingHistory.value = history

                    android.util.Log.d(TAG, "✅ Historial cargado: ${history.size}")
                }.onFailure { exception ->
                    android.util.Log.e(TAG, "❌ Error cargando historial", exception)
                }
            } catch (e: Exception) {
                android.util.Log.e(TAG, "❌ Error en loadReadingHistory", e)
            }
        }
    }

    /**
     * Cargar estadísticas
     */
    private fun loadStats() {
        viewModelScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    repository.getUserStats()
                }

                result.onSuccess { userStats ->
                    _stats.value = userStats

                    android.util.Log.d(TAG, "✅ Estadísticas cargadas")
                }.onFailure { exception ->
                    android.util.Log.e(TAG, "❌ Error cargando estadísticas", exception)
                }
            } catch (e: Exception) {
                android.util.Log.e(TAG, "❌ Error en loadStats", e)
            }
        }
    }

    /**
     * Cargar ejercicios
     */
    private fun loadExercises() {
        viewModelScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    repository.getExercises()
                }

                result.onSuccess { exerciseList ->
                    _exercises.value = exerciseList

                    android.util.Log.d(TAG, "✅ Ejercicios cargados: ${exerciseList.size}")
                }.onFailure { exception ->
                    android.util.Log.e(TAG, "❌ Error cargando ejercicios", exception)
                }
            } catch (e: Exception) {
                android.util.Log.e(TAG, "❌ Error en loadExercises", e)
            }
        }
    }

    /**
     * Cargar FAQs
     */
    private fun loadFAQs() {
        viewModelScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    repository.getFAQs()
                }

                result.onSuccess { faqList ->
                    _faqs.value = faqList

                    android.util.Log.d(TAG, "✅ FAQs cargados: ${faqList.size}")
                }.onFailure { exception ->
                    android.util.Log.e(TAG, "❌ Error cargando FAQs", exception)
                }
            } catch (e: Exception) {
                android.util.Log.e(TAG, "❌ Error en loadFAQs", e)
            }
        }
    }

    /**
     * Expandir/colapsar FAQ
     */
    fun toggleFAQ(faqId: String) {
        _faqs.value = _faqs.value.map { faq ->
            if (faq.id == faqId) {
                faq.copy(isExpanded = !faq.isExpanded)
            } else {
                faq
            }
        }
    }

    /**
     * Obtener recurso por ID
     */
    suspend fun getResourceById(id: String): ResourceItem? {
        return try {
            val result = withContext(Dispatchers.IO) {
                repository.getResourceById(id)
            }
            result.getOrNull()
        } catch (e: Exception) {
            android.util.Log.e(TAG, "❌ Error obteniendo recurso $id", e)
            null
        }
    }

    /**
     * Obtener recursos relacionados
     */
    suspend fun getRelatedResources(resourceId: String): List<ResourceItem> {
        return try {
            val result = withContext(Dispatchers.IO) {
                repository.getRelatedResources(resourceId)
            }
            result.getOrNull() ?: emptyList()
        } catch (e: Exception) {
            android.util.Log.e(TAG, "❌ Error obteniendo recursos relacionados", e)
            emptyList()
        }
    }

    /**
     * Limpiar error
     */
    fun clearError() {
        _error.value = null
    }

    override fun onCleared() {
        super.onCleared()
        android.util.Log.d(TAG, "ViewModel cleared")
    }
}