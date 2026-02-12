package com.example.uleammed.burnoutprediction.model

import android.content.Context
import com.example.uleammed.scoring.CriticalPattern
import com.example.uleammed.scoring.CriticalLevel
import com.example.uleammed.scoring.HealthScore
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * ✅ MODELO DE PREDICCIÓN DE BURNOUT MEJORADO
 *
 * Integra el sistema de scoring mejorado con el modelo de IA
 */
class BurnoutPredictionModel(private val context: Context) {

    companion object {
        private const val MODEL_PATH = "burnout_model.tflite"
        private const val TAG = "BurnoutPredictionModel"

        // Parámetros del StandardScaler (del entrenamiento)
        private val FEATURE_MEAN = floatArrayOf(
            5.707162f,  // estres_index
            4.740204f,  // ergonomia_index
            5.836906f,  // carga_trabajo_index
            5.190881f,  // calidad_sueno_index
            5.356390f,  // actividad_fisica_index
            4.853331f,  // sintomas_musculares_index
            4.398310f,  // sintomas_visuales_index
            4.865175f   // salud_general_index
        )

        private val FEATURE_STD = floatArrayOf(
            2.719379f,  // estres_index
            2.408723f,  // ergonomia_index
            2.206976f,  // carga_trabajo_index
            2.714523f,  // calidad_sueno_index
            2.276300f,  // actividad_fisica_index
            2.564424f,  // sintomas_musculares_index
            2.551944f,  // sintomas_visuales_index
            2.204002f   // salud_general_index
        )
    }

    private var interpreter: Interpreter? = null

    init {
        loadModel()
    }

    private fun loadModel() {
        try {
            val modelFile = FileUtil.loadMappedFile(context, MODEL_PATH)
            val options = Interpreter.Options().apply {
                setNumThreads(4)
                setUseNNAPI(true)
            }
            interpreter = Interpreter(modelFile, options)
            android.util.Log.d(TAG, "✅ Modelo cargado exitosamente")
        } catch (e: Exception) {
            android.util.Log.e(TAG, "❌ Error cargando modelo", e)
            throw RuntimeException("Error cargando modelo de IA", e)
        }
    }

    /**
     * ✅ NUEVO: Convierte HealthScore a array de índices para el modelo
     */
    fun healthScoreToIndices(healthScore: HealthScore): FloatArray {
        return floatArrayOf(
            healthScore.estresSaludMentalScore / 100f * 10f,
            (100 - healthScore.ergonomiaScore) / 100f * 10f,  // INVERTIDO
            healthScore.cargaTrabajoScore / 100f * 10f,
            healthScore.habitosSuenoScore / 100f * 10f,
            healthScore.actividadFisicaScore / 100f * 10f,
            healthScore.sintomasMuscularesScore / 100f * 10f,
            healthScore.sintomasVisualesScore / 100f * 10f,
            healthScore.saludGeneralScore / 100f * 10f
        )
    }

