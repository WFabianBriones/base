package com.example.uleammed.scoring

import android.content.Context
import android.content.SharedPreferences
import com.example.uleammed.HealthQuestionnaire
import com.example.uleammed.questionnaires.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * ✅ REPOSITORIO CORREGIDO - Manejo unificado de Firebase y caching local
 */
class ScoringRepository(private val context: Context) {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val gson = Gson()

    private val prefs: SharedPreferences = context.getSharedPreferences(
        "health_scores",
        Context.MODE_PRIVATE
    )

    companion object {
        private const val TAG = "ScoringRepository"
        private const val COLLECTION_SCORES = "health_scores"
        private const val KEY_LOCAL_SCORE = "local_score"
        private const val KEY_LAST_CALC_TIME = "last_calculation_time"
    }

    /**
     * ✅ CORREGIDO: Estructura Firebase unificada
     * Todos los cuestionarios están en: users/{userId}/questionnaires/{questionnaireId}
     */
    suspend fun calculateAllScores(): Result<HealthScore> = withContext(Dispatchers.IO) {
        return@withContext try {
            val userId = auth.currentUser?.uid
                ?: return@withContext Result.failure(IllegalStateException("Usuario no autenticado"))

            android.util.Log.d(TAG, "🔄 Iniciando cálculo de scores para usuario: $userId")

            // ✅ ESTRUCTURA UNIFICADA: users/{userId}/questionnaires/
            val userQuestionnaires = firestore.collection("users")
                .document(userId)
                .collection("questionnaires")

            // Obtener todos los documentos
            val saludGeneralDoc = userQuestionnaires.document("salud_general").get().await()
            val ergonomiaDoc = userQuestionnaires.document("ergonomia").get().await()
            val muscularesDoc = userQuestionnaires.document("sintomas_musculares").get().await()
            val visualesDoc = userQuestionnaires.document("sintomas_visuales").get().await()
            val cargaDoc = userQuestionnaires.document("carga_trabajo").get().await()
            val estresDoc = userQuestionnaires.document("estres_salud_mental").get().await()
            val suenoDoc = userQuestionnaires.document("habitos_sueno").get().await()
            val actividadDoc = userQuestionnaires.document("actividad_fisica").get().await()
            val balanceDoc = userQuestionnaires.document("balance_vida_trabajo").get().await()

            val scores = mutableMapOf<String, Pair<Int, RiskLevel>>()

            // ===== CALCULAR SCORES INDIVIDUALES =====

            // 1. Salud General
            var saludGeneralScore = 0
            var saludGeneralRisk = RiskLevel.BAJO
            if (saludGeneralDoc.exists()) {
                val q = saludGeneralDoc.toObject(HealthQuestionnaire::class.java)
                if (q != null) {
                    val result = ScoreCalculator.calculateHealthQuestionnaireScore(q)
                    saludGeneralScore = result.first
                    saludGeneralRisk = result.second
                    scores["salud_general"] = result
                    android.util.Log.d(TAG, "✅ Salud General: $saludGeneralScore (${saludGeneralRisk.displayName})")
                }
            }

            // 2. Ergonomía
            var ergonomiaScore = 0
            var ergonomiaRisk = RiskLevel.BAJO
            if (ergonomiaDoc.exists()) {
                val q = ergonomiaDoc.toObject(ErgonomiaQuestionnaire::class.java)
                if (q != null) {
                    val result = ScoreCalculator.calculateErgonomiaScore(q)
                    ergonomiaScore = result.first
                    ergonomiaRisk = result.second
                    scores["ergonomia"] = result
                    android.util.Log.d(TAG, "✅ Ergonomía: $ergonomiaScore (${ergonomiaRisk.displayName})")
                }
            }

            // 3. Síntomas Musculares
            var sintomasMuscularesScore = 0
            var sintomasMuscularesRisk = RiskLevel.BAJO
            if (muscularesDoc.exists()) {
                val q = muscularesDoc.toObject(SintomasMuscularesQuestionnaire::class.java)
                if (q != null) {
                    val result = ScoreCalculator.calculateSintomasMuscularesScore(q)
                    sintomasMuscularesScore = result.first
                    sintomasMuscularesRisk = result.second
                    scores["sintomas_musculares"] = result
                    android.util.Log.d(TAG, "✅ Síntomas Musculares: $sintomasMuscularesScore (${sintomasMuscularesRisk.displayName})")
                }
            }

            // 4. Síntomas Visuales
            var sintomasVisualesScore = 0
            var sintomasVisualesRisk = RiskLevel.BAJO
            if (visualesDoc.exists()) {
                val q = visualesDoc.toObject(SintomasVisualesQuestionnaire::class.java)
                if (q != null) {
                    val result = ScoreCalculator.calculateSintomasVisualesScore(q)
                    sintomasVisualesScore = result.first
                    sintomasVisualesRisk = result.second
                    scores["sintomas_visuales"] = result
                    android.util.Log.d(TAG, "✅ Síntomas Visuales: $sintomasVisualesScore (${sintomasVisualesRisk.displayName})")
                }
            }

            // 5. Carga de Trabajo
            var cargaTrabajoScore = 0
            var cargaTrabajoRisk = RiskLevel.BAJO
            if (cargaDoc.exists()) {
                val q = cargaDoc.toObject(CargaTrabajoQuestionnaire::class.java)
                if (q != null) {
                    val result = ScoreCalculator.calculateCargaTrabajoScore(q)
                    cargaTrabajoScore = result.first
                    cargaTrabajoRisk = result.second
                    scores["carga_trabajo"] = result
                    android.util.Log.d(TAG, "✅ Carga de Trabajo: $cargaTrabajoScore (${cargaTrabajoRisk.displayName})")
                }
            }

            // 6. Estrés y Salud Mental
            var estresSaludMentalScore = 0
            var estresSaludMentalRisk = RiskLevel.BAJO
            if (estresDoc.exists()) {
                val q = estresDoc.toObject(EstresSaludMentalQuestionnaire::class.java)
                if (q != null) {
                    val result = ScoreCalculator.calculateEstresSaludMentalScore(q)
                    estresSaludMentalScore = result.first
                    estresSaludMentalRisk = result.second
                    scores["estres"] = result
                    android.util.Log.d(TAG, "✅ Estrés: $estresSaludMentalScore (${estresSaludMentalRisk.displayName})")
                }
            }

            // 7. Hábitos de Sueño
            var habitosSuenoScore = 0
            var habitosSuenoRisk = RiskLevel.BAJO
            if (suenoDoc.exists()) {
                val q = suenoDoc.toObject(HabitosSuenoQuestionnaire::class.java)
                if (q != null) {
                    val result = ScoreCalculator.calculateHabitosSuenoScore(q)
                    habitosSuenoScore = result.first
                    habitosSuenoRisk = result.second
                    scores["sueno"] = result
                    android.util.Log.d(TAG, "✅ Sueño: $habitosSuenoScore (${habitosSuenoRisk.displayName})")
                }
            }

            // 8. Actividad Física
            var actividadFisicaScore = 0
            var actividadFisicaRisk = RiskLevel.BAJO
            if (actividadDoc.exists()) {
                val q = actividadDoc.toObject(ActividadFisicaQuestionnaire::class.java)
                if (q != null) {
                    val result = ScoreCalculator.calculateActividadFisicaScore(q)
                    actividadFisicaScore = result.first
                    actividadFisicaRisk = result.second
                    scores["actividad_fisica"] = result
                    android.util.Log.d(TAG, "✅ Actividad Física: $actividadFisicaScore (${actividadFisicaRisk.displayName})")
                }
            }

            // 9. Balance Vida-Trabajo
            var balanceVidaTrabajoScore = 0
            var balanceVidaTrabajoRisk = RiskLevel.BAJO
            if (balanceDoc.exists()) {
                val q = balanceDoc.toObject(BalanceVidaTrabajoQuestionnaire::class.java)
                if (q != null) {
                    val result = ScoreCalculator.calculateBalanceVidaTrabajoScore(q)
                    balanceVidaTrabajoScore = result.first
                    balanceVidaTrabajoRisk = result.second
                    scores["balance"] = result
                    android.util.Log.d(TAG, "✅ Balance: $balanceVidaTrabajoScore (${balanceVidaTrabajoRisk.displayName})")
                }
            }

            // ===== CALCULAR SCORE GENERAL =====
            val (overallScore, overallRisk) = calculateOverallScore(scores)

            // ===== IDENTIFICAR ÁREAS CRÍTICAS =====
            val topConcerns = identifyTopConcerns(scores)
            val recommendations = generateRecommendations(scores)

            // ===== CREAR HEALTHSCORE =====
            val healthScore = HealthScore(
                timestamp = System.currentTimeMillis(),
                saludGeneralScore = saludGeneralScore,
                saludGeneralRisk = saludGeneralRisk,
                ergonomiaScore = ergonomiaScore,
                ergonomiaRisk = ergonomiaRisk,
                sintomasMuscularesScore = sintomasMuscularesScore,
                sintomasMuscularesRisk = sintomasMuscularesRisk,
                sintomasVisualesScore = sintomasVisualesScore,
                sintomasVisualesRisk = sintomasVisualesRisk,
                cargaTrabajoScore = cargaTrabajoScore,
                cargaTrabajoRisk = cargaTrabajoRisk,
                estresSaludMentalScore = estresSaludMentalScore,
                estresSaludMentalRisk = estresSaludMentalRisk,
                habitosSuenoScore = habitosSuenoScore,
                habitosSuenoRisk = habitosSuenoRisk,
                actividadFisicaScore = actividadFisicaScore,
                actividadFisicaRisk = actividadFisicaRisk,
                balanceVidaTrabajoScore = balanceVidaTrabajoScore,
                balanceVidaTrabajoRisk = balanceVidaTrabajoRisk,
                overallScore = overallScore,
                overallRisk = overallRisk,
                topConcerns = topConcerns,
                recommendations = recommendations
            )

            // ✅ VALIDAR
            val validation = validateScores(healthScore)
            if (!validation.isValid) {
                android.util.Log.w(TAG, "⚠️ Validación de scores falló:")
                validation.errors.forEach { error ->
                    android.util.Log.w(TAG, "  - $error")
                }
            }

            // ✅ GUARDAR
            saveToFirestore(healthScore)
            saveToLocal(healthScore)
            saveCalculationTime()

            // Contar cuestionarios completados (sin salud_general)
            val specificQuestionnairesCompleted = scores.keys.filter { it != "salud_general" }.size

            android.util.Log.d(TAG, """
                ✅ Scores calculados exitosamente
                - Score global: $overallScore
                - Riesgo: ${overallRisk.displayName}
                - Cuestionarios específicos completados: $specificQuestionnairesCompleted/8
                - Áreas críticas: ${topConcerns.size}
            """.trimIndent())

            Result.success(healthScore)

        } catch (e: Exception) {
            android.util.Log.e(TAG, "❌ Error calculando scores", e)
            Result.failure(e)
        }
    }

