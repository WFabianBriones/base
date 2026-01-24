package com.example.uleammed.scoring

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlin.math.cos
import kotlin.math.sin

// --- Asume que RiskLevel, HealthScore, ScoringState y ScoringViewModel existen en otros archivos ---
// No se incluyen aquí por simplicidad, pero son necesarios para que el código compile.

/**
 * Dashboard principal con todos los gráficos de salud
 */
@Composable
fun HealthDashboard(
    viewModel: ScoringViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    val healthScore by viewModel.healthScore.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        when (state) {
            is ScoringState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator()
                        Text("Analizando tu salud laboral...")
                    }
                }
            }

            is ScoringState.Error -> {
                ErrorView(
                    message = (state as ScoringState.Error).message,
                    onRetry = { viewModel.loadScore() }
                )
            }

            is ScoringState.Success -> {
                if (healthScore != null) {
                    DashboardContent(
                        healthScore = healthScore!!,
                        onRecalculate = { viewModel.recalculateScores() }
                    )
                }
            }

            else -> {
                // Idle - intentar cargar
                LaunchedEffect(Unit) {
                    viewModel.loadScore()
                }
            }
        }
    }
}

/**
 * Contenido principal del dashboard
 */
@Composable
fun DashboardContent(
    healthScore: HealthScore,
    onRecalculate: () -> Unit
) {
    // Verificar qué encuestas están completadas
    val completedSurveys = getCompletedSurveys(healthScore)
    val hasAnySurvey = completedSurveys.isNotEmpty()

    if (!hasAnySurvey) {
        // No hay encuestas completadas
        EmptyDashboardView()
        return
    }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Score global circular (solo si hay al menos 3 encuestas)
        if (completedSurveys.size >= 3) {
            item {
                OverallScoreCard(healthScore = healthScore)
            }
        }

        // 2. Resumen de estado
        item {
            StatusSummaryCard(
                healthScore = healthScore,
                completedCount = completedSurveys.size
            )
        }

        // 3. Gráfico de radar (solo si hay al menos 4 encuestas)
        if (completedSurveys.size >= 4) {
            item {
                RadarChartCard(healthScore = healthScore)
            }
        }

        // 4. Barra de progreso por área (solo áreas completadas)
        item {
            ProgressBarsCard(
                healthScore = healthScore,
                completedSurveys = completedSurveys
            )
        }

        // 5. Áreas críticas (si hay)
        if (healthScore.topConcerns.isNotEmpty()) {
            item {
                TopConcernsCard(concerns = healthScore.topConcerns)
            }
        }

        // 6. Recomendaciones (si hay)
        if (healthScore.recommendations.isNotEmpty()) {
            item {
                RecommendationsCard(recommendations = healthScore.recommendations)
            }
        }

        // 7. Progreso de encuestas
        item {
            SurveyProgressCard(
                completedCount = completedSurveys.size,
                totalCount = 8
            )
        }

        // 8. Botón de recalcular
        item {
            Button(
                onClick = onRecalculate,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Icon(Icons.Filled.Refresh, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Recalcular Análisis")
            }
        }

        // Espaciado final
        item {
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

/**
 * 1. Card de score global circular
 */
@Composable
fun OverallScoreCard(healthScore: HealthScore) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(healthScore.overallRisk.color).copy(alpha = 0.15f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Tu Salud Laboral",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            // Gráfico circular animado
            CircularScoreIndicator(
                score = healthScore.overallScore,
                risk = healthScore.overallRisk,
                size = 180.dp
            )

            // Nivel de riesgo
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = Color(healthScore.overallRisk.color)
            ) {
                Text(
                    text = healthScore.overallRisk.displayName.uppercase(),
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            // Mensaje
            Text(
                text = when (healthScore.overallRisk) {
                    RiskLevel.BAJO -> "¡Excelente! Mantén tus hábitos saludables"
                    RiskLevel.MODERADO -> "Hay áreas que mejorar. Revisa las recomendaciones"
                    RiskLevel.ALTO -> "Varias áreas requieren atención inmediata"
                    RiskLevel.MUY_ALTO -> "⚠️ Situación crítica - Busca apoyo profesional"
                },
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

/**
 * Indicador circular de score con animación
 */
@Composable
fun CircularScoreIndicator(
    score: Int,
    risk: RiskLevel,
    size: androidx.compose.ui.unit.Dp
) {
    val animatedScore by animateFloatAsState(
        targetValue = score.toFloat(),
        animationSpec = tween(durationMillis = 1500, easing = EaseOutCubic),
        label = "score"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(size)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 20.dp.toPx()
            val radius = (size.toPx() - strokeWidth) / 2

            // Círculo de fondo
            drawCircle(
                color = Color.Gray.copy(alpha = 0.2f),
                radius = radius,
                style = Stroke(width = strokeWidth)
            )

            // Arco de progreso
            val sweepAngle = (animatedScore / 100f) * 360f
            drawArc(
                color = Color(risk.color),
                startAngle = -90f,
                sweepAngle = sweepAngle,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                size = Size(radius * 2, radius * 2),
                topLeft = Offset(
                    (size.toPx() - radius * 2) / 2,
                    (size.toPx() - radius * 2) / 2
                )
            )
        }

        // Score en el centro
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "${animatedScore.toInt()}",
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.Bold,
                color = Color(risk.color)
            )
            Text(
                text = "/ 100",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * 2. Resumen de estado con iconos
 */
@Composable
fun StatusSummaryCard(
    healthScore: HealthScore,
    completedCount: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Resumen General",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                SummaryItem(
                    icon = Icons.Filled.CheckCircle,
                    value = countByRisk(healthScore, RiskLevel.BAJO).toString(),
                    label = "Áreas OK",
                    color = Color(RiskLevel.BAJO.color)
                )

                SummaryItem(
                    icon = Icons.Filled.Warning,
                    value = countByRisk(healthScore, RiskLevel.MODERADO).toString(),
                    label = "Atención",
                    color = Color(RiskLevel.MODERADO.color)
                )

                SummaryItem(
                    icon = Icons.Filled.Error,
                    value = (countByRisk(healthScore, RiskLevel.ALTO) +
                            countByRisk(healthScore, RiskLevel.MUY_ALTO)).toString(),
                    label = "Críticas",
                    color = Color(RiskLevel.ALTO.color)
                )
            }
        }
    }
}

@Composable
fun SummaryItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    label: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(32.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * 3. Gráfico de radar (spider chart)
 */
@Composable
fun RadarChartCard(healthScore: HealthScore) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Vista de Radar",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            RadarChart(
                healthScore = healthScore,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
            )
        }
    }
}

