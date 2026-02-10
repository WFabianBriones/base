package com.example.uleammed.burnoutprediction.model

import android.content.Context
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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

    fun close() {
        interpreter?.close()
        interpreter = null
    }
}