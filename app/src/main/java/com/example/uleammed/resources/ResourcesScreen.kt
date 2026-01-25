package com.example.uleammed.resources

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

/**
 * Pantalla principal de Recursos con HEADER SIMPLIFICADO
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResourcesContentNew(
    onResourceClick: (String) -> Unit = {},
    onExerciseClick: (String) -> Unit = {},
    viewModel: ResourceViewModel = viewModel()
) {
    val filteredResources by viewModel.filteredResources.collectAsState()
    val currentFilter by viewModel.currentFilter.collectAsState()
    val state by viewModel.state.collectAsState()
    val favorites by viewModel.favorites.collectAsState()
    val exercises by viewModel.exercises.collectAsState()
    val faqs by viewModel.faqs.collectAsState()

    var selectedTab by remember { mutableStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }

    val tabs = listOf(
        "Artículos" to Icons.Filled.Article,
        "Ejercicios" to Icons.Filled.FitnessCenter,
        "FAQs" to Icons.Filled.QuestionAnswer,
        "Favoritos" to Icons.Filled.Bookmark
    )

    Column(modifier = Modifier.fillMaxSize()) {

        // ========== HEADER VISUAL SIMPLIFICADO ==========
        ResourcesHeaderSimple()

        // Barra de búsqueda
        SearchBar(
            query = searchQuery,
            onQueryChange = {
                searchQuery = it
                viewModel.search(it)
            },
            onSearch = { viewModel.search(it) },
            active = false,
            onActiveChange = { },
            placeholder = { Text("Buscar recursos de salud...") },
            leadingIcon = {
                Icon(Icons.Filled.Search, contentDescription = null)
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = {
                        searchQuery = ""
                        viewModel.search("")
                    }) {
                        Icon(Icons.Filled.Close, contentDescription = "Limpiar")
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 8.dp)
        ) { }

        // Tabs
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            tabs.forEachIndexed { index, (title, icon) ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title) },
                    icon = { Icon(icon, contentDescription = null) }
                )
            }
        }

        // Filtros (solo en tab de artículos)
        if (selectedTab == 0) {
            FilterSection(
                currentFilter = currentFilter,
                onFilterChange = { viewModel.applyFilter(it) },
                onClearFilters = { viewModel.clearFilters() }
            )

            Spacer(modifier = Modifier.height(8.dp))
        }

        // Contenido según tab
        when (selectedTab) {
            0 -> ArticlesTab(
                resources = filteredResources,
                state = state,
                onResourceClick = onResourceClick,
                onFavoriteClick = { resourceId ->
                    viewModel.toggleFavorite(resourceId)
                }
            )
            1 -> ExercisesTab(
                exercises = exercises,
                onStartExercise = onExerciseClick
            )
            2 -> FAQsTab(
                faqs = faqs,
                onToggleFAQ = { faqId ->
                    viewModel.toggleFAQ(faqId)
                }
            )
            3 -> FavoritesTab(
                favorites = favorites,
                onResourceClick = onResourceClick,
                onFavoriteClick = { resourceId ->
                    viewModel.toggleFavorite(resourceId)
                }
            )
        }
    }
}

// ========== HEADER SIMPLIFICADO (SOLO TÍTULO E ICONO) ==========
@Composable
private fun ResourcesHeaderSimple() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Icono con fondo verde
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.primary
        ) {
            Icon(
                imageVector = Icons.Filled.MenuBook,
                contentDescription = null,
                modifier = Modifier
                    .size(48.dp)
                    .padding(10.dp),
                tint = MaterialTheme.colorScheme.onPrimary
            )
        }

        // Título y subtítulo en verde
        Column {
            Text(
                text = "Biblioteca de Recursos",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Aprende y cuida tu salud laboral",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
            )
        }
    }
}

/**
 * Tab de Artículos
 */
@Composable
fun ArticlesTab(
    resources: List<ResourceItem>,
    state: ResourceState,
    onResourceClick: (String) -> Unit,
    onFavoriteClick: (String) -> Unit
) {
    when (state) {
        is ResourceState.Loading -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }

        is ResourceState.Success -> {
            if (resources.isEmpty()) {
                EmptyResourcesState(
                    message = "No se encontraron recursos con estos filtros",
                    icon = Icons.Filled.FilterListOff
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Contador de resultados
                    item {
                        Text(
                            text = "${resources.size} recurso${if (resources.size != 1) "s" else ""} encontrado${if (resources.size != 1) "s" else ""}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }

                    items(resources, key = { it.id }) { resource ->
                        ResourceCard(
                            resource = resource,
                            onResourceClick = { onResourceClick(resource.id) },
                            onFavoriteClick = { onFavoriteClick(resource.id) }
                        )
                    }
                }
            }
        }

        is ResourceState.Error -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.ErrorOutline,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Error al cargar recursos",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.error
                )
                Text(
                    text = state.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        else -> { }
    }
}

/**
 * Tab de Ejercicios
 */
@Composable
fun ExercisesTab(
    exercises: List<ExerciseResource>,
    onStartExercise: (String) -> Unit
) {
    if (exercises.isEmpty()) {
        EmptyResourcesState(
            message = "No hay ejercicios disponibles",
            icon = Icons.Filled.FitnessCenter
        )
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Ejercicios simples para realizar en tu puesto de trabajo. Toma pausas activas cada 30-60 minutos.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            items(exercises, key = { it.id }) { exercise ->
                ExerciseCard(
                    exercise = exercise,
                    onStartExercise = { onStartExercise(exercise.id) }
                )
            }
        }
    }
}

/**
 * Tab de FAQs
 */
@Composable
fun FAQsTab(
    faqs: List<FAQItem>,
    onToggleFAQ: (String) -> Unit
) {
    if (faqs.isEmpty()) {
        EmptyResourcesState(
            message = "No hay preguntas frecuentes",
            icon = Icons.Filled.QuestionAnswer
        )
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    text = "Preguntas Frecuentes",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    text = "${faqs.size} pregunta${if (faqs.size != 1) "s" else ""} respondida${if (faqs.size != 1) "s" else ""}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            items(faqs, key = { it.id }) { faq ->
                FAQCard(
                    faq = faq,
                    onToggle = { onToggleFAQ(faq.id) }
                )
            }
        }
    }
}

/**
 * Tab de Favoritos
 */
@Composable
fun FavoritesTab(
    favorites: List<ResourceItem>,
    onResourceClick: (String) -> Unit,
    onFavoriteClick: (String) -> Unit
) {
    if (favorites.isEmpty()) {
        EmptyResourcesState(
            message = "Aún no tienes recursos favoritos",
            icon = Icons.Filled.BookmarkBorder
        )
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Tus recursos guardados para consulta rápida.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            items(favorites, key = { it.id }) { resource ->
                ResourceCard(
                    resource = resource,
                    onResourceClick = { onResourceClick(resource.id) },
                    onFavoriteClick = { onFavoriteClick(resource.id) }
                )
            }
        }
    }
}

// NOTA: EmptyResourcesState, FilterSection, ResourceCard, ExerciseCard, FAQCard
// ya están definidas en ResourceComponents.kt - no las duplicamos aquí