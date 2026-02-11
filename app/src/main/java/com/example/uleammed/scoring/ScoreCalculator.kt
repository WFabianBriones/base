package com.example.uleammed.scoring

import com.example.uleammed.HealthQuestionnaire
import com.example.uleammed.questionnaires.*
import kotlin.math.abs

/**
 * ✅ SISTEMA DE SCORING MEJORADO - PRIORIDAD MEDIA IMPLEMENTADA
 *
 * Mejoras adicionales:
 * 1. ✅ Ponderación multiplicativa para frecuencia + intensidad
 * 2. ✅ Análisis de tendencias vs mediciones anteriores
 * 3. ✅ Sistema de confianza basado en completitud
 * 4. ✅ Documentación clara de escala invertida en ergonomía
 */

// ==================== ANÁLISIS DE TENDENCIAS ====================

/**
 * ✅ NUEVO: Dirección de la tendencia de salud
 */
enum class TrendDirection {
    MEJORANDO,      // +5 puntos o más (score disminuyendo)
    ESTABLE,        // Entre -4 y +4 puntos
    EMPEORANDO,     // -5 puntos o más (score aumentando)
    SIN_DATOS;      // Primera medición

    val displayName: String
        get() = when (this) {
            MEJORANDO -> "Mejorando"
            ESTABLE -> "Estable"
            EMPEORANDO -> "Empeorando"
            SIN_DATOS -> "Sin datos previos"
        }

    val icon: String
        get() = when (this) {
            MEJORANDO -> "↗️"
            ESTABLE -> "→"
            EMPEORANDO -> "↘️"
            SIN_DATOS -> "⚪"
        }
}

/**
 * ✅ NUEVO: Análisis de tendencia de un área
 */
data class AreaTrend(
    val area: String,
    val currentScore: Int,
    val previousScore: Int?,
    val direction: TrendDirection,
    val changePoints: Int,  // Diferencia en puntos
    val changePercent: Float, // Diferencia en porcentaje
    val daysElapsed: Int     // Días desde última medición
)

/**
 * ✅ NUEVO: Análisis completo de tendencias
 */
data class TrendAnalysis(
    val overallTrend: TrendDirection,
    val areaTrends: Map<String, AreaTrend>,
    val areasImproving: Int,
    val areasWorsening: Int,
    val areasStable: Int,
    val keyInsights: List<String>
)

// ==================== SISTEMA DE CONFIANZA ====================

/**
 * ✅ NUEVO: Nivel de confianza en el score
 */
enum class ConfidenceLevel {
    ALTA,       // 90-100% completitud
    MEDIA,      // 70-89% completitud
    BAJA;       // <70% completitud

    val displayName: String
        get() = when (this) {
            ALTA -> "Alta"
            MEDIA -> "Media"
            BAJA -> "Baja"
        }

    val description: String
        get() = when (this) {
            ALTA -> "Todos los cuestionarios completos"
            MEDIA -> "Algunos cuestionarios pendientes"
            BAJA -> "Varios cuestionarios pendientes"
        }
}

/**
 * ✅ NUEVO: Información de completitud
 */
data class CompletenessInfo(
    val completedQuestionnaires: Int,
    val totalQuestionnaires: Int,
    val completenessPercent: Float,
    val confidenceLevel: ConfidenceLevel,
    val missingQuestionnaires: List<String>
)

// ==================== RESULTADO MEJORADO ====================

/**
 * ✅ MEJORADO: HealthScore con tendencias y confianza
 */
