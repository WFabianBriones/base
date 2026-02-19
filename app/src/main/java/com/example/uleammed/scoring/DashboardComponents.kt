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
import androidx.compose.material.icons.filled.Psychology  // ⭐ AGREGAR
import android.content.Context
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// --- Asume que RiskLevel, HealthScore, ScoringState y ScoringViewModel existen en otros archivos ---
// No se incluyen aquí por simplicidad, pero son necesarios para que el código compile.

/**
 * Dashboard principal con todos los gráficos de salud
 */
@Composable
fun HealthDashboard(
    viewModel: ScoringViewModel = viewModel(),
    onNavigateToBurnoutAnalysis: (Map<String, Float>) -> Unit = {}  // ⭐ AGREGAR
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
    onRecalculate: () -> Unit,
    onNavigateToBurnoutAnalysis: (Map<String, Float>) -> Unit
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

        if (completedSurveys.size >= 3) {
            item {
                BurnoutAIAnalysisCard(
                    healthScore = healthScore,
                    onAnalyze = onNavigateToBurnoutAnalysis
                )
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
                totalCount = 9  // Bug fix: salud_general es la 9ª encuesta, faltaba en el conteo
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
            ExportPdfButton(healthScore = healthScore)
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

/**
 * 1. Card de score global circular
 */
@Composable
fun OverallScoreCard(healthScore: HealthScore) {
    // overallScore está en escala "mayor = mayor riesgo" (0 = sin riesgo, 100 = crítico)
    // Se usa directamente: arco más lleno = más riesgo, coherente con la etiqueta.
    val riskScore = healthScore.overallScore.coerceIn(0, 100)

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
                text = "Riesgo en Salud Laboral",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            CircularScoreIndicator(
                score = riskScore,
                risk = healthScore.overallRisk,
                size = 180.dp
            )

            // Badge: fondo semitransparente, texto del color del riesgo
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = Color(healthScore.overallRisk.color).copy(alpha = 0.15f)
            ) {
                Text(
                    text = healthScore.overallRisk.displayName,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(healthScore.overallRisk.color)
                )
            }

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

            Text(
                text = "Índice de riesgo (0 = sin riesgo · 100 = crítico)",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
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
/**
 * Gráfico de radar MEJORADO (spider chart) - VERSIÓN CORREGIDA
 */
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

            // Nota de convención: mayor área = mayor riesgo (polígono crece con problemas)
            Text(
                text = "Área mayor = mayor riesgo   ·   Centro = sin riesgo",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
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
    // Convención: todos los valores en escala "mayor = más riesgo" (0 = sin riesgo, 100 = crítico).
    // Así el polígono CRECE cuando hay problemas, coherente con el gráfico circular.
    //
    // Casos especiales:
    //   • ergonomiaScore: escala invertida (mayor = mejor) → se usa (100 - score).
    //     Si score == -1 (no completado) → se trata como 0 riesgo, NO como 100.
    //   • cualquier otro score == -1 → coerceAtLeast(0) → punto en el centro (sin dato).
    val dataPoints = listOf(
        "Ergonomía"         to (if (healthScore.ergonomiaScore == -1) 0 else (100 - healthScore.ergonomiaScore).coerceIn(0, 100)),
        "Sint. Musculares"  to healthScore.sintomasMuscularesScore.coerceAtLeast(0),
        "Sint. Visuales"    to healthScore.sintomasVisualesScore.coerceAtLeast(0),
        "Carga Trabajo"     to healthScore.cargaTrabajoScore.coerceAtLeast(0),
        "Estrés"            to healthScore.estresSaludMentalScore.coerceAtLeast(0),
        "Sueño"             to healthScore.habitosSuenoScore.coerceAtLeast(0),
        "Act. Física"       to healthScore.actividadFisicaScore.coerceAtLeast(0),
        "Balance"           to healthScore.balanceVidaTrabajoScore.coerceAtLeast(0)
    )

    val riskColor = Color(healthScore.overallRisk.color)

    Canvas(modifier = modifier) {
        val centerX = size.width / 2
        val centerY = size.height / 2
        val center = Offset(centerX, centerY)

        // Radio del área de datos. Deja margen suficiente para las etiquetas en todos los lados.
        val radius = size.minDimension / 3.2f
        // Bug B fix: labelRadius debe alejarse lo suficiente del borde del polígono.
        // Con 1.40f hay margen incluso cuando el vértice está en radio=1.0 (score 100).
        val labelRadius = radius * 1.42f

        val angleStep = 360f / dataPoints.size

        val textPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.parseColor("#424242")
            textSize = 28f
            textAlign = android.graphics.Paint.Align.CENTER
            isAntiAlias = true
        }

        // 1. Círculos de fondo (telaraña)
        for (i in 1..5) {
            drawCircle(
                color = Color.Gray.copy(alpha = 0.15f),
                radius = radius * (i / 5f),
                center = center,
                style = Stroke(width = 1.5.dp.toPx())
            )
        }

        // 2. Líneas radiales
        dataPoints.forEachIndexed { index, _ ->
            val angle = Math.toRadians((angleStep * index - 90).toDouble())
            drawLine(
                color = Color.Gray.copy(alpha = 0.25f),
                start = center,
                end = Offset(
                    centerX + (radius * cos(angle)).toFloat(),
                    centerY + (radius * sin(angle)).toFloat()
                ),
                strokeWidth = 1.5.dp.toPx()
            )
        }

        // 3. Calcular puntos del polígono
        val points = dataPoints.mapIndexed { index, (_, value) ->
            val angle = Math.toRadians((angleStep * index - 90).toDouble())
            val distance = radius * (value / 100f)
            Offset(
                centerX + (distance * cos(angle)).toFloat(),
                centerY + (distance * sin(angle)).toFloat()
            )
        }

        // 4. Polígono con relleno
        val path = androidx.compose.ui.graphics.Path().apply {
            if (points.isNotEmpty()) {
                moveTo(points[0].x, points[0].y)
                points.drop(1).forEach { lineTo(it.x, it.y) }
                close()
            }
        }
        drawPath(path = path, color = riskColor.copy(alpha = 0.25f))
        drawPath(path = path, color = riskColor, style = Stroke(width = 3.dp.toPx()))

        // 5. Puntos en vértices
        points.forEach { point ->
            drawCircle(color = riskColor, radius = 6.dp.toPx(), center = point)
            drawCircle(color = Color.White, radius = 3.dp.toPx(), center = point)
        }

        // 6. Etiquetas — Bug B fix: drawIntoCanvas se llama UNA SOLA VEZ fuera del bucle.
        // Llamarlo dentro del forEach creaba un wrapper canvas por iteración innecesariamente.
        drawIntoCanvas { canvas ->
            dataPoints.forEachIndexed { index, (label, _) ->
                val angle = Math.toRadians((angleStep * index - 90).toDouble())
                val labelX = centerX + (labelRadius * cos(angle)).toFloat()
                val labelY = centerY + (labelRadius * sin(angle)).toFloat()

                val paint = android.graphics.Paint(textPaint).apply {
                    textAlign = when {
                        labelX < centerX - 20f -> android.graphics.Paint.Align.RIGHT
                        labelX > centerX + 20f -> android.graphics.Paint.Align.LEFT
                        else                   -> android.graphics.Paint.Align.CENTER
                    }
                }

                val words = label.split(" ")
                if (words.size > 1) {
                    canvas.nativeCanvas.drawText(words[0], labelX, labelY - 10f, paint)
                    canvas.nativeCanvas.drawText(
                        words.drop(1).joinToString(" "), labelX, labelY + 20f, paint
                    )
                } else {
                    canvas.nativeCanvas.drawText(label, labelX, labelY + 8f, paint)
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
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Análisis por Área",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            // Bug fix: salud_general ahora aparece si fue completada.
            // Bug fix: se eliminaron los trendIcon hardcodeados ("de EJEMPLO").
            //          El ícono de estado se deriva del RiskLevel real de cada área.
            // Bug fix: higherIsBetter=true solo en ergonomía (mayor score = mejor ergonomía).
            if (completedSurveys.contains("salud_general")) {
                ProgressBarItem("Salud General", healthScore.saludGeneralScore, healthScore.saludGeneralRisk, higherIsBetter = false)
            }
            if (completedSurveys.contains("ergonomia")) {
                ProgressBarItem("Ergonomía", healthScore.ergonomiaScore, healthScore.ergonomiaRisk, higherIsBetter = true)
            }
            if (completedSurveys.contains("sintomas_musculares")) {
                ProgressBarItem("Síntomas Musculares", healthScore.sintomasMuscularesScore, healthScore.sintomasMuscularesRisk, higherIsBetter = false)
            }
            if (completedSurveys.contains("sintomas_visuales")) {
                ProgressBarItem("Síntomas Visuales", healthScore.sintomasVisualesScore, healthScore.sintomasVisualesRisk, higherIsBetter = false)
            }
            if (completedSurveys.contains("carga_trabajo")) {
                ProgressBarItem("Carga de Trabajo", healthScore.cargaTrabajoScore, healthScore.cargaTrabajoRisk, higherIsBetter = false)
            }
            if (completedSurveys.contains("estres")) {
                ProgressBarItem("Estrés y Salud Mental", healthScore.estresSaludMentalScore, healthScore.estresSaludMentalRisk, higherIsBetter = false)
            }
            if (completedSurveys.contains("sueno")) {
                ProgressBarItem("Calidad del Sueño", healthScore.habitosSuenoScore, healthScore.habitosSuenoRisk, higherIsBetter = false)
            }
            if (completedSurveys.contains("actividad_fisica")) {
                ProgressBarItem("Actividad Física", healthScore.actividadFisicaScore, healthScore.actividadFisicaRisk, higherIsBetter = false)
            }
            if (completedSurveys.contains("balance")) {
                ProgressBarItem("Balance Vida-Trabajo", healthScore.balanceVidaTrabajoScore, healthScore.balanceVidaTrabajoRisk, higherIsBetter = false)
            }

            // Leyenda de lectura
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Info,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Barra más llena = mayor riesgo en esa área",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun ProgressBarItem(
    label: String,
    score: Int,
    risk: RiskLevel,
    higherIsBetter: Boolean
) {
    // Bug fix: normalizar SIEMPRE a escala "mayor = más riesgo" antes de mostrar.
    // Con higherIsBetter=true (ergonomía: 85 = muy buena), el riskScore = 100-85 = 15.
    // Así el badge y la barra muestran el mismo valor y ambos son coherentes:
    // barra corta + número bajo = área en buen estado.
    val riskScore = if (higherIsBetter) (100 - score).coerceIn(0, 100) else score.coerceIn(0, 100)

    // Ícono derivado del RiskLevel real del área (no hardcodeado).
    val statusIcon = when (risk) {
        RiskLevel.BAJO     -> Icons.Filled.CheckCircle
        RiskLevel.MODERADO -> Icons.Filled.Warning
        RiskLevel.ALTO     -> Icons.Filled.Error
        RiskLevel.MUY_ALTO -> Icons.Filled.Error
    }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Ícono de estado a la izquierda del label
            Icon(
                imageVector = statusIcon,
                contentDescription = risk.displayName,
                tint = Color(risk.color),
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))

            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )

            // Badge: muestra el riskScore normalizado + nivel de riesgo.
            // Bug fix: antes mostraba el score raw de ergonomía (85) mientras la barra
            // estaba al 15%. Ahora ambos muestran el mismo índice de riesgo.
            Surface(
                shape = MaterialTheme.shapes.small,
                color = Color(risk.color).copy(alpha = 0.15f)
            ) {
                Text(
                    text = "$riskScore  ·  ${risk.displayName}",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(risk.color)
                )
            }
        }

        LinearProgressIndicator(
            progress = { riskScore / 100f },
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
fun TopConcernsCard(concerns: List<AreaConcern>) {
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
                // Bug fix: antes todos los badges eran el mismo color rojo (error).
                // Ahora cada fila usa el color real de su RiskLevel:
                // MUY_ALTO = rojo, ALTO = naranja, MODERADO = amarillo.
                val riskColor = Color(concern.risk.color)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Número de orden con color del nivel de riesgo
                    Surface(
                        shape = CircleShape,
                        color = riskColor,
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

                    // Nombre del área
                    Text(
                        text = concern.name,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )

                    // Badge del nivel de riesgo
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = riskColor.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = concern.risk.displayName,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = riskColor
                        )
                    }
                }
            }
        }
    }
}

/**
 * 6. Recomendaciones
 */
@Composable
fun RecommendationsCard(recommendations: List<Recommendation>) {
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
                // Bug fix: antes todas las recomendaciones usaban el mismo ArrowRight azul.
                // Ahora el ícono y el color reflejan la urgencia real:
                //   MUY_ALTO → Error (rojo)
                //   ALTO     → Warning (naranja)
                //   MODERADO → ArrowRight (azul, sugerencia)
                //   BAJO     → CheckCircle (verde, estado positivo)
                val (icon, tint) = when (recommendation.urgency) {
                    RiskLevel.MUY_ALTO -> Icons.Filled.Error to Color(RiskLevel.MUY_ALTO.color)
                    RiskLevel.ALTO     -> Icons.Filled.Warning to Color(RiskLevel.ALTO.color)
                    RiskLevel.MODERADO -> Icons.Filled.ArrowRight to MaterialTheme.colorScheme.primary
                    RiskLevel.BAJO     -> Icons.Filled.CheckCircle to Color(RiskLevel.BAJO.color)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = recommendation.urgency.displayName,
                        tint = tint,
                        modifier = Modifier
                            .size(20.dp)
                            .padding(top = 2.dp)
                    )
                    Text(
                        text = recommendation.text,
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

// Bug fix: solo contar áreas cuya encuesta fue completada (score != -1).
// Antes, los Risk defaultean a RiskLevel.BAJO aunque el score sea -1,
// lo que hacía que todas las áreas sin datos aparecieran como "Áreas OK".
private fun countByRisk(healthScore: HealthScore, targetRisk: RiskLevel): Int {
    val completedAreaRisks = buildList {
        if (healthScore.saludGeneralScore != -1)        add(healthScore.saludGeneralRisk)
        if (healthScore.ergonomiaScore != -1)           add(healthScore.ergonomiaRisk)
        if (healthScore.sintomasMuscularesScore != -1)  add(healthScore.sintomasMuscularesRisk)
        if (healthScore.sintomasVisualesScore != -1)    add(healthScore.sintomasVisualesRisk)
        if (healthScore.cargaTrabajoScore != -1)        add(healthScore.cargaTrabajoRisk)
        if (healthScore.estresSaludMentalScore != -1)   add(healthScore.estresSaludMentalRisk)
        if (healthScore.habitosSuenoScore != -1)        add(healthScore.habitosSuenoRisk)
        if (healthScore.actividadFisicaScore != -1)     add(healthScore.actividadFisicaRisk)
        if (healthScore.balanceVidaTrabajoScore != -1)  add(healthScore.balanceVidaTrabajoRisk)
    }
    return completedAreaRisks.count { it == targetRisk }
}

/**
 * Helper: Detectar qué encuestas están completadas
 */
private fun getCompletedSurveys(healthScore: HealthScore): List<String> {
    val completed = mutableListOf<String>()

    // Bug fix: salud_general faltaba aunque se calcula y guarda en HealthScore
    if (healthScore.saludGeneralScore != -1) completed.add("salud_general")
    if (healthScore.ergonomiaScore != -1) completed.add("ergonomia")
    if (healthScore.sintomasMuscularesScore != -1) completed.add("sintomas_musculares")
    if (healthScore.sintomasVisualesScore != -1) completed.add("sintomas_visuales")
    if (healthScore.cargaTrabajoScore != -1) completed.add("carga_trabajo")
    if (healthScore.estresSaludMentalScore != -1) completed.add("estres")
    if (healthScore.habitosSuenoScore != -1) completed.add("sueno")
    if (healthScore.actividadFisicaScore != -1) completed.add("actividad_fisica")
    if (healthScore.balanceVidaTrabajoScore != -1) completed.add("balance")

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
                text = "Responde las encuestas en la pestaña 'Explorar' para ver tu análisis de salud laboral",
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
/**
 * ⭐ Card de análisis de burnout con IA
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BurnoutAIAnalysisCard(
    healthScore: HealthScore,
    onAnalyze: (Map<String, Float>) -> Unit
) {
    Card(
        onClick = {
            val indices = mapOf(
                "estres" to (healthScore.estresSaludMentalScore / 100f * 10f),
                // ergonomiaScore es mayor=mejor → invertir para que el índice sea mayor=peor
                "ergonomia" to ((100 - healthScore.ergonomiaScore) / 100f * 10f),
                "carga_trabajo" to (healthScore.cargaTrabajoScore / 100f * 10f),
                "calidad_sueno" to (healthScore.habitosSuenoScore / 100f * 10f),
                "actividad_fisica" to (healthScore.actividadFisicaScore / 100f * 10f),
                "sintomas_musculares" to (healthScore.sintomasMuscularesScore / 100f * 10f),
                "sintomas_visuales" to (healthScore.sintomasVisualesScore / 100f * 10f),
                "salud_general" to (healthScore.saludGeneralScore / 100f * 10f)
            )
            onAnalyze(indices)
        },
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
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
                tint = MaterialTheme.colorScheme.onSurfaceVariant
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
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "BETA",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
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
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}