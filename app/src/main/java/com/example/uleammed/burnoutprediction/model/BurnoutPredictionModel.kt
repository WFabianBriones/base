package com.example.uleammed.burnoutprediction.model

import android.content.Context
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.example.uleammed.scoring.HealthScore
import com.example.uleammed.scoring.CriticalPattern
import com.example.uleammed.scoring.CriticalLevel
class BurnoutPredictionModel(private val context: Context) {

    companion object {
        private const val MODEL_PATH = "burnout_model.tflite"

        // ⭐ ACTUALIZA ESTOS VALORES con los del Paso 2, Celda 7
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
                // OPCIONAL: Habilitar NNAPI si está disponible
                setUseNNAPI(true)
            }
            interpreter = Interpreter(modelFile, options)
            android.util.Log.d("BurnoutModel", "✅ Modelo cargado exitosamente")
        } catch (e: Exception) {
            android.util.Log.e("BurnoutModel", "❌ Error cargando modelo", e)
            throw RuntimeException("Error cargando modelo de IA", e)
        }
    }

    private fun normalizeFeatures(features: FloatArray): FloatArray {
        return FloatArray(8) { i ->
            (features[i] - FEATURE_MEAN[i]) / FEATURE_STD[i]
        }
    }

    suspend fun predict(data: QuestionnaireData): BurnoutPrediction =
        withContext(Dispatchers.Default) {
            val interpreter = this@BurnoutPredictionModel.interpreter
                ?: throw IllegalStateException("Modelo no inicializado")

            val rawFeatures = floatArrayOf(
                data.estresIndex,
                data.ergonomiaIndex,
                data.cargaTrabajoIndex,
                data.calidadSuenoIndex,
                data.actividadFisicaIndex,
                data.sintomasMuscularesIndex,
                data.sintomasVisualesIndex,
                data.saludGeneralIndex
            )

            val normalized = normalizeFeatures(rawFeatures)
            val inputArray = Array(1) { normalized }
            val outputArray = Array(1) { FloatArray(3) }

            interpreter.run(inputArray, outputArray)

            BurnoutPrediction(
                probabilidadBajo = outputArray[0][0],
                probabilidadMedio = outputArray[0][1],
                probabilidadAlto = outputArray[0][2]
            )
        }
// ✅ AGREGAR ESTOS MÉTODOS DENTRO DE LA CLASE

    fun healthScoreToIndices(healthScore: HealthScore): FloatArray {
        return floatArrayOf(
            (healthScore.estresSaludMentalScore / 100f * 10f),
            ((100 - healthScore.ergonomiaScore) / 100f * 10f),
            (healthScore.cargaTrabajoScore / 100f * 10f),
            (healthScore.habitosSuenoScore / 100f * 10f),
            (healthScore.actividadFisicaScore / 100f * 10f),
            (healthScore.sintomasMuscularesScore / 100f * 10f),
            (healthScore.sintomasVisualesScore / 100f * 10f),
            (healthScore.saludGeneralScore / 100f * 10f)
        )
    }

    suspend fun predictFromHealthScore(healthScore: HealthScore): EnhancedBurnoutPrediction =
        withContext(Dispatchers.Default) {
            val interpreter = this@BurnoutPredictionModel.interpreter
                ?: throw IllegalStateException("Modelo no inicializado")

            val features = healthScoreToIndices(healthScore)
            val normalizedFeatures = normalizeFeatures(features)

            val inputArray = Array(1) { normalizedFeatures }
            val outputArray = Array(1) { FloatArray(3) }

            interpreter.run(inputArray, outputArray)
            val probabilities = outputArray[0]

            val criticalPatterns = healthScore.criticalPatterns
            val hasUrgentPatterns = criticalPatterns.any {
                it.severity == CriticalLevel.INTERVENCION_URGENTE
            }

            val adjustedPrediction = if (hasUrgentPatterns) {
                NivelRiesgoBurnout.ALTO
            } else when {
                probabilities[2] > 0.5f -> NivelRiesgoBurnout.ALTO
                probabilities[1] > 0.4f -> NivelRiesgoBurnout.MEDIO
                else -> NivelRiesgoBurnout.BAJO
            }

            EnhancedBurnoutPrediction(
                probabilidadBajo = probabilities[0],
                probabilidadMedio = probabilities[1],
                probabilidadAlto = probabilities[2],
                nivelRiesgo = adjustedPrediction,
                confianza = probabilities.maxOrNull() ?: 0.5f,
                factoresRiesgo = emptyList(),
                recomendaciones = listOf("Basado en análisis de IA"),
                criticalPatterns = criticalPatterns,
                hasCriticalPatterns = criticalPatterns.isNotEmpty(),
                requiresUrgentAttention = hasUrgentPatterns,
                scoringVersion = healthScore.version
            )
        }
    fun close() {
        interpreter?.close()
        interpreter = null
    }
}