    /**
     * ✅ MEJORADO: Cálculo ponderado del score general
     */
    private fun calculateOverallScore(scores: Map<String, Pair<Int, RiskLevel>>): Pair<Int, RiskLevel> {
        if (scores.isEmpty()) return Pair(0, RiskLevel.BAJO)

        // Pesos para cada área (total = 100)
        val weights = mapOf(
            "salud_general" to 15,
            "ergonomia" to 10,
            "sintomas_musculares" to 12,
            "sintomas_visuales" to 8,
            "carga_trabajo" to 13,
            "estres" to 15,
            "sueno" to 10,
            "actividad_fisica" to 9,
            "balance" to 8
        )

        var totalWeightedScore = 0.0
        var totalWeight = 0

        scores.forEach { (key, pair) ->
            val weight = weights[key] ?: 0
            totalWeightedScore += pair.first * weight
            totalWeight += weight
        }

        val overallScore = if (totalWeight > 0) {
            (totalWeightedScore / totalWeight).toInt().coerceIn(0, 100)
        } else {
            0
        }

        val overallRisk = when {
            overallScore < 25 -> RiskLevel.BAJO
            overallScore < 50 -> RiskLevel.MODERADO
            overallScore < 70 -> RiskLevel.ALTO
            else -> RiskLevel.MUY_ALTO
        }

        return Pair(overallScore, overallRisk)
    }

