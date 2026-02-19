package com.example.uleammed.tendencias

import androidx.compose.ui.graphics.Color

enum class CollectionPeriod(
    val days: Int,
    val label: String,
    val windowLabel: String
) {
    WEEKLY(   days = 7,  label = "Sem", windowLabel = "últimas 4 semanas"),
    BIWEEKLY( days = 14, label = "Q",   windowLabel = "últimas 4 quincenas"),
    MONTHLY(  days = 30, label = "Mes", windowLabel = "últimos 4 meses");

    companion object {
        fun fromDays(days: Int): CollectionPeriod = when (days) {
            7    -> WEEKLY
            14   -> BIWEEKLY
            30   -> MONTHLY
            else -> WEEKLY
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────
//  Snapshot leído de Firestore
//
//  CAMBIO: se añadió `storedGlobalScore` para recibir el campo "globalScore"
//  guardado en Firestore. La propiedad computed `globalScore` se renombra a
//  `effectiveGlobalScore` para evitar el warning de CustomClassMapper.
//
//  Regla: Firestore solo puede mapear campos que tengan su nombre exacto en
//  el constructor. Una propiedad `val` sin argumento en el constructor es
//  invisible para el mapper → "No setter/field found".
// ─────────────────────────────────────────────────────────────────────────
data class HealthSnapshot(
    val timestamp                 : Long    = 0L,
    val estresIndex               : Float   = 0f,
    val ergonomiaIndex            : Float   = 0f,
    val cargaTrabajoIndex         : Float   = 0f,
    val calidadSuenoIndex         : Float   = 0f,
    val actividadFisicaIndex      : Float   = 0f,
    val sintomasMuscularesIndex   : Float   = 0f,
    val sintomasVisualesIndex     : Float   = 0f,
    val saludGeneralIndex         : Float   = 0f,
    val nivelRiesgo               : String  = "BAJO",
    val confianza                 : Float   = 0f,
    val storedGlobalScore         : Float?  = null   // ← campo real de Firestore
) {
    // Usa el valor guardado en Firestore si existe; si no, lo calcula
    val effectiveGlobalScore: Float
        get() = storedGlobalScore ?: listOf(
            estresIndex, ergonomiaIndex, cargaTrabajoIndex,
            calidadSuenoIndex, actividadFisicaIndex,
            sintomasMuscularesIndex, sintomasVisualesIndex,
            saludGeneralIndex
        ).average().toFloat() * 10f
}

data class PeriodHealthSnapshot(
    val periodLabel         : String,
    val periodStart         : Long,
    val estresScore         : Float,
    val ergonomiaScore      : Float,
    val cargaTrabajoScore   : Float,
    val calidadSuenoScore   : Float,
    val actividadFisicaScore: Float,
    val musculoScore        : Float,
    val visualScore         : Float,
    val saludGeneralScore   : Float,
    val globalScore         : Float
)

enum class HealthIndex(val label: String, val color: Color) {
    ESTRES(   "Estrés",        Color(0xFFE53935)),
    ERGONOMIA("Ergonomía",     Color(0xFF1E88E5)),
    CARGA(    "Carga laboral", Color(0xFF8E24AA)),
    SUENO(    "Sueño",         Color(0xFF00897B)),
    ACTIVIDAD("Act. física",   Color(0xFF43A047)),
    MUSCULO(  "Músculo-Esq.",  Color(0xFFFF8F00)),
    VISUAL(   "Visual",        Color(0xFF039BE5)),
    GLOBAL(   "Global",        Color(0xFF546E7A))
}

enum class TrendDirection(val emoji: String, val label: String, val color: Color) {
    IMPROVING("📈", "Tendencia positiva",  Color(0xFF43A047)),
    STABLE(   "➡️", "Tendencia estable",   Color(0xFF1E88E5)),
    DECLINING("📉", "Tendencia a la baja", Color(0xFFE53935))
}

data class TendenciasUiState(
    val snapshots      : List<PeriodHealthSnapshot> = emptyList(),
    val selectedIndices: Set<HealthIndex>            = setOf(HealthIndex.GLOBAL),
    val trendDirection : TrendDirection              = TrendDirection.STABLE,
    val period         : CollectionPeriod            = CollectionPeriod.WEEKLY,
    val isLoading      : Boolean                     = true,
    val error          : String?                     = null
)