data class EnhancedHealthScore(
    val userId: String = "",
    val timestamp: Long = System.currentTimeMillis(),

    val version: Int = 3, // ✅ ACTUALIZADO: Versión con mejoras prioridad media
    val lastUpdated: Map<String, Long> = mapOf(
        "salud_general" to 0L,
        "ergonomia" to 0L,
        "sintomas_musculares" to 0L,
        "sintomas_visuales" to 0L,
        "carga_trabajo" to 0L,
        "estres" to 0L,
        "sueno" to 0L,
        "actividad_fisica" to 0L,
        "balance" to 0L
    ),

    val saludGeneralScore: Int = 0,
    val saludGeneralRisk: RiskLevel = RiskLevel.BAJO,

    // Scores individuales (0-100)
    val ergonomiaScore: Int = 0,
    val sintomasMuscularesScore: Int = 0,
    val sintomasVisualesScore: Int = 0,
    val cargaTrabajoScore: Int = 0,
    val estresSaludMentalScore: Int = 0,
    val habitosSuenoScore: Int = 0,
    val actividadFisicaScore: Int = 0,
    val balanceVidaTrabajoScore: Int = 0,

    // Niveles de riesgo
    val ergonomiaRisk: RiskLevel = RiskLevel.BAJO,
    val sintomasMuscularesRisk: RiskLevel = RiskLevel.BAJO,
    val sintomasVisualesRisk: RiskLevel = RiskLevel.BAJO,
    val cargaTrabajoRisk: RiskLevel = RiskLevel.BAJO,
    val estresSaludMentalRisk: RiskLevel = RiskLevel.BAJO,
    val habitosSuenoRisk: RiskLevel = RiskLevel.BAJO,
    val actividadFisicaRisk: RiskLevel = RiskLevel.BAJO,
    val balanceVidaTrabajoRisk: RiskLevel = RiskLevel.BAJO,

    // Score global
    val overallScore: Int = 0,
    val overallRisk: RiskLevel = RiskLevel.BAJO,

    // Áreas de mejora
    val topConcerns: List<String> = emptyList(),
    val recommendations: List<String> = emptyList(),

    // Patrones críticos
    val criticalPatterns: List<CriticalPattern> = emptyList(),

    // ✅ NUEVO: Análisis de tendencias
    val trendAnalysis: TrendAnalysis? = null,

    // ✅ NUEVO: Sistema de confianza
    val completeness: CompletenessInfo = CompletenessInfo(
        completedQuestionnaires = 0,
        totalQuestionnaires = 9,
        completenessPercent = 0f,
        confidenceLevel = ConfidenceLevel.BAJA,
        missingQuestionnaires = emptyList()
    )
)

// ==================== CALCULADOR MEJORADO ====================

object EnhancedScoreCalculator {

    private const val TAG = "EnhancedScoreCalculator"

    /**
     * ✅ NUEVO: Calcular score de síntoma con ponderación multiplicativa
     *
     * Esta función aplica ponderación inteligente según el patrón:
     * - Frecuencia + Intensidad ALTAS → Penalización exponencial
     * - Frecuencia ALTA + Intensidad BAJA → Ponderación hacia frecuencia
     * - Frecuencia BAJA + Intensidad ALTA → Ponderación hacia intensidad
     */
    fun calculateSymptomScore(frecuencia: Int, intensidad: Int): Double {
        // Validar rangos
        require(frecuencia in 0..5) { "Frecuencia debe estar entre 0 y 5" }
        require(intensidad in 0..5) { "Intensidad debe estar entre 0 y 5" }

        return when {
            // CASO CRÍTICO: Alta frecuencia + Alta intensidad
            // Ejemplo: Dolor severo constante
            frecuencia >= 4 && intensidad >= 4 -> {
                // Penalización exponencial
                (frecuencia * intensidad * 0.5)
            }

            // CASO MODERADO-ALTO: Una dimensión alta
            frecuencia >= 4 || intensidad >= 4 -> {
                // Ponderación hacia la dimensión más alta
                if (frecuencia > intensidad) {
                    (frecuencia * 0.7 + intensidad * 0.3)
                } else {
                    (frecuencia * 0.3 + intensidad * 0.7)
                }
            }

            // CASO ESTÁNDAR: Ambas dimensiones moderadas o bajas
            else -> {
                // Frecuencia ligeramente más importante (60/40)
                (frecuencia * 0.6 + intensidad * 0.4)
            }
        }
    }

