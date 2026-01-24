package com.example.uleammed.burnoutprediction.model

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