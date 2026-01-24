package com.example.uleammed.burnoutprediction.model

import android.content.Context
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class BurnoutPredictionModel(private val context: Context) {

    companion object {
        private const val MODEL_PATH = "burnout_model.tflite"

        // ⭐ TUS PARÁMETROS REALES
        private val FEATURE_MEAN = floatArrayOf(
            5.4175f, 4.7866f, 5.9756f, 4.7321f,
            5.3381f, 4.4214f, 4.6780f, 4.6128f
        )

        private val FEATURE_STD = floatArrayOf(
            2.5588f, 2.3764f, 2.2812f, 2.6649f,
            2.4335f, 2.4714f, 2.1957f, 2.2618f
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
        } catch (e: Exception) {
            throw RuntimeException("Error cargando modelo de IA", e)
        }
    }

    private fun normalizeFeatures(features: FloatArray): FloatArray {
        return FloatArray(8) { i ->
            (features[i] - FEATURE_MEAN[i]) / FEATURE_STD[i]
        }
    }

    suspend fun predict(data: QuestionnaireData): BurnoutPrediction = withContext(Dispatchers.Default) {
        val interpreter = this@BurnoutPredictionModel.interpreter
            ?: throw IllegalStateException("Modelo no inicializado")

        val rawFeatures = floatArrayOf(
            data.estresIndex, data.ergonomiaIndex, data.cargaTrabajoIndex,
            data.calidadSuenoIndex, data.actividadFisicaIndex,
            data.sintomasMuscularesIndex, data.sintomasVisualesIndex,
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