@Composable
fun RadarChart(
    healthScore: HealthScore,
    modifier: Modifier = Modifier
) {
    val dataPoints = listOf(
        "Ergonomía" to healthScore.ergonomiaScore,
        "Síntomas\nMusculares" to (100 - healthScore.sintomasMuscularesScore), // Invertir
        "Síntomas\nVisuales" to (100 - healthScore.sintomasVisualesScore),
        "Carga\nTrabajo" to (100 - healthScore.cargaTrabajoScore),
        "Estrés" to (100 - healthScore.estresSaludMentalScore),
        "Sueño" to (100 - healthScore.habitosSuenoScore),
        "Actividad\nFísica" to (100 - healthScore.actividadFisicaScore),
        "Balance" to (100 - healthScore.balanceVidaTrabajoScore)
    )

    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2, size.height / 2)
        val radius = size.minDimension / 2.5f
        val angleStep = 360f / dataPoints.size

        // Dibujar líneas de fondo (telaraña)
        for (i in 1..5) {
            val currentRadius = radius * (i / 5f)
            drawCircle(
                color = Color.Gray.copy(alpha = 0.2f),
                radius = currentRadius,
                center = center,
                style = Stroke(width = 1.dp.toPx())
            )
        }

        // Dibujar líneas radiales
        dataPoints.forEachIndexed { index, _ ->
            val angle = Math.toRadians((angleStep * index - 90).toDouble())
            val end = Offset(
                center.x + (radius * cos(angle)).toFloat(),
                center.y + (radius * sin(angle)).toFloat()
            )
            drawLine(
                color = Color.Gray.copy(alpha = 0.3f),
                start = center,
                end = end,
                strokeWidth = 1.dp.toPx()
            )
        }

        // Dibujar el polígono de datos
        val points = dataPoints.mapIndexed { index, (_, value) ->
            val angle = Math.toRadians((angleStep * index - 90).toDouble())
            val distance = radius * (value / 100f)
            Offset(
                center.x + (distance * cos(angle)).toFloat(),
                center.y + (distance * sin(angle)).toFloat()
            )
        }

        // --- MODIFICACIÓN: Usar el color de riesgo global ---
        val riskColor = Color(healthScore.overallRisk.color)

        // Área rellena
        points.forEachIndexed { index, point ->
            if (index < points.size - 1) {
                drawLine(
                    color = riskColor.copy(alpha = 0.3f),
                    start = point,
                    end = points[index + 1],
                    strokeWidth = 3.dp.toPx()
                )
            }
        }
        drawLine(
            color = riskColor.copy(alpha = 0.3f),
            start = points.last(),
            end = points.first(),
            strokeWidth = 3.dp.toPx()
        )

        // Puntos
        points.forEach { point ->
            drawCircle(
                color = riskColor,
                radius = 6.dp.toPx(),
                center = point
            )
        }
    }

    // Leyenda
    Column(
        modifier = Modifier.padding(top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        dataPoints.chunked(4).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                row.forEach { (label, _) ->
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

/**
 * 4. Barras de progreso por área (solo completadas)
 */
@Composable
fun ProgressBarsCard(
    healthScore: HealthScore,
    completedSurveys: List<String>
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Análisis por Área",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            // Solo mostrar áreas completadas (Se añaden iconos de tendencia de EJEMPLO)
            if (completedSurveys.contains("ergonomia")) {
                ProgressBarItem("Ergonomía", healthScore.ergonomiaScore, healthScore.ergonomiaRisk, true, Icons.Filled.ArrowUpward)
            }
            if (completedSurveys.contains("sintomas_musculares")) {
                ProgressBarItem("Síntomas Musculares", healthScore.sintomasMuscularesScore, healthScore.sintomasMuscularesRisk, false, Icons.Filled.ArrowDownward)
            }
            if (completedSurveys.contains("sintomas_visuales")) {
                ProgressBarItem("Síntomas Visuales", healthScore.sintomasVisualesScore, healthScore.sintomasVisualesRisk, false, Icons.Filled.HorizontalRule)
            }
            if (completedSurveys.contains("carga_trabajo")) {
                ProgressBarItem("Carga de Trabajo", healthScore.cargaTrabajoScore, healthScore.cargaTrabajoRisk, false, Icons.Filled.ArrowUpward)
            }
            if (completedSurveys.contains("estres")) {
                ProgressBarItem("Estrés y Salud Mental", healthScore.estresSaludMentalScore, healthScore.estresSaludMentalRisk, false, Icons.Filled.ArrowDownward)
            }
            if (completedSurveys.contains("sueno")) {
                ProgressBarItem("Calidad del Sueño", healthScore.habitosSuenoScore, healthScore.habitosSuenoRisk, false, Icons.Filled.ArrowUpward)
            }
            if (completedSurveys.contains("actividad_fisica")) {
                ProgressBarItem("Actividad Física", healthScore.actividadFisicaScore, healthScore.actividadFisicaRisk, false, Icons.Filled.HorizontalRule)
            }
            if (completedSurveys.contains("balance")) {
                ProgressBarItem("Balance Vida-Trabajo", healthScore.balanceVidaTrabajoScore, healthScore.balanceVidaTrabajoRisk, false, Icons.Filled.ArrowUpward)
            }
        }
    }
}

// --- FUNCIÓN MODIFICADA: Ahora incluye trendIcon ---
@Composable
fun ProgressBarItem(
    label: String,
    score: Int,
    risk: RiskLevel,
    higherIsBetter: Boolean,
    trendIcon: androidx.compose.ui.graphics.vector.ImageVector? = null // NUEVO PARAMETRO
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )

            // ✅ LÓGICA DE ICONO DE TENDENCIA
            trendIcon?.let { icon ->
                val trendColor = when (icon) {
                    Icons.Filled.ArrowUpward -> Color(RiskLevel.BAJO.color)
                    Icons.Filled.ArrowDownward -> Color(RiskLevel.ALTO.color)
                    else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                }
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = trendColor,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
            }

            Surface(
                shape = MaterialTheme.shapes.small,
                color = Color(risk.color).copy(alpha = 0.2f)
            ) {
                Text(
                    text = "$score",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(risk.color)
                )
            }
        }

        LinearProgressIndicator(
            progress = { score / 100f },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = Color(risk.color),
            trackColor = Color(risk.color).copy(alpha = 0.2f)
        )
    }
}

