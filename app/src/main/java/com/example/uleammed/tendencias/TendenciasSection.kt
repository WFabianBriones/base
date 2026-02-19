package com.example.uleammed.tendencias

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlin.math.abs

@Composable
fun TendenciasSection(
    uiState: TendenciasUiState,
    onToggle: (HealthIndex) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier  = modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier            = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            TrendHeader(
                direction   = uiState.trendDirection,
                windowLabel = uiState.period.windowLabel
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            AnimatedContent(
                targetState = when {
                    uiState.isLoading           -> ContentState.LOADING
                    uiState.error != null       -> ContentState.ERROR
                    uiState.snapshots.isEmpty() -> ContentState.EMPTY
                    else                        -> ContentState.DATA
                },
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label          = "TendenciasContent"
            ) { state ->
                when (state) {
                    ContentState.LOADING -> TrendLoadingState()
                    ContentState.ERROR   -> TrendErrorState(uiState.error ?: "", onRetry)
                    ContentState.EMPTY   -> TrendEmptyState(uiState.period)
                    ContentState.DATA    -> TrendDataContent(
                        snapshots       = uiState.snapshots,
                        selectedIndices = uiState.selectedIndices,
                        trendDirection  = uiState.trendDirection,
                        onToggle        = onToggle
                    )
                }
            }
        }
    }
}

private enum class ContentState { LOADING, ERROR, EMPTY, DATA }

// ─────────────────────────────────────────────
//  Encabezado dinámico según período
// ─────────────────────────────────────────────
@Composable
private fun TrendHeader(direction: TrendDirection, windowLabel: String) {
    Row(
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(direction.emoji, style = MaterialTheme.typography.titleLarge)
        Column {
            Text(
                text       = "Tendencias — $windowLabel",
                style      = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text  = direction.label,
                style = MaterialTheme.typography.bodySmall,
                color = direction.color
            )
        }
    }
}

// ─────────────────────────────────────────────
//  Chips + gráfico + resumen
// ─────────────────────────────────────────────
@Composable
private fun TrendDataContent(
    snapshots: List<PeriodHealthSnapshot>,
    selectedIndices: Set<HealthIndex>,
    trendDirection: TrendDirection,
    onToggle: (HealthIndex) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        IndexFilterChips(selectedIndices, onToggle)
        HealthLineChart(snapshots, selectedIndices)
        TrendSummaryText(snapshots, trendDirection)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun IndexFilterChips(
    selectedIndices: Set<HealthIndex>,
    onToggle: (HealthIndex) -> Unit
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement   = Arrangement.spacedBy(4.dp)
    ) {
        HealthIndex.entries.forEach { index ->
            val selected = index in selectedIndices
            FilterChip(
                selected = selected,
                onClick  = { onToggle(index) },
                label    = { Text(index.label, style = MaterialTheme.typography.labelSmall) },
                colors   = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = index.color.copy(alpha = 0.18f),
                    selectedLabelColor     = index.color
                )
            )
        }
    }
}

// ─────────────────────────────────────────────
//  Gráfico de líneas — Canvas puro (sin Vico)
// ─────────────────────────────────────────────
@Composable
private fun HealthLineChart(
    snapshots: List<PeriodHealthSnapshot>,
    selectedIndices: Set<HealthIndex>,
    modifier: Modifier = Modifier
) {
    if (snapshots.isEmpty() || selectedIndices.isEmpty()) return

    // Recoge las series de datos seleccionadas
    val series: List<Pair<Color, List<Float>>> = selectedIndices.map { index ->
        index.color to snapshots.map { it.scoreFor(index) }
    }

    val gridColor   = Color.Gray.copy(alpha = 0.25f)
    val pointRadius = 4.dp

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp)
            .padding(start = 8.dp, end = 8.dp, bottom = 4.dp)
    ) {
        val w = size.width
        val h = size.height
        val padL = 0f
        val padB = 0f
        val chartW = w - padL
        val chartH = h - padB

        val allValues = series.flatMap { it.second }
        val minVal = (allValues.minOrNull() ?: 0f) - 5f
        val maxVal = (allValues.maxOrNull() ?: 100f) + 5f
        val range  = (maxVal - minVal).takeIf { it > 0 } ?: 1f

        val n = snapshots.size

        fun xOf(i: Int) = padL + i * (chartW / (n - 1).coerceAtLeast(1))
        fun yOf(v: Float) = chartH - ((v - minVal) / range) * chartH

        // Líneas de cuadrícula horizontales (5 niveles)
        val gridLevels = 5
        repeat(gridLevels + 1) { lvl ->
            val y = chartH * lvl / gridLevels
            drawLine(
                color       = gridColor,
                start       = Offset(padL, y),
                end         = Offset(w, y),
                strokeWidth = 1.dp.toPx()
            )
        }

        // Dibuja cada serie
        series.forEach { (color, values) ->
            if (values.size < 2) return@forEach

            // Área rellena (gradiente simulado con alpha bajo)
            val areaPath = Path().apply {
                moveTo(xOf(0), chartH)
                lineTo(xOf(0), yOf(values[0]))
                for (i in 1 until values.size) {
                    lineTo(xOf(i), yOf(values[i]))
                }
                lineTo(xOf(values.size - 1), chartH)
                close()
            }
            drawPath(areaPath, color = color.copy(alpha = 0.12f))

            // Línea principal
            val linePath = Path().apply {
                moveTo(xOf(0), yOf(values[0]))
                for (i in 1 until values.size) {
                    // Curva cúbica suave entre puntos
                    val x0 = xOf(i - 1); val y0 = yOf(values[i - 1])
                    val x1 = xOf(i);     val y1 = yOf(values[i])
                    val cx = (x0 + x1) / 2f
                    cubicTo(cx, y0, cx, y1, x1, y1)
                }
            }
            drawPath(
                linePath,
                color       = color,
                style       = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
            )

            // Puntos en cada dato
            values.forEachIndexed { i, v ->
                drawCircle(
                    color  = color,
                    radius = pointRadius.toPx(),
                    center = Offset(xOf(i), yOf(v))
                )
                drawCircle(
                    color  = Color.White,
                    radius = (pointRadius - 1.5.dp).toPx(),
                    center = Offset(xOf(i), yOf(v))
                )
            }
        }
    }
}

