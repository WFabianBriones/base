package com.example.uleammed.resources

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * Pantalla de detalle de un recurso individual
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResourceDetailScreen(
    resourceId: String,
    onBack: () -> Unit,
    viewModel: ResourceViewModel = viewModel()
) {
    var resource by remember { mutableStateOf<ResourceItem?>(null) }
    var relatedResources by remember { mutableStateOf<List<ResourceItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    val scope = rememberCoroutineScope()

    LaunchedEffect(resourceId) {
        scope.launch {
            isLoading = true
            resource = viewModel.getResourceById(resourceId)

            // Registrar lectura
            if (resource != null) {
                viewModel.trackReading(resourceId)
            }

            // Cargar recursos relacionados
            relatedResources = viewModel.getRelatedResources(resourceId)

            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (resource != null) {
                        Text(
                            text = resource!!.type.displayName,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    if (resource != null) {
                        // Botón de favorito
                        IconButton(
                            onClick = {
                                viewModel.toggleFavorite(resourceId)
                                resource = resource?.copy(isFavorite = !resource!!.isFavorite)
                            }
                        ) {
                            Icon(
                                imageVector = if (resource!!.isFavorite) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                                contentDescription = if (resource!!.isFavorite) "Quitar de favoritos" else "Añadir a favoritos",
                                tint = if (resource!!.isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // Botón de compartir
                        IconButton(onClick = { /* TODO: Compartir */ }) {
                            Icon(Icons.Filled.Share, contentDescription = "Compartir")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { paddingValues ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (resource == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.ErrorOutline,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                    Text(
                        text = "Recurso no encontrado",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Button(onClick = onBack) {
                        Text("Volver")
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header del artículo
                ResourceHeader(resource = resource!!)

                // Contenido principal
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Puntos clave (si existen)
                    if (resource!!.keyPoints.isNotEmpty()) {
                        KeyPointsSection(keyPoints = resource!!.keyPoints)
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    }

                    // Contenido completo
                    Text(
                        text = resource!!.content,
                        style = MaterialTheme.typography.bodyLarge,
                        lineHeight = MaterialTheme.typography.bodyLarge.fontSize * 1.6
                    )

                    // Referencias (si existen)
                    if (resource!!.references.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        ReferencesSection(references = resource!!.references)
                    }

                    // Recursos relacionados
                    if (relatedResources.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(24.dp))
                        RelatedResourcesSection(
                            resources = relatedResources,
                            onResourceClick = { /* TODO: Navegar a otro recurso */ }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Header del recurso con metadatos
 */
@Composable
fun ResourceHeader(resource: ResourceItem) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Categoría badge
        CategoryBadge(category = resource.category)

        // Título
        Text(
            text = resource.title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            lineHeight = MaterialTheme.typography.headlineMedium.fontSize * 1.3
        )

        // Resumen
        Text(
            text = resource.summary,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Metadatos
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    MetadataChip(
                        icon = Icons.Filled.AccessTime,
                        text = resource.readTime
                    )
                    MetadataChip(
                        icon = resource.difficulty.icon,
                        text = resource.difficulty.displayName
                    )
                }

                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    if (resource.isPeerReviewed) {
                        MetadataChip(
                            icon = Icons.Filled.Verified,
                            text = "Verificado",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    MetadataChip(
                        icon = Icons.Filled.Source,
                        text = resource.source
                    )
                }
            }
        }

        // Tags
        if (resource.tags.isNotEmpty()) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                resource.tags.take(5).forEach { tag ->
                    SuggestionChip(
                        onClick = { /* TODO: Buscar por tag */ },
                        label = { Text("#$tag") },
                        icon = {
                            Icon(
                                imageVector = Icons.Filled.Tag,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    )
                }
            }
        }
    }
}

/**
 * Sección de puntos clave
 */
@Composable
fun KeyPointsSection(keyPoints: List<String>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Lightbulb,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Puntos Clave",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            keyPoints.forEach { point ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = point,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

/**
 * Sección de referencias científicas
 */
@Composable
fun ReferencesSection(references: List<String>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.LibraryBooks,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary
                )
                Text(
                    text = "Referencias",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            references.forEach { reference ->
                Text(
                    text = "• $reference",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
    }
}

/**
 * Sección de recursos relacionados
 */
@Composable
fun RelatedResourcesSection(
    resources: List<ResourceItem>,
    onResourceClick: (String) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "También te puede interesar",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        resources.forEach { resource ->
            RelatedResourceCard(
                resource = resource,
                onClick = { onResourceClick(resource.id) }
            )
        }
    }
}

/**
 * Card compacto de recurso relacionado
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RelatedResourceCard(
    resource: ResourceItem,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = MaterialTheme.shapes.small,
                color = Color(resource.category.color).copy(alpha = 0.2f)
            ) {
                Icon(
                    imageVector = resource.type.icon,
                    contentDescription = null,
                    modifier = Modifier
                        .size(40.dp)
                        .padding(8.dp),
                    tint = Color(resource.category.color)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = resource.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2
                )
                MetadataChip(
                    icon = Icons.Filled.AccessTime,
                    text = resource.readTime
                )
            }

            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}