    /**
     * Identificar las 3 áreas con mayor score (Top concerns)
     */
    private fun identifyTopConcerns(scores: Map<String, Pair<Int, RiskLevel>>): List<String> {
        val displayNames = mapOf(
            "salud_general" to "Salud General",
            "ergonomia" to "Ergonomía",
            "sintomas_musculares" to "Síntomas Musculares",
            "sintomas_visuales" to "Síntomas Visuales",
            "carga_trabajo" to "Carga de Trabajo",
            "estres" to "Estrés y Salud Mental",
            "sueno" to "Hábitos de Sueño",
            "actividad_fisica" to "Actividad Física",
            "balance" to "Balance Vida-Trabajo"
        )

        return scores
            .map { (key, pair) -> Pair(displayNames[key] ?: key, pair.first) }
            .filter { it.second >= 40 } // Solo áreas con riesgo moderado o superior
            .sortedByDescending { it.second }
            .take(3)
            .map { it.first }
    }

    /**
     * Generar recomendaciones basadas en áreas de riesgo
     */
    private fun generateRecommendations(scores: Map<String, Pair<Int, RiskLevel>>): List<String> {
        val recommendations = mutableListOf<String>()

        scores.forEach { (key, pair) ->
            if (pair.second >= RiskLevel.MODERADO) {
                when (key) {
                    "ergonomia" -> recommendations.add("Ajusta tu estación de trabajo según las normas ergonómicas")
                    "sintomas_musculares" -> recommendations.add("Realiza pausas activas cada 2 horas")
                    "sintomas_visuales" -> recommendations.add("Aplica la regla 20-20-20 para descanso visual")
                    "carga_trabajo" -> recommendations.add("Prioriza tareas y establece límites claros")
                    "estres" -> recommendations.add("Practica técnicas de relajación y mindfulness")
                    "sueno" -> recommendations.add("Establece una rutina de sueño consistente")
                    "actividad_fisica" -> recommendations.add("Incorpora al menos 30 minutos de actividad física diaria")
                    "balance" -> recommendations.add("Define límites entre trabajo y vida personal")
                }
            }
        }

        if (recommendations.isEmpty()) {
            recommendations.add("Mantén tus buenos hábitos de salud laboral")
        }

        return recommendations.take(5) // Máximo 5 recomendaciones
    }