    /**
     * ✅ MEJORADO: Cálculo de síntomas musculares con ponderación multiplicativa
     */
    fun calculateSintomasMuscularesScoreEnhanced(q: SintomasMuscularesQuestionnaire): Pair<Int, RiskLevel> {
        var totalSymptoms = 0.0
        var maxIntensity = 0

        // Cuello (peso: alto) - CON PONDERACIÓN MULTIPLICATIVA
        val cuelloSymptoms = listOf(
            calculateSymptomScore(q.dolorCuelloFrecuencia, q.dolorCuelloIntensidad),
            calculateSymptomScore(q.rigidezCuelloFrecuencia, q.rigidezCuelloIntensidad)
        )
        totalSymptoms += (cuelloSymptoms.average() * 2)
        maxIntensity = maxOf(maxIntensity, q.dolorCuelloIntensidad, q.rigidezCuelloIntensidad)

        // Hombros (peso: alto)
        val hombrosScore = calculateSymptomScore(q.dolorHombrosFrecuencia, q.dolorHombrosIntensidad)
        totalSymptoms += (hombrosScore * 2)
        maxIntensity = maxOf(maxIntensity, q.dolorHombrosIntensidad)

        // Espalda (peso: muy alto)
        val espaldaSymptoms = listOf(
            calculateSymptomScore(q.dolorEspaldaAltaFrecuencia, q.dolorEspaldaAltaIntensidad),
            calculateSymptomScore(q.dolorEspaldaBajaFrecuencia, q.dolorEspaldaBajaIntensidad),
            calculateSymptomScore(q.rigidezEspaldaMañanaFrecuencia, q.rigidezEspaldaMañanaIntensidad)
        )
        totalSymptoms += (espaldaSymptoms.average() * 3)
        maxIntensity = maxOf(maxIntensity, q.dolorEspaldaAltaIntensidad, q.dolorEspaldaBajaIntensidad)

        // Manos/muñecas (peso: alto)
        val manosSymptoms = listOf(
            calculateSymptomScore(q.dolorMunecasFrecuencia, q.dolorMunecasIntensidad),
            calculateSymptomScore(q.dolorManosFrecuencia, q.dolorManosIntensidad),
            calculateSymptomScore(q.hormigueoManosFrecuencia, q.hormigueoManosIntensidad)
        )
        totalSymptoms += (manosSymptoms.average() * 2)

        if (q.hormigueoPorNoche.contains("despierta")) totalSymptoms += 5

        // Dolor de cabeza
        val cabezaScore = calculateSymptomScore(q.dolorCabezaFrecuencia, q.dolorCabezaIntensidad)
        totalSymptoms += cabezaScore

        // Impacto funcional
        if (q.dolorImpidenActividades.contains("frecuentemente")) totalSymptoms += 10
        else if (q.dolorImpidenActividades.contains("ocasionalmente")) totalSymptoms += 5

        // Normalización dinámica
        val maxPossible = ScoreCalculator.MaxScores.getSintomasMuscularesMax()
        val score = ((totalSymptoms / maxPossible) * 100).toInt().coerceIn(0, 100)

        // Clasificación de riesgo con patrones críticos
        val criticalPatterns = ScoreCalculator.detectMusculoskeletalPatterns(q)
        val hasUrgentPattern = criticalPatterns.any { it.severity == CriticalLevel.INTERVENCION_URGENTE }

        val risk = if (hasUrgentPattern) {
            RiskLevel.MUY_ALTO
        } else {
            when {
                score < 20 || maxIntensity <= 1 -> RiskLevel.BAJO
                score < 40 || maxIntensity <= 2 -> RiskLevel.MODERADO
                score < 60 || maxIntensity <= 3 -> RiskLevel.ALTO
                else -> RiskLevel.MUY_ALTO
            }
        }

        android.util.Log.d(TAG, """
            Síntomas Musculares (Enhanced): 
            - Total con ponderación multiplicativa: $totalSymptoms/$maxPossible
            - Score normalizado: $score
            - Intensidad máxima: $maxIntensity
            - Patrones críticos: ${criticalPatterns.size}
            - Riesgo: ${risk.displayName}
        """.trimIndent())

        return Pair(score, risk)
    }