    /**
     * ✅ NUEVO: Predicción mejorada desde HealthScore
     */
    suspend fun predictFromHealthScore(healthScore: HealthScore): EnhancedBurnoutPrediction =
        withContext(Dispatchers.Default) {
            val interpreter = this@BurnoutPredictionModel.interpreter
                ?: throw IllegalStateException("Modelo no inicializado")

            // 1. Convertir a índices
            val features = healthScoreToIndices(healthScore)

            // 2. Normalizar
            val normalizedFeatures = normalizeFeatures(features)

            // 3. Preparar input para TFLite
            val inputArray = Array(1) { normalizedFeatures }
            val outputArray = Array(1) { FloatArray(3) }

            // 4. Ejecutar modelo
            interpreter.run(inputArray, outputArray)
            val probabilities = outputArray[0]

            // 5. Clasificar según probabilidades
            val predictedClass = probabilities.indices.maxByOrNull { probabilities[it] } ?: 1
            val baseRiskLevel = when (predictedClass) {
                0 -> NivelRiesgoBurnout.BAJO
                1 -> NivelRiesgoBurnout.MEDIO
                else -> NivelRiesgoBurnout.ALTO
            }

            val baseConfidence = probabilities[predictedClass]

            // 6. Analizar patrones críticos
            val urgentPatterns = healthScore.criticalPatterns.filter {
                it.severity == CriticalLevel.INTERVENCION_URGENTE
            }
            val criticalPatterns = healthScore.criticalPatterns.filter {
                it.severity == CriticalLevel.ATENCION_REQUERIDA
            }

            // 7. Ajustar predicción según patrones
            val adjustedRiskLevel = when {
                urgentPatterns.isNotEmpty() -> NivelRiesgoBurnout.ALTO
                criticalPatterns.isNotEmpty() && probabilities[2] > 0.3f -> NivelRiesgoBurnout.ALTO
                else -> baseRiskLevel
            }

            // 8. Ajustar confianza
            val hasCriticalEvidence = healthScore.criticalPatterns.isNotEmpty()
            val adjustedConfidence = if (hasCriticalEvidence) {
                (baseConfidence * 1.2f).coerceAtMost(1.0f)
            } else {
                baseConfidence
            }

            // 9. Generar factores de riesgo
            val riskFactors = identifyEnhancedRiskFactors(features, healthScore)

            // 10. Generar recomendaciones
            val recommendations = generatePrioritizedRecommendations(
                adjustedRiskLevel,
                healthScore.criticalPatterns,
                riskFactors
            )

            EnhancedBurnoutPrediction(
                probabilidadBajo = probabilities[0],
                probabilidadMedio = probabilities[1],
                probabilidadAlto = probabilities[2],
                nivelRiesgo = adjustedRiskLevel,
                confianza = adjustedConfidence,
                factoresRiesgo = riskFactors,
                recomendaciones = recommendations,
                criticalPatterns = healthScore.criticalPatterns,
                hasCriticalPatterns = hasCriticalEvidence,
                requiresUrgentAttention = urgentPatterns.isNotEmpty(),
                scoringVersion = healthScore.version
            )
        }

    /**
     * Método original para retrocompatibilidad
     */
    suspend fun predict(data: QuestionnaireData): BurnoutPrediction =
        withContext(Dispatchers.Default) {
            val interpreter = this@BurnoutPredictionModel.interpreter
                ?: throw IllegalStateException("Modelo no inicializado")

            val features = floatArrayOf(
                data.estresIndex,
                data.ergonomiaIndex,
                data.cargaTrabajoIndex,
                data.calidadSuenoIndex,
                data.actividadFisicaIndex,
                data.sintomasMuscularesIndex,
                data.sintomasVisualesIndex,
                data.saludGeneralIndex
            )

            val normalizedFeatures = normalizeFeatures(features)
            val inputArray = Array(1) { normalizedFeatures }
            val outputArray = Array(1) { FloatArray(3) }

            interpreter.run(inputArray, outputArray)
            val probabilities = outputArray[0]

            BurnoutPrediction(
                probabilidadBajo = probabilities[0],
                probabilidadMedio = probabilities[1],
                probabilidadAlto = probabilities[2]
            )
        }

    private fun normalizeFeatures(features: FloatArray): FloatArray {
        return FloatArray(8) { i ->
            (features[i] - FEATURE_MEAN[i]) / FEATURE_STD[i]
        }
    }

