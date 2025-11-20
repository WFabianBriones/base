package com.example.uleammed

/**
 * Modelo de datos para entrada del modelo de predicción de burnout
 */
data class BurnoutInputData(
    // ERGONOMÍA (22 variables) - Normalizado 0-1
    val scoreErgonomia: Float,
    val calidadSilla: Float,
    val calidadMonitor: Float,
    val calidadIluminacion: Float,
    val frecuenciaPausas: Float,

    // SÍNTOMAS MÚSCULO-ESQUELÉTICOS (28 variables)
    val dolorCuello: Float,
    val dolorEspalda: Float,
    val dolorHombros: Float,
    val dolorMunecas: Float,
    val rigidezMuscular: Float,
    val limitacionMovimiento: Float,

    // SÍNTOMAS VISUALES (16 variables)
    val fatigaVisual: Float,
    val ojosSecos: Float,
    val visionBorrosa: Float,
    val sensibilidadLuz: Float,

    // CARGA DE TRABAJO (15 variables)
    val cargaTrabajo: Float,
    val presionTiempo: Float,
    val horasExtra: Float,
    val trabajoFinSemana: Float,
    val autonomia: Float,
    val apoyoSocial: Float,
    val satisfaccionLaboral: Float,

    // ESTRÉS Y SALUD MENTAL (19 variables) - MÁS IMPORTANTE
    val nivelEstres: Float,
    val fatigaEmocional: Float,
    val dificultadConcentracion: Float,
    val irritabilidad: Float,
    val ansiedad: Float,
    val perdidaMotivacion: Float,
    val sensacionAbrumado: Float,
    val despersonalizacion: Float,
    val agotamientoEmocional: Float,

    // HÁBITOS DE SUEÑO (9 variables)
    val calidadSueno: Float,
    val horasSueno: Float,
    val dificultadDormir: Float,

    // ACTIVIDAD FÍSICA (10 variables)
    val frecuenciaEjercicio: Float,
    val nivelActividad: Float,

    // BALANCE VIDA-TRABAJO (8 variables)
    val equilibrioVidaTrabajo: Float,
    val tiempoLibre: Float,
    val capacidadDesconectar: Float,

    // DEMOGRÁFICAS
    val edad: Float, // normalizado
    val aniosExperiencia: Float, // normalizado
    val genero: Float // 0=F, 1=M, 0.5=Otro
) {
    /**
     * Convierte a array de floats para entrada del modelo
     */
    fun toFloatArray(): FloatArray = floatArrayOf(
        scoreErgonomia, calidadSilla, calidadMonitor, calidadIluminacion, frecuenciaPausas,
        dolorCuello, dolorEspalda, dolorHombros, dolorMunecas, rigidezMuscular, limitacionMovimiento,
        fatigaVisual, ojosSecos, visionBorrosa, sensibilidadLuz,
        cargaTrabajo, presionTiempo, horasExtra, trabajoFinSemana, autonomia, apoyoSocial, satisfaccionLaboral,
        nivelEstres, fatigaEmocional, dificultadConcentracion, irritabilidad, ansiedad,
        perdidaMotivacion, sensacionAbrumado, despersonalizacion, agotamientoEmocional,
        calidadSueno, horasSueno, dificultadDormir,
        frecuenciaEjercicio, nivelActividad,
        equilibrioVidaTrabajo, tiempoLibre, capacidadDesconectar,
        edad, aniosExperiencia, genero
    )

    companion object {
        const val INPUT_SIZE = 42 // Total de características
    }
}

/**
 * Resultado de la predicción de burnout
 */
data class BurnoutPrediction(
    val nivelRiesgo: NivelRiesgo,
    val probabilidadBurnout: Float, // 0.0 - 1.0
    val probabilidadBajo: Float,
    val probabilidadModerado: Float,
    val probabilidadAlto: Float,
    val factoresRiesgo: List<FactorRiesgo>,
    val recomendaciones: List<Recomendacion>,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Niveles de riesgo de burnout
 */
enum class NivelRiesgo(val displayName: String, val color: Long, val descripcion: String) {
    BAJO(
        "Riesgo Bajo",
        0xFF4CAF50, // Verde
        "Tu nivel de estrés está controlado. Mantén tus hábitos saludables."
    ),
    MODERADO(
        "Riesgo Moderado",
        0xFFFFA726, // Naranja
        "Algunos factores de riesgo están presentes. Es momento de tomar medidas preventivas."
    ),
    ALTO(
        "Riesgo Alto",
        0xFFF44336, // Rojo
        "Presentas varios indicadores de burnout. Considera buscar apoyo profesional."
    ),
    CRITICO(
        "Riesgo Crítico",
        0xFF9C27B0, // Morado
        "⚠️ Nivel crítico detectado. Busca ayuda profesional urgentemente."
    )
}

/**
 * Factor de riesgo identificado
 */
data class FactorRiesgo(
    val nombre: String,
    val categoria: CategoriaFactor,
    val valor: Float, // 0.0 - 1.0
    val impacto: ImpactoFactor,
    val descripcion: String
)

enum class CategoriaFactor {
    ERGONOMIA,
    SINTOMAS_FISICOS,
    SINTOMAS_VISUALES,
    CARGA_TRABAJO,
    ESTRES_MENTAL,
    SUENO,
    ACTIVIDAD_FISICA,
    BALANCE_VIDA
}

enum class ImpactoFactor(val displayName: String, val color: Long) {
    CRITICO("Crítico", 0xFFF44336),
    ALTO("Alto", 0xFFFF9800),
    MODERADO("Moderado", 0xFFFFC107),
    BAJO("Bajo", 0xFF4CAF50)
}

/**
 * Recomendación personalizada
 */
data class Recomendacion(
    val titulo: String,
    val descripcion: String,
    val prioridad: PrioridadRecomendacion,
    val categoria: CategoriaFactor,
    val accionesConcretas: List<String>
)

enum class PrioridadRecomendacion {
    URGENTE,
    ALTA,
    MEDIA,
    BAJA
}

/**
 * Historial de predicciones para seguimiento
 */
data class BurnoutHistory(
    val userId: String,
    val predicciones: List<BurnoutPrediction>,
    val tendencia: TendenciaBurnout
)

enum class TendenciaBurnout {
    MEJORANDO,
    ESTABLE,
    EMPEORANDO
}