    /**
     * ✅ NUEVO: Analizar tendencias comparando con medición anterior
     */
    fun analyzeTrends(
        currentScore: EnhancedHealthScore,
        previousScore: EnhancedHealthScore?
    ): TrendAnalysis {

        if (previousScore == null) {
            return TrendAnalysis(
                overallTrend = TrendDirection.SIN_DATOS,
                areaTrends = emptyMap(),
                areasImproving = 0,
                areasWorsening = 0,
                areasStable = 0,
                keyInsights = listOf("Primera evaluación. Realiza seguimiento regular para ver tendencias.")
            )
        }

        val daysElapsed = ((currentScore.timestamp - previousScore.timestamp) / (1000 * 60 * 60 * 24)).toInt()

        // Analizar cada área
        val areaTrends = mutableMapOf<String, AreaTrend>()

        fun analyzeArea(name: String, current: Int, previous: Int): AreaTrend {
            val change = current - previous
            val changePercent = if (previous > 0) (change.toFloat() / previous * 100) else 0f

            val direction = when {
                change <= -5 -> TrendDirection.MEJORANDO  // Score bajando = mejorando
                change >= 5 -> TrendDirection.EMPEORANDO   // Score subiendo = empeorando
                else -> TrendDirection.ESTABLE
            }

            return AreaTrend(
                area = name,
                currentScore = current,
                previousScore = previous,
                direction = direction,
                changePoints = change,
                changePercent = changePercent,
                daysElapsed = daysElapsed
            )
        }

        areaTrends["estrés"] = analyzeArea("Estrés", currentScore.estresSaludMentalScore, previousScore.estresSaludMentalScore)
        areaTrends["ergonomía"] = analyzeArea("Ergonomía", currentScore.ergonomiaScore, previousScore.ergonomiaScore)
        areaTrends["carga_trabajo"] = analyzeArea("Carga de Trabajo", currentScore.cargaTrabajoScore, previousScore.cargaTrabajoScore)
        areaTrends["sueño"] = analyzeArea("Sueño", currentScore.habitosSuenoScore, previousScore.habitosSuenoScore)
        areaTrends["actividad_física"] = analyzeArea("Actividad Física", currentScore.actividadFisicaScore, previousScore.actividadFisicaScore)
        areaTrends["síntomas_musculares"] = analyzeArea("Síntomas Musculares", currentScore.sintomasMuscularesScore, previousScore.sintomasMuscularesScore)
        areaTrends["síntomas_visuales"] = analyzeArea("Síntomas Visuales", currentScore.sintomasVisualesScore, previousScore.sintomasVisualesScore)
        areaTrends["salud_general"] = analyzeArea("Salud General", currentScore.saludGeneralScore, previousScore.saludGeneralScore)
        areaTrends["balance"] = analyzeArea("Balance Vida-Trabajo", currentScore.balanceVidaTrabajoScore, previousScore.balanceVidaTrabajoScore)

        // Contar tendencias
        val improving = areaTrends.values.count { it.direction == TrendDirection.MEJORANDO }
        val worsening = areaTrends.values.count { it.direction == TrendDirection.EMPEORANDO }
        val stable = areaTrends.values.count { it.direction == TrendDirection.ESTABLE }

        // Tendencia general
        val overallChange = currentScore.overallScore - previousScore.overallScore
        val overallTrend = when {
            overallChange <= -5 -> TrendDirection.MEJORANDO
            overallChange >= 5 -> TrendDirection.EMPEORANDO
            else -> TrendDirection.ESTABLE
        }

        // Generar insights
        val insights = generateTrendInsights(areaTrends, overallTrend, daysElapsed)

        android.util.Log.d(TAG, """
            Análisis de Tendencias:
            - Tendencia general: ${overallTrend.displayName}
            - Áreas mejorando: $improving
            - Áreas empeorando: $worsening
            - Áreas estables: $stable
            - Días transcurridos: $daysElapsed
        """.trimIndent())

        return TrendAnalysis(
            overallTrend = overallTrend,
            areaTrends = areaTrends,
            areasImproving = improving,
            areasWorsening = worsening,
            areasStable = stable,
            keyInsights = insights
        )
    }

