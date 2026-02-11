package com.example.uleammed.burnoutprediction.model

import com.example.uleammed.scoring.CriticalPattern
// ⚠️ IMPORTANTE: Importar RiskFactor y RiskSeverity desde donde YA estén definidos
// Si tienes estas clases en otro paquete, importa desde ahí:
// import com.example.uleammed.scoring.RiskFactor
// import com.example.uleammed.scoring.RiskSeverity

/**
 * Predicción de burnout estándar
 *
 * Esta es la clase original que devuelve el modelo de IA básico.
 * Contiene las probabilidades de cada nivel de riesgo.
 */
data class BurnoutPrediction(
    val probabilidadBajo: Float,
    val probabilidadMedio: Float,
    val probabilidadAlto: Float
) {
    /**
     * Nivel de riesgo calculado basado en la probabilidad más alta
     */
    val nivelRiesgo: NivelRiesgoBurnout
        get() = when {
            probabilidadAlto >= probabilidadMedio && probabilidadAlto >= probabilidadBajo ->
                NivelRiesgoBurnout.ALTO
            probabilidadMedio >= probabilidadBajo ->
                NivelRiesgoBurnout.MEDIO
            else ->
                NivelRiesgoBurnout.BAJO
        }

    /**
     * Confianza de la predicción (la probabilidad más alta)
     */
    val confianza: Float
        get() = maxOf(probabilidadBajo, probabilidadMedio, probabilidadAlto)
}

// ==================== CLASES MEJORADAS ====================

/**
 * ✅ NUEVA: Predicción mejorada con integración de patrones críticos
 *
 * Esta versión mejorada integra:
 * - Patrones críticos detectados por el sistema de scoring
 * - Factores de riesgo detallados por área
 * - Recomendaciones priorizadas
 * - Indicadores de urgencia
 *
 * ⚠️ NOTA: RiskFactor debe estar definido en otro archivo
 * Si no existe, descomenta las clases al final de este archivo.
 */
data class EnhancedBurnoutPrediction(
    // Probabilidades del modelo IA
    val probabilidadBajo: Float,
    val probabilidadMedio: Float,
    val probabilidadAlto: Float,

    // Clasificación de riesgo (puede ser ajustada por patrones)
    val nivelRiesgo: NivelRiesgoBurnout,

    // Confianza de la predicción
    val confianza: Float,

    // Factores de riesgo identificados
    val factoresRiesgo: List<BurnoutRiskFactor>, // ⚠️ CAMBIADO para evitar conflicto

    // Recomendaciones priorizadas
    val recomendaciones: List<String>,

    // ✅ NUEVO: Patrones críticos detectados
    val criticalPatterns: List<CriticalPattern>,

    // ✅ NUEVO: Indicadores de estado
    val hasCriticalPatterns: Boolean,
    val requiresUrgentAttention: Boolean,

    // Versión del sistema de scoring usado
    val scoringVersion: Int
)

// ==================== CLASES PROPIAS DE BURNOUT ====================

/**
 * ✅ Factor de riesgo específico para burnout
 * RENOMBRADO a BurnoutRiskFactor para evitar conflicto con otras clases RiskFactor
 */
data class BurnoutRiskFactor(
    val area: String,              // Ej: "Estrés", "Carga de Trabajo"
    val severity: BurnoutRiskSeverity,    // Nivel de severidad
    val score: Int,                // Score original 0-100
    val index: Float,              // Índice normalizado 0-10
    val description: String        // Descripción del factor
)

/**
 * ✅ Severidad de factor de riesgo para burnout
 * RENOMBRADO a BurnoutRiskSeverity para evitar conflicto
 */
enum class BurnoutRiskSeverity {
    BAJO,       // Score 0-25: Factor bajo impacto
    MODERADO,   // Score 26-50: Requiere atención
    ALTO,       // Score 51-75: Factor importante
    MUY_ALTO;   // Score 76-100: Factor crítico

    val displayName: String
        get() = when (this) {
            BAJO -> "Bajo"
            MODERADO -> "Moderado"
            ALTO -> "Alto"
            MUY_ALTO -> "Muy Alto"
        }

    /**
     * Determina la severidad basándose en un score de 0-100
     */
    companion object {
        fun fromScore(score: Int): BurnoutRiskSeverity {
            return when {
                score <= 25 -> BAJO
                score <= 50 -> MODERADO
                score <= 75 -> ALTO
                else -> MUY_ALTO
            }
        }
    }
}