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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import kotlin.math.cos
import kotlin.math.sin
import androidx.compose.material.icons.filled.Psychology

/**
 * Dashboard principal con todos los gráficos de salud
 */
@Composable
fun HealthDashboard(
    viewModel: ScoringViewModel = viewModel(),
    onNavigateToBurnoutAnalysis: (Map<String, Float>) -> Unit = {}
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
                        onRecalculate = { viewModel.recalculateScores() },
                        onNavigateToBurnoutAnalysis = onNavigateToBurnoutAnalysis
                    )
                }
            }

            else -> {
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
    onRecalculate: () -> Unit,
    onNavigateToBurnoutAnalysis: (Map<String, Float>) -> Unit
) {
    val completedSurveys = getCompletedSurveys(healthScore)
    val hasAnySurvey = completedSurveys.isNotEmpty()

    if (!hasAnySurvey) {
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

        if (completedSurveys.size >= 3) {
            item {
                BurnoutAIAnalysisCard(
                    healthScore = healthScore,
                    onAnalyze = onNavigateToBurnoutAnalysis
                )
            }
        }

        // 2. Resumen de estado (solo si hay encuestas completadas)
        if (completedSurveys.isNotEmpty()) {
            item {
                StatusSummaryCard(
                    healthScore = healthScore,
                    completedCount = completedSurveys.size
                )
            }
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
                text = "Nivel de Riesgo Laboral",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            CircularScoreIndicator(
                score = healthScore.overallScore,
                risk = healthScore.overallRisk,
                size = 180.dp
            )

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

            Text(
                text = when (healthScore.overallRisk) {
                    RiskLevel.BAJO -> "Buen estado general. Mantén tus hábitos saludables"
                    RiskLevel.MODERADO -> "Hay áreas que mejorar. Revisa las recomendaciones"
                    RiskLevel.ALTO -> "Varias áreas requieren atención. Implementa cambios pronto"
                    RiskLevel.MUY_ALTO -> "Situación que requiere intervención. Busca apoyo profesional"
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

            drawCircle(
                color = Color.Gray.copy(alpha = 0.2f),
                radius = radius,
                style = Stroke(width = strokeWidth)
            )

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
 * 2. Resumen de estado con iconos - CORREGIDO
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

@Composable
fun RadarChartCard(healthScore: HealthScore) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
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
                    .height(350.dp)
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
        "Síntomas Musculares" to (100 - healthScore.sintomasMuscularesScore),
        "Síntomas Visuales" to (100 - healthScore.sintomasVisualesScore),
        "Carga Trabajo" to (100 - healthScore.cargaTrabajoScore),
        "Estrés" to (100 - healthScore.estresSaludMentalScore),
        "Sueño" to (100 - healthScore.habitosSuenoScore),
        "Actividad Física" to (100 - healthScore.actividadFisicaScore),
        "Balance" to (100 - healthScore.balanceVidaTrabajoScore)
    )

    val riskColor = Color(healthScore.overallRisk.color)

    Canvas(modifier = modifier) {
        val centerX = size.width / 2
        val centerY = size.height / 2
        val center = Offset(centerX, centerY)

        val radius = size.minDimension / 3.0f
        val labelRadius = radius * 1.18f

        val angleStep = 360f / dataPoints.size

        val textPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.parseColor("#424242")
            textSize = 30f
            textAlign = android.graphics.Paint.Align.CENTER
            isAntiAlias = true
        }

        for (i in 1..5) {
            val currentRadius = radius * (i / 5f)
            drawCircle(
                color = Color.Gray.copy(alpha = 0.15f),
                radius = currentRadius,
                center = center,
                style = Stroke(width = 1.5.dp.toPx())
            )
        }

        dataPoints.forEachIndexed { index, _ ->
            val angle = Math.toRadians((angleStep * index - 90).toDouble())
            val end = Offset(
                centerX + (radius * cos(angle)).toFloat(),
                centerY + (radius * sin(angle)).toFloat()
            )
            drawLine(
                color = Color.Gray.copy(alpha = 0.25f),
                start = center,
                end = end,
                strokeWidth = 1.5.dp.toPx()
            )
        }

        val points = dataPoints.mapIndexed { index, (_, value) ->
            val angle = Math.toRadians((angleStep * index - 90).toDouble())
            val distance = radius * (value / 100f)
            Offset(
                centerX + (distance * cos(angle)).toFloat(),
                centerY + (distance * sin(angle)).toFloat()
            )
        }

        val path = androidx.compose.ui.graphics.Path().apply {
            if (points.isNotEmpty()) {
                moveTo(points[0].x, points[0].y)
                points.drop(1).forEach { lineTo(it.x, it.y) }
                close()
            }
        }

        drawPath(
            path = path,
            color = riskColor.copy(alpha = 0.25f)
        )

        drawPath(
            path = path,
            color = riskColor,
            style = Stroke(width = 3.dp.toPx())
        )

        points.forEach { point ->
            drawCircle(
                color = riskColor,
                radius = 6.dp.toPx(),
                center = point
            )
            drawCircle(
                color = Color.White,
                radius = 3.dp.toPx(),
                center = point
            )
        }

        dataPoints.forEachIndexed { index, (label, _) ->
            val angle = Math.toRadians((angleStep * index - 90).toDouble())

            val labelX = centerX + (labelRadius * cos(angle)).toFloat()
            val labelY = centerY + (labelRadius * sin(angle)).toFloat()

            val adjustedTextPaint = android.graphics.Paint(textPaint).apply {
                when {
                    labelX < centerX - 30 -> textAlign = android.graphics.Paint.Align.RIGHT
                    labelX > centerX + 30 -> textAlign = android.graphics.Paint.Align.LEFT
                    else -> textAlign = android.graphics.Paint.Align.CENTER
                }
            }

            val words = label.split(" ")

            drawIntoCanvas { canvas ->
                if (words.size > 1) {
                    val line1 = words[0]
                    val line2 = words.drop(1).joinToString(" ")

                    canvas.nativeCanvas.drawText(
                        line1,
                        labelX,
                        labelY - 8f,
                        adjustedTextPaint
                    )
                    canvas.nativeCanvas.drawText(
                        line2,
                        labelX,
                        labelY + 22f,
                        adjustedTextPaint
                    )
                } else {
                    canvas.nativeCanvas.drawText(
                        label,
                        labelX,
                        labelY + 8f,
                        adjustedTextPaint
                    )
                }
            }
        }
    }
}

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

@Composable
fun ProgressBarItem(
    label: String,
    score: Int,
    risk: RiskLevel,
    higherIsBetter: Boolean,
    trendIcon: androidx.compose.ui.graphics.vector.ImageVector? = null
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

// Función helper - CORREGIDA: solo cuenta áreas con score > 0
private fun countByRisk(healthScore: HealthScore, targetRisk: RiskLevel): Int {
    val risks = listOf(
        healthScore.ergonomiaScore to healthScore.ergonomiaRisk,
        healthScore.sintomasMuscularesScore to healthScore.sintomasMuscularesRisk,
        healthScore.sintomasVisualesScore to healthScore.sintomasVisualesRisk,
        healthScore.cargaTrabajoScore to healthScore.cargaTrabajoRisk,
        healthScore.estresSaludMentalScore to healthScore.estresSaludMentalRisk,
        healthScore.habitosSuenoScore to healthScore.habitosSuenoRisk,
        healthScore.actividadFisicaScore to healthScore.actividadFisicaRisk,
        healthScore.balanceVidaTrabajoScore to healthScore.balanceVidaTrabajoRisk
    )

    // Solo contar áreas que tienen datos (score > 0) y coinciden con el riesgo objetivo
    return risks.count { (score, risk) -> score > 0 && risk == targetRisk }
}

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
                text = "Responde al menos una encuesta en la pestaña 'Explorar' para ver tu análisis de salud laboral",
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
// En DashboardScreen.kt o ResultsScreen.kt

@Composable
fun EnhancedDashboard(enhancedScore: EnhancedHealthScore) {
    LazyColumn {
        // Completitud
        item {
            CompletenessCard(enhancedScore.completeness)
        }

        // Tendencias (si hay datos previos)
        enhancedScore.trendAnalysis?.let { trends ->
            item {
                TrendOverviewCard(trends)
            }

            item {
                AreaTrendsListCard(trends.areaTrends)
            }
        }

        // ... resto de cards
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BurnoutAIAnalysisCard(
    healthScore: HealthScore,
    onAnalyze: (Map<String, Float>) -> Unit
) {
    Card(
        onClick = {
            // ORDEN CRÍTICO: Debe coincidir EXACTAMENTE con el orden de entrenamiento de la red neuronal
            // 1. estres_index
            // 2. ergonomia_index
            // 3. carga_trabajo_index
            // 4. calidad_sueno_index
            // 5. actividad_fisica_index
            // 6. sintomas_musculares_index
            // 7. sintomas_visuales_index
            // 8. salud_general_index

            // IMPORTANTE: Ergonomía es la ÚNICA área donde score ALTO = BUENO
            // Para la red neuronal, TODOS los índices deben seguir: ALTO = MAYOR RIESGO
            // Por lo tanto, ergonomía debe invertirse: (100 - score)

            // Usar LinkedHashMap para GARANTIZAR el orden de inserción
            val indices = linkedMapOf(
                "estres_index" to (healthScore.estresSaludMentalScore / 100f * 10f),
                "ergonomia_index" to ((100 - healthScore.ergonomiaScore) / 100f * 10f),  // ⚠️ INVERTIDO
                "carga_trabajo_index" to (healthScore.cargaTrabajoScore / 100f * 10f),
                "calidad_sueno_index" to (healthScore.habitosSuenoScore / 100f * 10f),
                "actividad_fisica_index" to (healthScore.actividadFisicaScore / 100f * 10f),
                "sintomas_musculares_index" to (healthScore.sintomasMuscularesScore / 100f * 10f),
                "sintomas_visuales_index" to (healthScore.sintomasVisualesScore / 100f * 10f),
                "salud_general_index" to (healthScore.saludGeneralScore / 100f * 10f)
            )

            android.util.Log.d("BurnoutAnalysis", """
                📊 Datos preparados para red neuronal (escala 0-10, mayor=peor):
                  1. Estrés: ${indices["estres_index"]} (score original: ${healthScore.estresSaludMentalScore})
                  2. Ergonomía: ${indices["ergonomia_index"]} (score original: ${healthScore.ergonomiaScore} → INVERTIDO)
                  3. Carga Trabajo: ${indices["carga_trabajo_index"]} (score original: ${healthScore.cargaTrabajoScore})
                  4. Calidad Sueño: ${indices["calidad_sueno_index"]} (score original: ${healthScore.habitosSuenoScore})
                  5. Actividad Física: ${indices["actividad_fisica_index"]} (score original: ${healthScore.actividadFisicaScore})
                  6. Síntomas Musculares: ${indices["sintomas_musculares_index"]} (score original: ${healthScore.sintomasMuscularesScore})
                  7. Síntomas Visuales: ${indices["sintomas_visuales_index"]} (score original: ${healthScore.sintomasVisualesScore})
                  8. Salud General: ${indices["salud_general_index"]} (score original: ${healthScore.saludGeneralScore})
            """.trimIndent())

            onAnalyze(indices)
        },
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.Psychology,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.tertiary
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Análisis Predictivo con IA",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Burnout Risk Assessment",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.primary
                    ) {
                        Text(
                            text = "BETA",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    Text(
                        text = "Precisión: 82-86%",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Icon(
                imageVector = Icons.Filled.ArrowForward,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.tertiary
            )
        }
    }
}