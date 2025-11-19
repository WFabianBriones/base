package com.example.uleammed.resources

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.*

/**
 * Card de recurso individual
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResourceCard(
    resource: ResourceItem,
    onResourceClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        onClick = onResourceClick,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header con tipo y categoría
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Icono de tipo
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = resource.type.icon,
                            contentDescription = null,
                            modifier = Modifier.padding(6.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }

                    // Badge de categoría
                    CategoryBadge(category = resource.category)
                }

                // Botón de favorito
                IconButton(
                    onClick = { onFavoriteClick() },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = if (resource.isFavorite) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                        contentDescription = if (resource.isFavorite) "Quitar de favoritos" else "Añadir a favoritos",
                        tint = if (resource.isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Título
            Text(
                text = resource.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Resumen
            Text(
                text = resource.summary,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Metadatos
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Tiempo de lectura
                MetadataChip(
                    icon = Icons.Filled.AccessTime,
                    text = resource.readTime
                )

                // Dificultad
                MetadataChip(
                    icon = resource.difficulty.icon,
                    text = resource.difficulty.displayName
                )

                // Peer reviewed
                if (resource.isPeerReviewed) {
                    MetadataChip(
                        icon = Icons.Filled.Verified,
                        text = "Verificado",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                // Video
                if (resource.videoUrl != null) {
                    Icon(
                        imageVector = Icons.Filled.PlayCircle,
                        contentDescription = "Incluye video",
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.tertiary
                    )
                }

                // PDF
                if (resource.pdfUrl != null) {
                    Icon(
                        imageVector = Icons.Filled.PictureAsPdf,
                        contentDescription = "Descargable",
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

/**
 * Badge de categoría
 */
@Composable
fun CategoryBadge(
    category: ResourceCategory,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        color = Color(category.color).copy(alpha = 0.15f),
        border = BorderStroke(1.dp, Color(category.color).copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = category.icon,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = Color(category.color)
            )
            Text(
                text = category.displayName,
                style = MaterialTheme.typography.labelSmall,
                color = Color(category.color),
                fontWeight = FontWeight.Medium
            )
        }
    }
}

/**
 * Chip de metadatos
 */
@Composable
fun MetadataChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = tint
        )
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = tint
        )
    }
}

/**
 * Card de ejercicio
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseCard(
    exercise: ExerciseResource,
    onStartExercise: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.FitnessCenter,
                            contentDescription = null,
                            modifier = Modifier.padding(8.dp),
                            tint = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }

                    Column {
                        Text(
                            text = exercise.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        CategoryBadge(category = exercise.category)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = exercise.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Información del ejercicio
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ExerciseInfo(
                    icon = Icons.Filled.Timer,
                    label = "Duración",
                    value = "${exercise.duration}s"
                )
                ExerciseInfo(
                    icon = Icons.Filled.Repeat,
                    label = "Repeticiones",
                    value = "${exercise.repetitions}x"
                )
                ExerciseInfo(
                    icon = Icons.Filled.TrendingUp,
                    label = "Nivel",
                    value = exercise.difficulty.displayName
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onStartExercise,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Comenzar Ejercicio")
            }
        }
    }
}

/**
 * Información de ejercicio
 */
