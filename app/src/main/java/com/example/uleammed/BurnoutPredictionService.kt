package com.example.uleammed

import android.content.Context
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * Servicio para gestionar predicciones de burnout
 */
class BurnoutPredictionService(private val context: Context) {

    private val neuralNetwork = BurnoutNeuralNetwork(context)
    private val featureExtractor = BurnoutFeatureExtractor()
    private val firestore = FirebaseFirestore.getInstance()

    companion object {
        private const val TAG = "BurnoutPredictionService"
    }

    /**
     * Inicializa el modelo (cargar o crear nuevo)
     */
    suspend fun initialize() = withContext(Dispatchers.IO) {
        try {
            if (neuralNetwork.modelExists()) {
                android.util.Log.d(TAG, "📂 Cargando modelo existente...")
                neuralNetwork.loadModel()
            } else {
                android.util.Log.d(TAG, "🆕 Creando nuevo modelo...")
                neuralNetwork.initializeModel()
                neuralNetwork.compileModel()

                // En producción, entrenarías con datos reales
                // Por ahora, usamos datos sintéticos para demo
                trainWithSyntheticData()
            }

            android.util.Log.d(TAG, "✅ Modelo listo para predicciones")
        } catch (e: Exception) {
            android.util.Log.e(TAG, "❌ Error inicializando modelo", e)
            throw e
        }
    }

    /**
     * Predice burnout desde los cuestionarios del usuario
     */
    suspend fun predictFromQuestionnaires(userId: String): Result<BurnoutPrediction> =
        withContext(Dispatchers.IO) {
            try {
                android.util.Log.d(TAG, "🔍 Obteniendo cuestionarios para usuario: $userId")

                // Obtener todos los cuestionarios desde Firebase
                val ergonomia = getLatestErgonomia(userId)
                    ?: return@withContext Result.failure(Exception("Cuestionario de Ergonomía no encontrado"))

                val sintomasMusculares = getLatestSintomasMusculares(userId)
                    ?: return@withContext Result.failure(Exception("Cuestionario de Síntomas Musculares no encontrado"))

                val sintomasVisuales = getLatestSintomasVisuales(userId)
                    ?: return@withContext Result.failure(Exception("Cuestionario de Síntomas Visuales no encontrado"))

                val cargaTrabajo = getLatestCargaTrabajo(userId)
                    ?: return@withContext Result.failure(Exception("Cuestionario de Carga de Trabajo no encontrado"))

                val estresSaludMental = getLatestEstresSaludMental(userId)
                    ?: return@withContext Result.failure(Exception("Cuestionario de Estrés no encontrado"))

                val habitosSueno = getLatestHabitosSueno(userId)
                    ?: return@withContext Result.failure(Exception("Cuestionario de Sueño no encontrado"))

                val actividadFisica = getLatestActividadFisica(userId)
                    ?: return@withContext Result.failure(Exception("Cuestionario de Actividad Física no encontrado"))

                val balanceVidaTrabajo = getLatestBalanceVidaTrabajo(userId)
                    ?: return@withContext Result.failure(Exception("Cuestionario de Balance Vida-Trabajo no encontrado"))

                val healthQuestionnaire = getHealthQuestionnaire(userId)
                    ?: return@withContext Result.failure(Exception("Cuestionario de Salud General no encontrado"))

                android.util.Log.d(TAG, "✅ Todos los cuestionarios obtenidos")

                // Extraer características
                val inputData = featureExtractor.extractFeatures(
                    ergonomia = ergonomia,
                    sintomasMusculares = sintomasMusculares,
                    sintomasVisuales = sintomasVisuales,
                    cargaTrabajo = cargaTrabajo,
                    estresSaludMental = estresSaludMental,
                    habitosSueno = habitosSueno,
                    actividadFisica = actividadFisica,
                    balanceVidaTrabajo = balanceVidaTrabajo,
                    healthQuestionnaire = healthQuestionnaire
                )

                android.util.Log.d(TAG, "✅ Características extraídas: ${inputData.toFloatArray().size} variables")

                // Realizar predicción
                val prediction = neuralNetwork.predict(inputData)

                // Guardar predicción en Firebase
                savePrediction(userId, prediction)

                android.util.Log.d(TAG, """
                    🎯 Predicción completada
                    - Nivel: ${prediction.nivelRiesgo.displayName}
                    - Probabilidad: ${(prediction.probabilidadBurnout * 100).toInt()}%
                """.trimIndent())

                Result.success(prediction)

            } catch (e: Exception) {
                android.util.Log.e(TAG, "❌ Error en predicción", e)
                Result.failure(e)
            }
        }

