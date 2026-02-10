package com.example.uleammed.scoring.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.uleammed.scoring.*

/**
 * ✅ COMPONENTES UI PARA MEJORAS DE PRIORIDAD MEDIA
 *
 * Incluye:
 * - Visualización de tendencias
 * - Indicador de completitud
 * - Gráficos de progreso temporal
 * - Insights automáticos
 */

// ==================== CARD DE TENDENCIAS GENERALES ====================

@Composable
fun TrendOverviewCard(trendAnalysis: TrendAnalysis) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (trendAnalysis.overallTrend) {
                TrendDirection.MEJORANDO -> Color(0xFF4CAF50).copy(alpha = 0.1f)
                TrendDirection.EMPEORANDO -> Color(0xFFF44336).copy(alpha = 0.1f)
                TrendDirection.ESTABLE -> MaterialTheme.colorScheme.surfaceVariant
                TrendDirection.SIN_DATOS -> MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.TrendingUp, contentDescription = null)
                    Text(
                        text = "Tendencia General",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                TrendBadge(trendAnalysis.overallTrend)
            }

            // Estadísticas
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                TrendStat(
                    count = trendAnalysis.areasImproving,
                    label = "Mejorando",
                    color = Color(0xFF4CAF50)
                )

                TrendStat(
                    count = trendAnalysis.areasStable,
                    label = "Estables",
                    color = Color(0xFF9E9E9E)
                )

                TrendStat(
                    count = trendAnalysis.areasWorsening,
                    label = "Empeorando",
                    color = Color(0xFFF44336)
                )
            }

            // Insights
            if (trendAnalysis.keyInsights.isNotEmpty()) {
                Divider()

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Insights Clave",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )

                    trendAnalysis.keyInsights.forEach { insight ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Text(
                                text = "•",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = insight,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TrendBadge(trend: TrendDirection) {
    val (color, icon) = when (trend) {
        TrendDirection.MEJORANDO -> Color(0xFF4CAF50) to Icons.Filled.TrendingUp
        TrendDirection.EMPEORANDO -> Color(0xFFF44336) to Icons.Filled.TrendingDown
        TrendDirection.ESTABLE -> Color(0xFF9E9E9E) to Icons.Filled.Remove
        TrendDirection.SIN_DATOS -> Color(0xFF9E9E9E) to Icons.Filled.Help
    }

    Surface(
        color = color.copy(alpha = 0.15f),
        shape = MaterialTheme.shapes.small
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = color
            )
            Text(
                text = trend.displayName,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

@Composable
private fun TrendStat(count: Int, label: String, color: Color) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ==================== LISTA DETALLADA DE TENDENCIAS POR ÁREA ====================

@Composable
fun AreaTrendsListCard(trends: Map<String, AreaTrend>) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Filled.Assessment, contentDescription = null)
                Text(
                    text = "Tendencias por Área",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            // Ordenar por cambio (mayor cambio primero)
            val sortedTrends = trends.values.sortedByDescending { kotlin.math.abs(it.changePoints) }

            sortedTrends.forEach { trend ->
                AreaTrendItem(trend)
            }
        }
    }
}

@Composable
private fun AreaTrendItem(trend: AreaTrend) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.small
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = trend.area,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${trend.previousScore ?: "N/A"} → ${trend.currentScore}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = trend.direction.icon,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = when {
                            trend.changePoints > 0 -> "+${trend.changePoints}"
                            else -> "${trend.changePoints}"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = when (trend.direction) {
                            TrendDirection.MEJORANDO -> Color(0xFF4CAF50)
                            TrendDirection.EMPEORANDO -> Color(0xFFF44336)
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }

                Text(
                    text = "${trend.daysElapsed} días",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ==================== CARD DE COMPLETITUD ====================

@Composable
fun CompletenessCard(completeness: CompletenessInfo) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (completeness.confidenceLevel) {
                ConfidenceLevel.ALTA -> Color(0xFF4CAF50).copy(alpha = 0.1f)
                ConfidenceLevel.MEDIA -> Color(0xFFFFC107).copy(alpha = 0.1f)
                ConfidenceLevel.BAJA -> Color(0xFFFF9800).copy(alpha = 0.1f)
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        when (completeness.confidenceLevel) {
                            ConfidenceLevel.ALTA -> Icons.Filled.CheckCircle
                            ConfidenceLevel.MEDIA -> Icons.Filled.Info
                            ConfidenceLevel.BAJA -> Icons.Filled.Warning
                        },
                        contentDescription = null,
                        tint = when (completeness.confidenceLevel) {
                            ConfidenceLevel.ALTA -> Color(0xFF4CAF50)
                            ConfidenceLevel.MEDIA -> Color(0xFFFFC107)
                            ConfidenceLevel.BAJA -> Color(0xFFFF9800)
                        }
                    )
                    Text(
                        text = "Completitud de Datos",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                ConfidenceBadge(completeness.confidenceLevel)
            }

            // Barra de progreso
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${completeness.completedQuestionnaires}/${completeness.totalQuestionnaires} cuestionarios",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "${completeness.completenessPercent.toInt()}%",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                LinearProgressIndicator(
                    progress = completeness.completenessPercent / 100f,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp),
                    color = when (completeness.confidenceLevel) {
                        ConfidenceLevel.ALTA -> Color(0xFF4CAF50)
                        ConfidenceLevel.MEDIA -> Color(0xFFFFC107)
                        ConfidenceLevel.BAJA -> Color(0xFFFF9800)
                    }
                )
            }

            // Descripción
            Text(
                text = completeness.confidenceLevel.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Cuestionarios faltantes
            if (completeness.missingQuestionnaires.isNotEmpty()) {
                Divider()

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Cuestionarios pendientes:",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )

                    completeness.missingQuestionnaires.forEach { questionnaire ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Filled.Circle,
                                contentDescription = null,
                                modifier = Modifier.size(8.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = questionnaire,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ConfidenceBadge(level: ConfidenceLevel) {
    val color = when (level) {
        ConfidenceLevel.ALTA -> Color(0xFF4CAF50)
        ConfidenceLevel.MEDIA -> Color(0xFFFFC107)
        ConfidenceLevel.BAJA -> Color(0xFFFF9800)
    }

    Surface(
        color = color.copy(alpha = 0.15f),
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = "Confianza: ${level.displayName}",
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

// ==================== GRÁFICO DE TENDENCIA TEMPORAL ====================

@Composable
fun TrendGraphCard(
    areaName: String,
    trend: AreaTrend
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Filled.ShowChart, contentDescription = null)
                Text(
                    text = "Evolución: $areaName",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            // Gráfico simple de dos puntos
            SimpleTrendGraph(
                previousScore = trend.previousScore ?: 0,
                currentScore = trend.currentScore,
                maxScore = 100
            )

            // Estadísticas
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(
                    label = "Anterior",
                    value = "${trend.previousScore ?: "N/A"}",
                    color = Color.Gray
                )

                StatItem(
                    label = "Actual",
                    value = "${trend.currentScore}",
                    color = MaterialTheme.colorScheme.primary
                )

                StatItem(
                    label = "Cambio",
                    value = when {
                        trend.changePoints > 0 -> "+${trend.changePoints}"
                        else -> "${trend.changePoints}"
                    },
                    color = when (trend.direction) {
                        TrendDirection.MEJORANDO -> Color(0xFF4CAF50)
                        TrendDirection.EMPEORANDO -> Color(0xFFF44336)
                        else -> Color.Gray
                    }
                )
            }
        }
    }
}

@Composable
private fun SimpleTrendGraph(
    previousScore: Int,
    currentScore: Int,
    maxScore: Int
) {
    // Gráfico simple con dos barras
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Bottom
    ) {
        // Barra anterior
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = previousScore.toString(),
                style = MaterialTheme.typography.labelSmall
            )
            Box(
                modifier = Modifier
                    .width(60.dp)
                    .height((previousScore.toFloat() / maxScore * 100).dp)
                    .background(
                        color = Color.Gray.copy(alpha = 0.3f),
                        shape = MaterialTheme.shapes.small
                    )
            )
        }

        // Barra actual
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = currentScore.toString(),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold
            )
            Box(
                modifier = Modifier
                    .width(60.dp)
                    .height((currentScore.toFloat() / maxScore * 100).dp)
                    .background(
                        color = MaterialTheme.colorScheme.primary,
                        shape = MaterialTheme.shapes.small
                    )
            )
        }
    }
}

@Composable
private fun StatItem(label: String, value: String, color: Color) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

// ==================== PANTALLA COMPLETA DE TENDENCIAS ====================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrendsAnalysisScreen(
    enhancedScore: EnhancedHealthScore,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Análisis de Tendencias") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, "Volver")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(8.dp)) }

            // Completitud
            item {
                CompletenessCard(enhancedScore.completeness)
            }

            // Tendencias generales
            enhancedScore.trendAnalysis?.let { trendAnalysis ->
                item {
                    TrendOverviewCard(trendAnalysis)
                }

                // Tendencias detalladas
                item {
                    AreaTrendsListCard(trendAnalysis.areaTrends)
                }
            }

            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }
}