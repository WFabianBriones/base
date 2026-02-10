package com.example.uleammed.burnoutprediction.model

enum class NivelRiesgoBurnout {
    BAJO,
    MEDIO,
    ALTO;

    val displayName: String
        get() = when (this) {
            BAJO -> "Riesgo Bajo"
            MEDIO -> "Riesgo Medio"
            ALTO -> "Riesgo Alto"
        }

    val color: Long
        get() = when (this) {
            BAJO -> 0xFF4CAF50
            MEDIO -> 0xFFFFC107
            ALTO -> 0xFFF44336
        }
}

// ✅ AGREGAR ESTA LÍNEA
/**
 * Alias para compatibilidad con código mejorado
 */
typealias BurnoutRiskLevel = NivelRiesgoBurnout