    /**
     * Obtiene el historial de predicciones del usuario
     */
    suspend fun getPredictionHistory(userId: String): Result<List<BurnoutPrediction>> =
        withContext(Dispatchers.IO) {
            try {
                val snapshot = firestore.collection("burnout_predictions")
                    .document(userId)
                    .collection("predictions")
                    .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                    .limit(10)
                    .get()
                    .await()

                val predictions = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(BurnoutPrediction::class.java)
                }

                Result.success(predictions)
            } catch (e: Exception) {
                android.util.Log.e(TAG, "❌ Error obteniendo historial", e)
                Result.failure(e)
            }
        }

    /**
     * Analiza la tendencia de burnout (mejorando/empeorando/estable)
     */
    suspend fun analyzeTrend(userId: String): TendenciaBurnout = withContext(Dispatchers.IO) {
        try {
            val historyResult = getPredictionHistory(userId)
            if (historyResult.isFailure || historyResult.getOrNull()?.size ?: 0 < 2) {
                return@withContext TendenciaBurnout.ESTABLE
            }

            val predictions = historyResult.getOrNull()!!
            val latest = predictions.first().probabilidadBurnout
            val previous = predictions[1].probabilidadBurnout

            val diff = latest - previous

            when {
                diff < -0.1f -> TendenciaBurnout.MEJORANDO
                diff > 0.1f -> TendenciaBurnout.EMPEORANDO
                else -> TendenciaBurnout.ESTABLE
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "❌ Error analizando tendencia", e)
            TendenciaBurnout.ESTABLE
        }
    }

    // ============ FUNCIONES PRIVADAS ============

    private suspend fun getLatestErgonomia(userId: String): ErgonomiaQuestionnaire? {
        return try {
            val snapshot = firestore.collection("ergonomia_questionnaires")
                .document(userId)
                .collection("responses")
                .orderBy("completedAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .await()

            snapshot.documents.firstOrNull()?.toObject(ErgonomiaQuestionnaire::class.java)
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error obteniendo Ergonomía", e)
            null
        }
    }

    private suspend fun getLatestSintomasMusculares(userId: String): SintomasMuscularesQuestionnaire? {
        return try {
            val snapshot = firestore.collection("sintomas_musculares_questionnaires")
                .document(userId)
                .collection("responses")
                .orderBy("completedAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .await()

            snapshot.documents.firstOrNull()?.toObject(SintomasMuscularesQuestionnaire::class.java)
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error obteniendo Síntomas Musculares", e)
            null
        }
    }

    private suspend fun getLatestSintomasVisuales(userId: String): SintomasVisualesQuestionnaire? {
        return try {
            val snapshot = firestore.collection("sintomas_visuales_questionnaires")
                .document(userId)
                .collection("responses")
                .orderBy("completedAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .await()

            snapshot.documents.firstOrNull()?.toObject(SintomasVisualesQuestionnaire::class.java)
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error obteniendo Síntomas Visuales", e)
            null
        }
    }

    private suspend fun getLatestCargaTrabajo(userId: String): CargaTrabajoQuestionnaire? {
        return try {
            val snapshot = firestore.collection("carga_trabajo_questionnaires")
                .document(userId)
                .collection("responses")
                .orderBy("completedAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .await()

            snapshot.documents.firstOrNull()?.toObject(CargaTrabajoQuestionnaire::class.java)
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error obteniendo Carga de Trabajo", e)
            null
        }
    }

    private suspend fun getLatestEstresSaludMental(userId: String): EstresSaludMentalQuestionnaire? {
        return try {
            val snapshot = firestore.collection("estres_salud_mental_questionnaires")
                .document(userId)
                .collection("responses")
                .orderBy("completedAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .await()

            snapshot.documents.firstOrNull()?.toObject(EstresSaludMentalQuestionnaire::class.java)
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error obteniendo Estrés", e)
            null
        }
    }

    private suspend fun getLatestHabitosSueno(userId: String): HabitosSuenoQuestionnaire? {
        return try {
            val snapshot = firestore.collection("habitos_sueno_questionnaires")
                .document(userId)
                .collection("responses")
                .orderBy("completedAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .await()

            snapshot.documents.firstOrNull()?.toObject(HabitosSuenoQuestionnaire::class.java)
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error obteniendo Hábitos de Sueño", e)
            null
        }
    }

    private suspend fun getLatestActividadFisica(userId: String): ActividadFisicaQuestionnaire? {
        return try {
            val snapshot = firestore.collection("actividad_fisica_questionnaires")
                .document(userId)
                .collection("responses")
                .orderBy("completedAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .await()

            snapshot.documents.firstOrNull()?.toObject(ActividadFisicaQuestionnaire::class.java)
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error obteniendo Actividad Física", e)
            null
        }
    }

    private suspend fun getLatestBalanceVidaTrabajo(userId: String): BalanceVidaTrabajoQuestionnaire? {
        return try {
            val snapshot = firestore.collection("balance_vida_trabajo_questionnaires")
                .document(userId)
                .collection("responses")
                .orderBy("completedAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .await()

            snapshot.documents.firstOrNull()?.toObject(BalanceVidaTrabajoQuestionnaire::class.java)
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error obteniendo Balance Vida-Trabajo", e)
            null
        }
    }

    private suspend fun getHealthQuestionnaire(userId: String): HealthQuestionnaire? {
        return try {
            val snapshot = firestore.collection("questionnaires")
                .document(userId)
                .get()
                .await()

            snapshot.toObject(HealthQuestionnaire::class.java)
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error obteniendo Health Questionnaire", e)
            null
        }
    }

    private suspend fun savePrediction(userId: String, prediction: BurnoutPrediction) {
        try {
            firestore.collection("burnout_predictions")
                .document(userId)
                .collection("predictions")
                .add(prediction)
                .await()

            android.util.Log.d(TAG, "💾 Predicción guardada en Firebase")
        } catch (e: Exception) {
            android.util.Log.e(TAG, "❌ Error guardando predicción", e)
        }
    }

    /**
     * Entrena con datos sintéticos para DEMO
     * En producción, usarías datos reales recolectados
     */
    private suspend fun trainWithSyntheticData() {
        // Generar datos sintéticos para demo
        val trainingData = mutableListOf<BurnoutInputData>()
        val labels = mutableListOf<Int>()

        // 50 casos de bajo burnout
        repeat(50) {
            trainingData.add(generateSyntheticCase(NivelRiesgo.BAJO))
            labels.add(0)
        }

        // 50 casos de moderado burnout
        repeat(50) {
            trainingData.add(generateSyntheticCase(NivelRiesgo.MODERADO))
            labels.add(1)
        }

        // 50 casos de alto burnout
        repeat(50) {
            trainingData.add(generateSyntheticCase(NivelRiesgo.ALTO))
            labels.add(2)
        }

        android.util.Log.d(TAG, "🎲 Datos sintéticos generados: ${trainingData.size} muestras")

        // Entrenar
        neuralNetwork.trainModel(trainingData, labels, epochs = 100, batchSize = 16)

        // Guardar modelo
        neuralNetwork.saveModel()
    }

    private fun generateSyntheticCase(nivel: NivelRiesgo): BurnoutInputData {
        val random = java.util.Random()

        return when (nivel) {
            NivelRiesgo.BAJO -> BurnoutInputData(
                scoreErgonomia = 0.7f + random.nextFloat() * 0.3f,
                calidadSilla = 0.7f + random.nextFloat() * 0.3f,
                calidadMonitor = 0.7f + random.nextFloat() * 0.3f,
                calidadIluminacion = 0.7f + random.nextFloat() * 0.3f,
                frecuenciaPausas = 0.7f + random.nextFloat() * 0.3f,
                dolorCuello = random.nextFloat() * 0.3f,
                dolorEspalda = random.nextFloat() * 0.3f,
                dolorHombros = random.nextFloat() * 0.3f,
                dolorMunecas = random.nextFloat() * 0.3f,
                rigidezMuscular = random.nextFloat() * 0.3f,
                limitacionMovimiento = random.nextFloat() * 0.2f,
                fatigaVisual = random.nextFloat() * 0.3f,
                ojosSecos = random.nextFloat() * 0.3f,
                visionBorrosa = random.nextFloat() * 0.3f,
                sensibilidadLuz = random.nextFloat() * 0.3f,
                cargaTrabajo = random.nextFloat() * 0.4f,
                presionTiempo = random.nextFloat() * 0.4f,
                horasExtra = random.nextFloat() * 0.3f,
                trabajoFinSemana = random.nextFloat() * 0.3f,
                autonomia = 0.6f + random.nextFloat() * 0.4f,
                apoyoSocial = 0.6f + random.nextFloat() * 0.4f,
                satisfaccionLaboral = 0.6f + random.nextFloat() * 0.4f,
                nivelEstres = random.nextFloat() * 0.4f,
                fatigaEmocional = random.nextFloat() * 0.3f,
                dificultadConcentracion = random.nextFloat() * 0.3f,
                irritabilidad = random.nextFloat() * 0.3f,
                ansiedad = random.nextFloat() * 0.3f,
                perdidaMotivacion = random.nextFloat() * 0.3f,
                sensacionAbrumado = random.nextFloat() * 0.3f,
                despersonalizacion = random.nextFloat() * 0.3f,
                agotamientoEmocional = random.nextFloat() * 0.3f,
                calidadSueno = 0.7f + random.nextFloat() * 0.3f,
                horasSueno = 0.7f + random.nextFloat() * 0.3f,
                dificultadDormir = random.nextFloat() * 0.3f,
                frecuenciaEjercicio = 0.6f + random.nextFloat() * 0.4f,
                nivelActividad = 0.6f + random.nextFloat() * 0.4f,
                equilibrioVidaTrabajo = 0.7f + random.nextFloat() * 0.3f,
                tiempoLibre = 0.6f + random.nextFloat() * 0.4f,
                capacidadDesconectar = 0.7f + random.nextFloat() * 0.3f,
                edad = random.nextFloat(),
                aniosExperiencia = random.nextFloat(),
                genero = random.nextFloat()
            )

            NivelRiesgo.MODERADO -> BurnoutInputData(
                scoreErgonomia = 0.4f + random.nextFloat() * 0.3f,
                calidadSilla = 0.4f + random.nextFloat() * 0.3f,
                calidadMonitor = 0.4f + random.nextFloat() * 0.3f,
                calidadIluminacion = 0.4f + random.nextFloat() * 0.3f,
                frecuenciaPausas = 0.3f + random.nextFloat() * 0.3f,
                dolorCuello = 0.3f + random.nextFloat() * 0.4f,
                dolorEspalda = 0.3f + random.nextFloat() * 0.4f,
                dolorHombros = 0.3f + random.nextFloat() * 0.4f,
                dolorMunecas = 0.3f + random.nextFloat() * 0.4f,
                rigidezMuscular = 0.3f + random.nextFloat() * 0.4f,
                limitacionMovimiento = 0.3f + random.nextFloat() * 0.3f,
                fatigaVisual = 0.4f + random.nextFloat() * 0.3f,
                ojosSecos = 0.4f + random.nextFloat() * 0.3f,
                visionBorrosa = 0.4f + random.nextFloat() * 0.3f,
                sensibilidadLuz = 0.4f + random.nextFloat() * 0.3f,
                cargaTrabajo = 0.5f + random.nextFloat() * 0.3f,
                presionTiempo = 0.5f + random.nextFloat() * 0.3f,
                horasExtra = 0.4f + random.nextFloat() * 0.4f,
                trabajoFinSemana = 0.4f + random.nextFloat() * 0.4f,
                autonomia = 0.3f + random.nextFloat() * 0.4f,
                apoyoSocial = 0.3f + random.nextFloat() * 0.4f,
                satisfaccionLaboral = 0.3f + random.nextFloat() * 0.4f,
                nivelEstres = 0.5f + random.nextFloat() * 0.3f,
                fatigaEmocional = 0.4f + random.nextFloat() * 0.4f,
                dificultadConcentracion = 0.4f + random.nextFloat() * 0.4f,
                irritabilidad = 0.4f + random.nextFloat() * 0.4f,
                ansiedad = 0.4f + random.nextFloat() * 0.4f,
                perdidaMotivacion = 0.4f + random.nextFloat() * 0.4f,
                sensacionAbrumado = 0.4f + random.nextFloat() * 0.4f,
                despersonalizacion = 0.4f + random.nextFloat() * 0.4f,
                agotamientoEmocional = 0.4f + random.nextFloat() * 0.4f,
                calidadSueno = 0.3f + random.nextFloat() * 0.4f,
                horasSueno = 0.3f + random.nextFloat() * 0.4f,
                dificultadDormir = 0.4f + random.nextFloat() * 0.4f,
                frecuenciaEjercicio = 0.2f + random.nextFloat() * 0.4f,
                nivelActividad = 0.2f + random.nextFloat() * 0.4f,
                equilibrioVidaTrabajo = 0.3f + random.nextFloat() * 0.4f,
                tiempoLibre = 0.3f + random.nextFloat() * 0.4f,
                capacidadDesconectar = 0.3f + random.nextFloat() * 0.4f,
                edad = random.nextFloat(),
                aniosExperiencia = random.nextFloat(),
                genero = random.nextFloat()
            )

            else -> BurnoutInputData( // ALTO/CRÍTICO
                scoreErgonomia = random.nextFloat() * 0.4f,
                calidadSilla = random.nextFloat() * 0.4f,
                calidadMonitor = random.nextFloat() * 0.4f,
                calidadIluminacion = random.nextFloat() * 0.4f,
                frecuenciaPausas = random.nextFloat() * 0.3f,
                dolorCuello = 0.6f + random.nextFloat() * 0.4f,
                dolorEspalda = 0.6f + random.nextFloat() * 0.4f,
                dolorHombros = 0.6f + random.nextFloat() * 0.4f,
                dolorMunecas = 0.6f + random.nextFloat() * 0.4f,
                rigidezMuscular = 0.6f + random.nextFloat() * 0.4f,
                limitacionMovimiento = 0.5f + random.nextFloat() * 0.5f,
                fatigaVisual = 0.6f + random.nextFloat() * 0.4f,
                ojosSecos = 0.6f + random.nextFloat() * 0.4f,
                visionBorrosa = 0.6f + random.nextFloat() * 0.4f,
                sensibilidadLuz = 0.6f + random.nextFloat() * 0.4f,
                cargaTrabajo = 0.7f + random.nextFloat() * 0.3f,
                presionTiempo = 0.7f + random.nextFloat() * 0.3f,
                horasExtra = 0.7f + random.nextFloat() * 0.3f,
                trabajoFinSemana = 0.7f + random.nextFloat() * 0.3f,
                autonomia = random.nextFloat() * 0.3f,
                apoyoSocial = random.nextFloat() * 0.3f,
                satisfaccionLaboral = random.nextFloat() * 0.3f,
                nivelEstres = 0.7f + random.nextFloat() * 0.3f,
                fatigaEmocional = 0.7f + random.nextFloat() * 0.3f,
                dificultadConcentracion = 0.6f + random.nextFloat() * 0.4f,
                irritabilidad = 0.6f + random.nextFloat() * 0.4f,
                ansiedad = 0.7f + random.nextFloat() * 0.3f,
                perdidaMotivacion = 0.7f + random.nextFloat() * 0.3f,
                sensacionAbrumado = 0.7f + random.nextFloat() * 0.3f,
                despersonalizacion = 0.6f + random.nextFloat() * 0.4f,
                agotamientoEmocional = 0.7f + random.nextFloat() * 0.3f,
                calidadSueno = random.nextFloat() * 0.4f,
                horasSueno = random.nextFloat() * 0.4f,
                dificultadDormir = 0.6f + random.nextFloat() * 0.4f,
                frecuenciaEjercicio = random.nextFloat() * 0.3f,
                nivelActividad = random.nextFloat() * 0.3f,
                equilibrioVidaTrabajo = random.nextFloat() * 0.3f,
                tiempoLibre = random.nextFloat() * 0.3f,
                capacidadDesconectar = random.nextFloat() * 0.3f,
                edad = random.nextFloat(),
                aniosExperiencia = random.nextFloat(),
                genero = random.nextFloat()
            )
        }
    }

    fun close() {
        neuralNetwork.close()
    }
}