private fun PeriodHealthSnapshot.scoreFor(index: HealthIndex): Float = when (index) {
    HealthIndex.ESTRES    -> estresScore
    HealthIndex.ERGONOMIA -> ergonomiaScore
    HealthIndex.CARGA     -> cargaTrabajoScore
    HealthIndex.SUENO     -> calidadSuenoScore
    HealthIndex.ACTIVIDAD -> actividadFisicaScore
    HealthIndex.MUSCULO   -> musculoScore
    HealthIndex.VISUAL    -> visualScore
    HealthIndex.GLOBAL    -> globalScore
}

// ─────────────────────────────────────────────
//  Resumen textual
// ─────────────────────────────────────────────
@Composable
private fun TrendSummaryText(
    snapshots: List<PeriodHealthSnapshot>,
    direction: TrendDirection
) {
    if (snapshots.size < 2) return

    val first      = snapshots.first()
    val last       = snapshots.last()
    val globalDiff = last.globalScore - first.globalScore
    val absDiff    = abs(globalDiff)

    val changes = mapOf(
        "Estrés"        to (last.estresScore         - first.estresScore),
        "Ergonomía"     to (last.ergonomiaScore       - first.ergonomiaScore),
        "Carga laboral" to (last.cargaTrabajoScore    - first.cargaTrabajoScore),
        "Sueño"         to (last.calidadSuenoScore    - first.calidadSuenoScore),
        "Act. física"   to (last.actividadFisicaScore - first.actividadFisicaScore),
        "Músculo-Esq."  to (last.musculoScore         - first.musculoScore),
        "Visual"        to (last.visualScore          - first.visualScore)
    )

    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = when {
                absDiff < 2f   -> "Tu salud general se ha mantenido estable en este período."
                globalDiff > 0 -> "Tu score global mejoró ${String.format("%.1f", absDiff)} pts en este período."
                else           -> "Tu score global bajó ${String.format("%.1f", absDiff)} pts. Revisa tus hábitos."
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        changes.maxByOrNull { it.value }?.let { (name, change) ->
            if (change > 3f) Text(
                "✅ $name mejoró ${String.format("%.1f", change)} pts",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF43A047)
            )
        }
        changes.minByOrNull { it.value }?.let { (name, change) ->
            if (change < -3f) Text(
                "⚠️ $name bajó ${String.format("%.1f", abs(change))} pts — revisa tus hábitos",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFE53935)
            )
        }
    }
}

// ─────────────────────────────────────────────
//  Estados de carga / vacío / error
// ─────────────────────────────────────────────
@Composable
private fun TrendLoadingState() {
    Box(
        modifier = Modifier.fillMaxWidth().height(180.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            Text(
                "Analizando tendencias...",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun TrendEmptyState(period: CollectionPeriod) {
    val periodText = when (period) {
        CollectionPeriod.WEEKLY   -> "2 semanas"
        CollectionPeriod.BIWEEKLY -> "2 quincenas"
        CollectionPeriod.MONTHLY  -> "2 meses"
    }
    Box(
        modifier = Modifier.fillMaxWidth().height(140.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text("📋", style = MaterialTheme.typography.displaySmall)
            Text(
                text      = "Ejecuta el análisis IA al menos\n$periodText seguidas para ver tendencias",
                style     = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                color     = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun TrendErrorState(message: String, onRetry: () -> Unit) {
    Column(
        modifier            = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("⚠️", style = MaterialTheme.typography.titleLarge)
        Text(
            text      = message,
            style     = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            color     = MaterialTheme.colorScheme.error
        )
        OutlinedButton(onClick = onRetry) { Text("Reintentar") }
    }
}