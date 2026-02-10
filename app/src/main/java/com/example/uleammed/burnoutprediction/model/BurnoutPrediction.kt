package com.example.uleammed.burnoutprediction.model


import com.example.uleammed.scoring.CriticalPattern

data class BurnoutPrediction(
    val probabilidadBajo: Float,
    val probabilidadMedio: Float,
    val probabilidadAlto: Float
) {
    val nivelRiesgo: NivelRiesgoBurnout
        get() = when {
            probabilidadAlto >= probabilidadMedio && probabilidadAlto >= probabilidadBajo ->
                NivelRiesgoBurnout.ALTO
            probabilidadMedio >= probabilidadBajo ->
                NivelRiesgoBurnout.MEDIO
            else ->
                NivelRiesgoBurnout.BAJO
        }

    val confianza: Float
        get() = maxOf(probabilidadBajo, probabilidadMedio, probabilidadAlto)
}
// ✅ AGREGAR AL FINAL DEL ARCHIVO

data class EnhancedBurnoutPrediction(
    val probabilidadBajo: Float,
    val probabilidadMedio: Float,
    val probabilidadAlto: Float,
    val nivelRiesgo: NivelRiesgoBurnout,
    val confianza: Float,
    val factoresRiesgo: List<RiskFactor>,
    val recomendaciones: List<String>,
    val criticalPatterns: List<com.example.uleammed.scoring.CriticalPattern>,
    val hasCriticalPatterns: Boolean,
    val requiresUrgentAttention: Boolean,
    val scoringVersion: Int
)

data class RiskFactor(
    val area: String,
    val severity: RiskSeverity,
    val score: Int,
    val index: Float,
    val description: String
)

enum class RiskSeverity {
    BAJO, MODERADO, ALTO, MUY_ALTO
}