    private fun identifyEnhancedRiskFactors(
        features: FloatArray,
        healthScore: HealthScore
    ): List<BurnoutRiskFactor> {
        val factors = mutableListOf<BurnoutRiskFactor>()

        val areas = listOf(
            Triple("Estrés y Salud Mental", features[0], healthScore.estresSaludMentalScore),
            Triple("Ergonomía", features[1], 100 - healthScore.ergonomiaScore),
            Triple("Carga de Trabajo", features[2], healthScore.cargaTrabajoScore),
            Triple("Calidad de Sueño", features[3], healthScore.habitosSuenoScore),
            Triple("Actividad Física", features[4], healthScore.actividadFisicaScore),
            Triple("Síntomas Musculares", features[5], healthScore.sintomasMuscularesScore),
            Triple("Síntomas Visuales", features[6], healthScore.sintomasVisualesScore),
            Triple("Salud General", features[7], healthScore.saludGeneralScore)
        )

        areas.forEach { (area, index, score) ->
            val severity = when {
                index > 8.0f -> BurnoutRiskSeverity.MUY_ALTO
                index > 6.5f -> BurnoutRiskSeverity.ALTO
                index > 4.5f -> BurnoutRiskSeverity.MODERADO
                else -> BurnoutRiskSeverity.BAJO
            }

            if (severity != BurnoutRiskSeverity.BAJO) {
                factors.add(
                    BurnoutRiskFactor(
                        area = area,
                        severity = severity,
                        score = score,
                        index = index,
                        description = getRiskDescription(area, severity)
                    )
                )
            }
        }

        return factors.sortedByDescending { it.index }
    }

    private fun getRiskDescription(area: String, severity: BurnoutRiskSeverity): String {
        return when (severity) {
            BurnoutRiskSeverity.MUY_ALTO -> "$area presenta niveles críticos que requieren atención inmediata"
            BurnoutRiskSeverity.ALTO -> "$area muestra indicadores elevados de riesgo"
            BurnoutRiskSeverity.MODERADO -> "$area requiere monitoreo y mejoras"
            BurnoutRiskSeverity.BAJO -> "$area está en niveles aceptables"
        }
    }

    private fun generatePrioritizedRecommendations(
        riskLevel: NivelRiesgoBurnout,
        patterns: List<CriticalPattern>,
        factors: List<BurnoutRiskFactor>
    ): List<String> {
        val recommendations = mutableListOf<String>()

        // 1. Recomendaciones urgentes de patrones
        patterns.filter { it.severity == CriticalLevel.INTERVENCION_URGENTE }
            .forEach { recommendations.add("⚠️ URGENTE: ${it.recommendation}") }

        // 2. Recomendaciones importantes de patrones
        patterns.filter { it.severity == CriticalLevel.ATENCION_REQUERIDA }
            .forEach { recommendations.add("📌 IMPORTANTE: ${it.recommendation}") }

        // 3. Recomendaciones basadas en IA
        when (riskLevel) {
            NivelRiesgoBurnout.ALTO -> {
                recommendations.add("🎯 Considera consultar con un profesional de salud mental")
                recommendations.add("🎯 Evalúa reducir carga de trabajo o tomar descanso")
            }
            NivelRiesgoBurnout.MEDIO -> {
                recommendations.add("🎯 Implementa técnicas de gestión de estrés")
                recommendations.add("🎯 Mejora tus hábitos de sueño y actividad física")
            }
            else -> {
                recommendations.add("💡 Mantén tus buenos hábitos de autocuidado")
            }
        }

        // 4. Recomendaciones por factores específicos
        factors.take(3).forEach { factor ->
            if (factor.severity == BurnoutRiskSeverity.MUY_ALTO ||
                factor.severity == BurnoutRiskSeverity.ALTO) {
                recommendations.add("📋 ${factor.area}: ${getAreaRecommendation(factor.area)}")
            }
        }

        // 5. Alertas tempranas
        patterns.filter { it.severity == CriticalLevel.ALERTA_TEMPRANA }
            .take(2)
            .forEach { recommendations.add("💡 ${it.recommendation}") }

        return recommendations.take(7)
    }

    private fun getAreaRecommendation(area: String): String {
        return when {
            area.contains("Estrés") -> "Practica técnicas de relajación diarias"
            area.contains("Ergonomía") -> "Mejora tu puesto de trabajo"
            area.contains("Carga") -> "Prioriza tareas y delega cuando sea posible"
            area.contains("Sueño") -> "Establece horario regular de sueño"
            area.contains("Actividad") -> "Incorpora ejercicio regular"
            area.contains("Musculares") -> "Realiza pausas activas cada hora"
            area.contains("Visuales") -> "Aplica la regla 20-20-20"
            else -> "Monitorea este indicador regularmente"
        }
    }

    fun close() {
        interpreter?.close()
        interpreter = null
    }
}