    /**
     * Guardar en Firestore
     */
    private suspend fun saveToFirestore(healthScore: HealthScore) {
        try {
            val userId = auth.currentUser?.uid ?: return

            // Guardar score actual
            firestore.collection(COLLECTION_SCORES)
                .document(userId)
                .set(healthScore)
                .await()

            // Guardar en histórico
            saveScoreHistory(healthScore)

            android.util.Log.d(TAG, "✅ Score guardado en Firestore")
        } catch (e: Exception) {
            android.util.Log.e(TAG, "❌ Error guardando en Firestore", e)
        }
    }

    /**
     * Guardar en caché local
     */
    private fun saveToLocal(healthScore: HealthScore) {
        try {
            val json = gson.toJson(healthScore)
            prefs.edit()
                .putString(KEY_LOCAL_SCORE, json)
                .apply()
            android.util.Log.d(TAG, "✅ Score guardado en caché local")
        } catch (e: Exception) {
            android.util.Log.e(TAG, "❌ Error guardando en local", e)
        }
    }

    /**
     * Obtener score actual (con detección de cuestionarios nuevos)
     */
    suspend fun getCurrentScore(): Result<HealthScore> = withContext(Dispatchers.IO) {
        return@withContext try {
            val userId = auth.currentUser?.uid
                ?: return@withContext Result.failure(IllegalStateException("Usuario no autenticado"))

            // Intentar desde Firestore primero
            val doc = firestore.collection(COLLECTION_SCORES)
                .document(userId)
                .get()
                .await()

            if (doc.exists()) {
                val score = doc.toObject(HealthScore::class.java)
                if (score != null) {
                    // ✅ Verificar si hay cuestionarios nuevos
                    val hasNewQuestionnaires = checkForNewQuestionnaires(userId, score)

                    if (hasNewQuestionnaires) {
                        android.util.Log.d(TAG, "🔄 Detectados cuestionarios nuevos, recalculando...")
                        return@withContext calculateAllScores()
                    }

                    saveToLocal(score)
                    return@withContext Result.success(score)
                }
            }

            // Si no hay en Firestore, intentar desde local
            val localJson = prefs.getString(KEY_LOCAL_SCORE, null)
            if (localJson != null) {
                val score = gson.fromJson(localJson, HealthScore::class.java)

                // También verificar aquí si hay cuestionarios nuevos
                val hasNewQuestionnaires = checkForNewQuestionnaires(userId, score)
                if (hasNewQuestionnaires) {
                    android.util.Log.d(TAG, "🔄 Detectados cuestionarios nuevos en caché, recalculando...")
                    return@withContext calculateAllScores()
                }

                return@withContext Result.success(score)
            }

            // No hay score, calcular por primera vez
            calculateAllScores()

        } catch (e: Exception) {
            android.util.Log.e(TAG, "❌ Error obteniendo score", e)
            Result.failure(e)
        }
    }