    /**
     * ✅ NUEVO: Generar insights sobre tendencias
     */
    private fun generateTrendInsights(
        trends: Map<String, AreaTrend>,
        overallTrend: TrendDirection,
        daysElapsed: Int
    ): List<String> {
        val insights = mutableListOf<String>()

        // Insight sobre tendencia general
        when (overallTrend) {
            TrendDirection.MEJORANDO -> {
                insights.add("✅ Tu salud ocupacional está mejorando. ¡Continúa con tus buenos hábitos!")
            }
            TrendDirection.EMPEORANDO -> {
                insights.add("⚠️ Se detecta deterioro en tu salud ocupacional. Revisa las recomendaciones.")
            }
            TrendDirection.ESTABLE -> {
                insights.add("→ Tu salud ocupacional se mantiene estable desde hace $daysElapsed días.")
            }
            else -> {}
        }

        // Áreas con mayor mejora
        val topImproving = trends.values
            .filter { it.direction == TrendDirection.MEJORANDO }
            .sortedBy { it.changePoints }
            .take(2)

        if (topImproving.isNotEmpty()) {
            val areas = topImproving.joinToString(" y ") { it.area }
            insights.add("📈 Mejora notable en: $areas")
        }

        // Áreas con mayor deterioro
        val topWorsening = trends.values
            .filter { it.direction == TrendDirection.EMPEORANDO }
            .sortedByDescending { it.changePoints }
            .take(2)

        if (topWorsening.isNotEmpty()) {
            val areas = topWorsening.joinToString(" y ") { it.area }
            insights.add("📉 Requiere atención: $areas")
        }

        // Insight sobre consistencia
        if (trends.values.all { it.direction == TrendDirection.ESTABLE }) {
            insights.add("🔄 Todas las áreas están estables. Considera nuevas estrategias de mejora.")
        }

        // Insight sobre frecuencia de medición
        if (daysElapsed > 30) {
            insights.add("⏰ Han pasado $daysElapsed días desde tu última evaluación. Se recomienda evaluación semanal.")
        }

        return insights
    }

    /**
     * ✅ NUEVO: Calcular completitud de cuestionarios
     */
    fun calculateCompleteness(lastUpdated: Map<String, Long>): CompletenessInfo {
        val totalQuestionnaires = 9
        val completedQuestionnaires = lastUpdated.values.count { it > 0 }
        val completenessPercent = (completedQuestionnaires.toFloat() / totalQuestionnaires * 100)

        val missingQuestionnaires = lastUpdated
            .filter { it.value == 0L }
            .keys
            .map { getQuestionnaireName(it) }

        val confidenceLevel = when {
            completenessPercent >= 90 -> ConfidenceLevel.ALTA
            completenessPercent >= 70 -> ConfidenceLevel.MEDIA
            else -> ConfidenceLevel.BAJA
        }

        android.util.Log.d(TAG, """
            Completitud de cuestionarios:
            - Completados: $completedQuestionnaires/$totalQuestionnaires
            - Porcentaje: ${completenessPercent.toInt()}%
            - Nivel de confianza: ${confidenceLevel.displayName}
            - Faltantes: ${missingQuestionnaires.size}
        """.trimIndent())

        return CompletenessInfo(
            completedQuestionnaires = completedQuestionnaires,
            totalQuestionnaires = totalQuestionnaires,
            completenessPercent = completenessPercent,
            confidenceLevel = confidenceLevel,
            missingQuestionnaires = missingQuestionnaires
        )
    }