/**
 * 5. Áreas críticas
 */
@Composable
fun TopConcernsCard(concerns: List<String>) {
    if (concerns.isEmpty()) return

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.PriorityHigh,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
                Text(
                    text = "Áreas Prioritarias",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            concerns.forEachIndexed { index, concern ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "${index + 1}",
                                color = Color.White,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Text(
                        text = concern,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

/**
 * 6. Recomendaciones
 */
@Composable
fun RecommendationsCard(recommendations: List<String>) {
    if (recommendations.isEmpty()) return

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
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
                    text = "Recomendaciones",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            recommendations.forEach { recommendation ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.ArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = recommendation,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

/**
 * Vista de error
 */
@Composable
fun ErrorView(
    message: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.ErrorOutline,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Error al cargar análisis",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onRetry) {
            Icon(Icons.Filled.Refresh, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Reintentar")
        }
    }
}

// Función helper
private fun countByRisk(healthScore: HealthScore, targetRisk: RiskLevel): Int {
    val risks = listOf(
        healthScore.ergonomiaRisk,
        healthScore.sintomasMuscularesRisk,
        healthScore.sintomasVisualesRisk,
        healthScore.cargaTrabajoRisk,
        healthScore.estresSaludMentalRisk,
        healthScore.habitosSuenoRisk,
        healthScore.actividadFisicaRisk,
        healthScore.balanceVidaTrabajoRisk
    )
    return risks.count { it == targetRisk }
}

/**
 * Helper: Detectar qué encuestas están completadas
 */
private fun getCompletedSurveys(healthScore: HealthScore): List<String> {
    val completed = mutableListOf<String>()

    if (healthScore.ergonomiaScore > 0) completed.add("ergonomia")
    if (healthScore.sintomasMuscularesScore > 0) completed.add("sintomas_musculares")
    if (healthScore.sintomasVisualesScore > 0) completed.add("sintomas_visuales")
    if (healthScore.cargaTrabajoScore > 0) completed.add("carga_trabajo")
    if (healthScore.estresSaludMentalScore > 0) completed.add("estres")
    if (healthScore.habitosSuenoScore > 0) completed.add("sueno")
    if (healthScore.actividadFisicaScore > 0) completed.add("actividad_fisica")
    if (healthScore.balanceVidaTrabajoScore > 0) completed.add("balance")

    return completed
}

/**
 * Vista cuando no hay encuestas completadas
 */
@Composable
fun EmptyDashboardView() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Assessment,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Text(
                text = "Completa tus Encuestas",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Text(
                text = "Responde al menos una encuesta para ver tu análisis de salud laboral con gráficos personalizados.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Icon(
                imageVector = Icons.Filled.ArrowForward,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Text(
                text = "Ve a la pestaña 'Explorar' para comenzar",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/**
 * Card de progreso de encuestas
 */
@Composable
fun SurveyProgressCard(completedCount: Int, totalCount: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Encuestas Completadas",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.primary
                ) {
                    Text(
                        text = "$completedCount / $totalCount",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            LinearProgressIndicator(
                progress = { completedCount.toFloat() / totalCount },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .clip(RoundedCornerShape(6.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            if (completedCount < totalCount) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Info,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Completa más encuestas para un análisis más completo",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = Color(RiskLevel.BAJO.color)
                    )
                    Text(
                        text = "¡Excelente! Has completado todas las encuestas",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(RiskLevel.BAJO.color),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}