    /**
     * ✅ NUEVO: Verificar si hay cuestionarios nuevos en Firebase
     */
    private suspend fun checkForNewQuestionnaires(userId: String, currentScore: HealthScore): Boolean {
        return try {
            val questionnairesRef = firestore.collection("users")
                .document(userId)
                .collection("questionnaires")
                .get()
                .await()

            // Contar cuestionarios completados en Firebase (excluyendo salud_general)
            val firebaseCompletedCount = questionnairesRef.documents
                .filter { doc ->
                    doc.id != "salud_general" && (doc.getLong("completedAt") ?: 0L) > 0
                }
                .size

            // Contar cuestionarios con score > 0 en el HealthScore actual
            val scoreCompletedCount = listOf(
                currentScore.ergonomiaScore,
                currentScore.sintomasMuscularesScore,
                currentScore.sintomasVisualesScore,
                currentScore.cargaTrabajoScore,
                currentScore.estresSaludMentalScore,
                currentScore.habitosSuenoScore,
                currentScore.actividadFisicaScore,
                currentScore.balanceVidaTrabajoScore
            ).count { it > 0 }

            val hasNew = firebaseCompletedCount > scoreCompletedCount

            if (hasNew) {
                android.util.Log.d(TAG,
                    "📊 Firebase: $firebaseCompletedCount/8 completados | Score actual: $scoreCompletedCount/8")
            }

            hasNew
        } catch (e: Exception) {
            android.util.Log.e(TAG, "❌ Error verificando cuestionarios nuevos", e)
            false
        }
    }

    /**
     * Guardar en histórico
     */
    private suspend fun saveScoreHistory(healthScore: HealthScore) {
        try {
            val userId = auth.currentUser?.uid ?: return

            firestore.collection("users")
                .document(userId)
                .collection("score_history")
                .document(healthScore.timestamp.toString())
                .set(healthScore)
                .await()

            android.util.Log.d(TAG, "✅ Score guardado en histórico: ${healthScore.timestamp}")
        } catch (e: Exception) {
            android.util.Log.e(TAG, "❌ Error guardando en histórico", e)
        }
    }

    /**
     * Obtener tendencia de scores (últimos N días)
     */
    suspend fun getScoreTrend(userId: String, days: Int = 30): Result<List<HealthScore>> =
        withContext(Dispatchers.IO) {
            return@withContext try {
                val cutoff = System.currentTimeMillis() - (days * 24 * 60 * 60 * 1000L)

                val querySnapshot = firestore.collection("users")
                    .document(userId)
                    .collection("score_history")
                    .whereGreaterThan("timestamp", cutoff)
                    .orderBy("timestamp", Query.Direction.ASCENDING)
                    .get()
                    .await()

                val scores = querySnapshot.toObjects(HealthScore::class.java)
                android.util.Log.d(TAG, "✅ Tendencia obtenida: ${scores.size} registros")
                Result.success(scores)
            } catch (e: Exception) {
                android.util.Log.e(TAG, "❌ Error obteniendo tendencia", e)
                Result.failure(e)
            }
        }

    /**
     * Obtener último tiempo de cálculo
     */
    fun getLastCalculationTime(): Long {
        return prefs.getLong(KEY_LAST_CALC_TIME, 0L)
    }

    /**
     * Guardar tiempo de cálculo
     */
    private fun saveCalculationTime() {
        prefs.edit()
            .putLong(KEY_LAST_CALC_TIME, System.currentTimeMillis())
            .apply()
    }

    /**
     * Validar scores
     */
    private fun validateScores(healthScore: HealthScore): ValidationResult {
        val errors = mutableListOf<String>()

        val allScores = listOf(
            "salud_general" to healthScore.saludGeneralScore,
            "ergonomia" to healthScore.ergonomiaScore,
            "sintomas_musculares" to healthScore.sintomasMuscularesScore,
            "sintomas_visuales" to healthScore.sintomasVisualesScore,
            "carga_trabajo" to healthScore.cargaTrabajoScore,
            "estres" to healthScore.estresSaludMentalScore,
            "sueno" to healthScore.habitosSuenoScore,
            "actividad_fisica" to healthScore.actividadFisicaScore,
            "balance" to healthScore.balanceVidaTrabajoScore,
            "overall" to healthScore.overallScore
        )

        allScores.forEach { (area, score) ->
            if (score !in 0..100) {
                errors.add("Score de $area fuera de rango: $score")
            }
        }

        if (healthScore.overallScore < 25 && healthScore.overallRisk != RiskLevel.BAJO) {
            errors.add("Inconsistencia: overall score ${healthScore.overallScore} con riesgo ${healthScore.overallRisk.displayName}")
        }

        if (healthScore.overallScore >= 70 &&
            healthScore.overallRisk != RiskLevel.ALTO &&
            healthScore.overallRisk != RiskLevel.MUY_ALTO) {
            errors.add("Inconsistencia: overall score ${healthScore.overallScore} debería ser riesgo alto/muy alto")
        }

        return ValidationResult(
            isValid = errors.isEmpty(),
            errors = errors
        )
    }

    /**
     * Limpiar scores (para testing)
     */
    fun clearScores() {
        prefs.edit().clear().apply()
    }
}