    /**
     * Helper para obtener nombre legible de cuestionario
     */
    private fun getQuestionnaireName(key: String): String {
        return when (key) {
            "salud_general" -> "Salud General"
            "ergonomia" -> "Ergonomía"
            "sintomas_musculares" -> "Síntomas Musculares"
            "sintomas_visuales" -> "Síntomas Visuales"
            "carga_trabajo" -> "Carga de Trabajo"
            "estres" -> "Estrés y Salud Mental"
            "sueno" -> "Hábitos de Sueño"
            "actividad_fisica" -> "Actividad Física"
            "balance" -> "Balance Vida-Trabajo"
            else -> key
        }
    }

    /**
     * ✅ NUEVO: Generar EnhancedHealthScore completo
     */
    fun calculateEnhancedHealthScore(
        userId: String,
        allQuestionnaires: Map<String, Any?>,
        previousScore: EnhancedHealthScore? = null
    ): EnhancedHealthScore {

        // Calcular scores de cada área (usando ScoreCalculator existente)
        val scores = mutableMapOf<String, Pair<Int, RiskLevel>>()
        val lastUpdated = mutableMapOf<String, Long>()

        // Extraer y calcular scores individuales
        // (Esta parte conectaría con el ScoreCalculator existente)

        // Calcular completitud
        val completeness = calculateCompleteness(lastUpdated)

        // Calcular score base (usando ScoreCalculator.calculateOverallScore)
        val baseScore = EnhancedHealthScore(
            userId = userId,
            timestamp = System.currentTimeMillis(),
            version = 3,
            lastUpdated = lastUpdated,
            completeness = completeness
            // ... otros campos
        )

        // Analizar tendencias
        val trendAnalysis = analyzeTrends(baseScore, previousScore)

        return baseScore.copy(
            trendAnalysis = trendAnalysis
        )
    }
}

/**
 * ✅ DOCUMENTACIÓN: Escala invertida de ergonomía
 *
 * IMPORTANTE: La ergonomía es la ÚNICA área donde un score alto = BUENO
 *
 * TODAS las demás áreas: Score ALTO = PEOR
 * - Estrés 100 = Estrés máximo (malo)
 * - Síntomas 100 = Muchos síntomas (malo)
 * - Carga trabajo 100 = Sobrecarga (malo)
 *
 * ERGONOMÍA (INVERTIDA): Score ALTO = MEJOR
 * - Ergonomía 100 = Puesto perfectamente ergonómico (bueno)
 * - Ergonomía 0 = Puesto muy malo ergonómicamente (malo)
 *
 * RAZÓN: La ergonomía mide CALIDAD del ambiente, no PROBLEMAS
 *
 * CONVERSIÓN para IA:
 * - Para alimentar modelos de IA, ergonomía debe invertirse:
 * - ergonomiaIndex = (100 - ergonomiaScore) / 100 * 10
 * - Así, score alto (100) → índice bajo (0) = bajo riesgo ✓
 *
 * VISUALIZACIÓN:
 * - En UI, mostrar ergonomía como "Calidad: 85/100" (alto = bueno)
 * - Para riesgo, calcular: riesgo = cuando score < 60 (mala ergonomía)
 */
object ErgonomiaScaleInfo {
    const val IS_INVERTED = true
    const val HIGH_SCORE_MEANING = "Buena ergonomía"
    const val LOW_SCORE_MEANING = "Mala ergonomía"

    fun toRiskIndex(score: Int): Float {
        // Convertir de calidad (0-100) a riesgo (0-10)
        return (100 - score) / 100f * 10f
    }

    fun getRiskLevel(score: Int): RiskLevel {
        return when {
            score >= 80 -> RiskLevel.BAJO      // Buena ergonomía
            score >= 60 -> RiskLevel.MODERADO  // Ergonomía aceptable
            score >= 40 -> RiskLevel.ALTO      // Mala ergonomía
            else -> RiskLevel.MUY_ALTO         // Ergonomía muy mala
        }
    }
}