@Composable
fun ExerciseInfo(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * Card de FAQ expandible
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FAQCard(
    faq: FAQItem,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        onClick = onToggle,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Help,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = faq.question,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Icon(
                    imageVector = if (faq.isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = if (faq.isExpanded) "Contraer" else "Expandir"
                )
            }

            AnimatedVisibility(
                visible = faq.isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    HorizontalDivider(modifier = Modifier.padding(bottom = 12.dp))

                    Text(
                        text = faq.answer,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (faq.sources.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Fuentes:",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        faq.sources.forEach { source ->
                            Text(
                                text = "• $source",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    CategoryBadge(category = faq.category)
                }
            }
        }
    }
}

/**
 * Sección de filtros
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterSection(
    currentFilter: ResourceFilter,
    onFilterChange: (ResourceFilter) -> Unit,
    onClearFilters: () -> Unit
) {
    var showFilterDialog by remember { mutableStateOf(false) }

    if (showFilterDialog) {
        FilterDialog(
            currentFilter = currentFilter,
            onDismiss = { showFilterDialog = false },
            onApply = { newFilter ->
                onFilterChange(newFilter)
                showFilterDialog = false
            }
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Botón de filtros
        FilterChip(
            selected = currentFilter.isActive(),
            onClick = { showFilterDialog = true },
            label = {
                Text(
                    if (currentFilter.isActive()) "Filtros activos" else "Filtros"
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Filled.FilterList,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            }
        )

        // Chip de favoritos
        FilterChip(
            selected = currentFilter.favoritesOnly,
            onClick = {
                onFilterChange(currentFilter.copy(favoritesOnly = !currentFilter.favoritesOnly))
            },
            label = { Text("Favoritos") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Filled.Bookmark,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            }
        )

        // Chip de videos
        FilterChip(
            selected = currentFilter.hasVideo,
            onClick = {
                onFilterChange(currentFilter.copy(hasVideo = !currentFilter.hasVideo))
            },
            label = { Text("Videos") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Filled.PlayCircle,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            }
        )

        // Botón limpiar
        if (currentFilter.isActive()) {
            IconButton(
                onClick = onClearFilters,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Limpiar filtros"
                )
            }
        }
    }
}

/**
 * Dialog de filtros completo
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterDialog(
    currentFilter: ResourceFilter,
    onDismiss: () -> Unit,
    onApply: (ResourceFilter) -> Unit
) {
    var tempFilter by remember { mutableStateOf(currentFilter) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.FilterList, contentDescription = null)
                Text("Filtros de Búsqueda")
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Tipos de recurso
                Text(
                    text = "Tipo de Recurso",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    ResourceType.values().forEach { type ->
                        FilterChip(
                            selected = tempFilter.types.contains(type),
                            onClick = {
                                val newTypes = tempFilter.types.toMutableSet()
                                if (newTypes.contains(type)) {
                                    newTypes.remove(type)
                                } else {
                                    newTypes.add(type)
                                }
                                tempFilter = tempFilter.copy(types = newTypes)
                            },
                            label = { Text(type.displayName) }
                        )
                    }
                }

                HorizontalDivider()

                // Dificultad
                Text(
                    text = "Nivel de Dificultad",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    ResourceDifficulty.values().forEach { difficulty ->
                        FilterChip(
                            selected = tempFilter.difficulties.contains(difficulty),
                            onClick = {
                                val newDifficulties = tempFilter.difficulties.toMutableSet()
                                if (newDifficulties.contains(difficulty)) {
                                    newDifficulties.remove(difficulty)
                                } else {
                                    newDifficulties.add(difficulty)
                                }
                                tempFilter = tempFilter.copy(difficulties = newDifficulties)
                            },
                            label = { Text(difficulty.displayName) }
                        )
                    }
                }

                HorizontalDivider()

                // Opciones adicionales
                Text(
                    text = "Opciones",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Solo verificados científicamente")
                    Switch(
                        checked = tempFilter.peerReviewedOnly,
                        onCheckedChange = {
                            tempFilter = tempFilter.copy(peerReviewedOnly = it)
                        }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Con archivo descargable")
                    Switch(
                        checked = tempFilter.hasDownload,
                        onCheckedChange = {
                            tempFilter = tempFilter.copy(hasDownload = it)
                        }
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = { onApply(tempFilter) }) {
                Text("Aplicar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

/**
 * Estado vacío
 */
@Composable
fun EmptyResourcesState(
    message: String = "No se encontraron recursos",
    icon: androidx.compose.ui.graphics.vector.ImageVector = Icons.Filled.SearchOff
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = message,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * FlowRow simple para chips
 */
@Composable
fun FlowRow(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    content: @Composable () -> Unit
) {
    Column(modifier = modifier) {
        content()
    }
}