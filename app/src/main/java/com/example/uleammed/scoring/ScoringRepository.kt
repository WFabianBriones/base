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
     * ✅ CORREGIDO: Calcula scores usando la estructura users/{userId}/questionnaires/
     */
    suspend fun calculateAllScores(): Result<HealthScore> = withContext(Dispatchers.IO) {
        return@withContext try {
            val userId = auth.currentUser?.uid ?: return@withContext Result.failure(
                IllegalStateException("Usuario no autenticado")
            )

            android.util.Log.d(TAG, "🔄 Iniciando cálculo de scores para usuario: $userId")

            // ✅ OBTENER CUESTIONARIOS DESDE: users/{userId}/questionnaires/
            val userQuestionnaires = firestore.collection("users")
                .document(userId)
                .collection("questionnaires")

            // ✅ 1. SALUD GENERAL
            val saludGeneralDoc = userQuestionnaires.document("salud_general").get().await()

            // ✅ 2-9. CUESTIONARIOS ESPECÍFICOS
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
            } else {
                android.util.Log.w(TAG, "⚠️ Cuestionario de Salud General no encontrado")
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

            // ===== CALCULAR SCORE GLOBAL =====
            val (overallScore, overallRisk) = ScoreCalculator.calculateOverallScore(scores)

            // Identificar áreas críticas
            val topConcerns = identifyTopConcerns(scores)

            // Generar recomendaciones
            val recommendations = generateRecommendations(scores)

            // Crear objeto HealthScore con las 9 áreas
            val healthScore = HealthScore(
                userId = userId,
                timestamp = System.currentTimeMillis(),
                saludGeneralScore = saludGeneralScore,
                saludGeneralRisk = saludGeneralRisk,
                ergonomiaScore = ergonomiaScore,
                sintomasMuscularesScore = sintomasMuscularesScore,
                sintomasVisualesScore = sintomasVisualesScore,
                cargaTrabajoScore = cargaTrabajoScore,
                estresSaludMentalScore = estresSaludMentalScore,
                habitosSuenoScore = habitosSuenoScore,
                actividadFisicaScore = actividadFisicaScore,
                balanceVidaTrabajoScore = balanceVidaTrabajoScore,
                ergonomiaRisk = ergonomiaRisk,
                sintomasMuscularesRisk = sintomasMuscularesRisk,
                sintomasVisualesRisk = sintomasVisualesRisk,
                cargaTrabajoRisk = cargaTrabajoRisk,
                estresSaludMentalRisk = estresSaludMentalRisk,
                habitosSuenoRisk = habitosSuenoRisk,
                actividadFisicaRisk = actividadFisicaRisk,
                balanceVidaTrabajoRisk = balanceVidaTrabajoRisk,
                overallScore = overallScore,
                overallRisk = overallRisk,
                topConcerns = topConcerns,
                recommendations = recommendations
            )

            // Validar antes de guardar
            val validation = validateScores(healthScore)
            if (!validation.isValid) {
                android.util.Log.w(TAG, "⚠️ Validación de scores falló:")
                validation.errors.forEach { error ->
                    android.util.Log.w(TAG, "  - $error")
                }
            }

            // Guardar en Firestore y caché local
            saveToFirestore(healthScore)
            saveToLocal(healthScore)

            android.util.Log.d(TAG, """
                ✅ Scores calculados exitosamente
                - Score global: $overallScore
                - Riesgo: ${overallRisk.displayName}
                - Cuestionarios completados: ${scores.size}/9
                - Áreas críticas: ${topConcerns.size}
            """.trimIndent())

            Result.success(healthScore)

        } catch (e: Exception) {
            android.util.Log.e(TAG, "❌ Error calculando scores", e)
            Result.failure(e)
        }
    }

    private fun identifyTopConcerns(scores: Map<String, Pair<Int, RiskLevel>>): List<String> {
        val concerns = mutableListOf<Pair<String, Int>>()

        scores.forEach { (key, pair) ->
            val displayName = when (key) {
                "salud_general" -> "Salud General"
                "ergonomia" -> "Ergonomía"
                "sintomas_musculares" -> "Síntomas Musculares"
                "sintomas_visuales" -> "Síntomas Visuales"
                "carga_trabajo" -> "Carga de Trabajo"
                "estres" -> "Estrés y Salud Mental"
                "sueno" -> "Calidad del Sueño"
                "actividad_fisica" -> "Actividad Física"
                "balance" -> "Balance Vida-Trabajo"
                else -> key
            }

            concerns.add(Pair(displayName, pair.first))
        }

        return concerns
            .sortedByDescending { it.second }
            .take(3)
            .map { it.first }
    }

    private fun generateRecommendations(scores: Map<String, Pair<Int, RiskLevel>>): List<String> {
        val recommendations = mutableListOf<String>()

        scores.forEach { (key, pair) ->
            val risk = pair.second

            when (key) {
                "salud_general" -> {
                    if (risk.value >= RiskLevel.ALTO.value) {
                        recommendations.add("🏥 Consulta médica general recomendada para evaluación integral")
                    }
                }
                "ergonomia" -> {
                    if (risk.value >= RiskLevel.ALTO.value) {
                        recommendations.add("⚠️ Mejora urgente de tu estación de trabajo ergonómica")
                    } else if (risk.value >= RiskLevel.MODERADO.value) {
                        recommendations.add("Ajusta tu silla y monitor para mejor postura")
                    }
                }
                "sintomas_musculares" -> {
                    if (risk.value >= RiskLevel.ALTO.value) {
                        recommendations.add("🚨 Consulta médica recomendada por dolor músculo-esquelético")
                    } else if (risk.value >= RiskLevel.MODERADO.value) {
                        recommendations.add("Realiza estiramientos cada 30 minutos")
                    }
                }
                "sintomas_visuales" -> {
                    if (risk.value >= RiskLevel.ALTO.value) {
                        recommendations.add("👁️ Examen visual urgente recomendado")
                    } else if (risk.value >= RiskLevel.MODERADO.value) {
                        recommendations.add("Aplica la regla 20-20-20 para tus ojos")
                    }
                }
                "carga_trabajo" -> {
                    if (risk.value >= RiskLevel.ALTO.value) {
                        recommendations.add("⚡ Tu carga laboral es excesiva - habla con tu supervisor")
                    } else if (risk.value >= RiskLevel.MODERADO.value) {
                        recommendations.add("Establece límites claros en tu horario laboral")
                    }
                }
                "estres" -> {
                    if (risk.value >= RiskLevel.MUY_ALTO.value) {
                        recommendations.add("🆘 Riesgo de burnout - busca apoyo profesional inmediatamente")
                    } else if (risk.value >= RiskLevel.ALTO.value) {
                        recommendations.add("❗ Considera consultar un profesional de salud mental")
                    } else if (risk.value >= RiskLevel.MODERADO.value) {
                        recommendations.add("Practica técnicas de manejo del estrés diariamente")
                    }
                }
                "sueno" -> {
                    if (risk.value >= RiskLevel.ALTO.value) {
                        recommendations.add("💤 Mejora urgente de tu higiene del sueño necesaria")
                    } else if (risk.value >= RiskLevel.MODERADO.value) {
                        recommendations.add("Apaga dispositivos 2 horas antes de dormir")
                    }
                }
                "actividad_fisica" -> {
                    if (risk.value >= RiskLevel.ALTO.value) {
                        recommendations.add("🏃 Incrementa tu actividad física gradualmente")
                    } else if (risk.value >= RiskLevel.MODERADO.value) {
                        recommendations.add("Objetivo: 150 minutos de ejercicio semanal")
                    }
                }
                "balance" -> {
                    if (risk.value >= RiskLevel.ALTO.value) {
                        recommendations.add("⚖️ Tu balance vida-trabajo está comprometido - toma acción")
                    } else if (risk.value >= RiskLevel.MODERADO.value) {
                        recommendations.add("Dedica tiempo de calidad a tu vida personal")
                    }
                }
            }
        }

        if (recommendations.isEmpty()) {
            recommendations.add("✅ ¡Excelente! Mantén tus hábitos saludables")
        }

        return recommendations.take(5)
    }

    private suspend fun saveToFirestore(healthScore: HealthScore) {
        try {
            val userId = auth.currentUser?.uid ?: return

            firestore.collection(COLLECTION_SCORES)
                .document(userId)
                .set(healthScore)
                .await()

            saveScoreHistory(healthScore)
            saveCalculationTime()

            android.util.Log.d(TAG, "✅ Score guardado en Firestore e histórico")
        } catch (e: Exception) {
            android.util.Log.e(TAG, "❌ Error guardando en Firestore", e)
        }
    }

    private fun saveToLocal(healthScore: HealthScore) {
        try {
            val json = gson.toJson(healthScore)
            prefs.edit()
                .putString(KEY_LOCAL_SCORE, json)
                .apply()

            android.util.Log.d(TAG, "✅ Score guardado localmente")
        } catch (e: Exception) {
            android.util.Log.e(TAG, "❌ Error guardando localmente", e)
        }
    }

    suspend fun getCurrentScore(): Result<HealthScore> = withContext(Dispatchers.IO) {
        return@withContext try {
            val userId = auth.currentUser?.uid ?: return@withContext Result.failure(
                IllegalStateException("Usuario no autenticado")
            )

            val doc = firestore.collection(COLLECTION_SCORES)
                .document(userId)
                .get()
                .await()

            if (doc.exists()) {
                val score = doc.toObject(HealthScore::class.java)
                if (score != null) {
                    saveToLocal(score)
                    return@withContext Result.success(score)
                }
            }

            val localJson = prefs.getString(KEY_LOCAL_SCORE, null)
            if (localJson != null) {
                val score = gson.fromJson(localJson, HealthScore::class.java)
                return@withContext Result.success(score)
            }

            calculateAllScores()

        } catch (e: Exception) {
            android.util.Log.e(TAG, "❌ Error obteniendo score", e)
            Result.failure(e)
        }
    }

    fun clearScores() {
        prefs.edit().clear().apply()
    }

    suspend fun saveScoreHistory(healthScore: HealthScore) {
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

    fun getLastCalculationTime(): Long {
        return prefs.getLong(KEY_LAST_CALC_TIME, 0L)
    }

    private fun saveCalculationTime() {
        prefs.edit()
            .putLong(KEY_LAST_CALC_TIME, System.currentTimeMillis())
            .apply()
    }

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

        if (healthScore.overallScore >= 65 && healthScore.overallRisk != RiskLevel.ALTO && healthScore.overallRisk != RiskLevel.MUY_ALTO) {
            errors.add("Inconsistencia: overall score ${healthScore.overallScore} debería ser riesgo alto/muy alto")
        }

        return ValidationResult(
            isValid = errors.isEmpty(),
            errors = errors
        )
    }
}