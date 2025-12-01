package com.example.uleammed.scoring

import android.content.Context
import android.content.SharedPreferences
import com.example.uleammed.HealthQuestionnaire
import com.example.uleammed.questionnaires.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
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
    }

    /**
     * Calcula scores de TODAS las 9 encuestas completadas
     */
    suspend fun calculateAllScores(): Result<HealthScore> = withContext(Dispatchers.IO) {
        return@withContext try {
            val userId = auth.currentUser?.uid ?: return@withContext Result.failure(
                IllegalStateException("Usuario no autenticado")
            )

            android.util.Log.d(TAG, "🔄 Iniciando cálculo de scores para usuario: $userId")

            // Obtener cuestionarios desde Firestore
            val db = firestore.collection("users").document(userId)

            // ✅ 1. SALUD GENERAL (cuestionario inicial)
            val saludGeneralDoc = firestore.collection("questionnaires")
                .document(userId)
                .get()
                .await()

            // ✅ 2-9. CUESTIONARIOS ESPECÍFICOS
            val ergonomiaDoc = db.collection("questionnaires").document("ergonomia").get().await()
            val muscularesDoc = db.collection("questionnaires").document("sintomas_musculares").get().await()
            val visualesDoc = db.collection("questionnaires").document("sintomas_visuales").get().await()
            val cargaDoc = db.collection("questionnaires").document("carga_trabajo").get().await()
            val estresDoc = db.collection("questionnaires").document("estres_salud_mental").get().await()
            val suenoDoc = db.collection("questionnaires").document("habitos_sueno").get().await()
            val actividadDoc = db.collection("questionnaires").document("actividad_fisica").get().await()
            val balanceDoc = db.collection("questionnaires").document("balance_vida_trabajo").get().await()

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

    /**
     * Identifica las 3 áreas más críticas
     */
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

    /**
     * Genera recomendaciones personalizadas
     */
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

    /**
     * Guardar en Firestore
     */
    private suspend fun saveToFirestore(healthScore: HealthScore) {
        try {
            val userId = auth.currentUser?.uid ?: return

            firestore.collection(COLLECTION_SCORES)
                .document(userId)
                .set(healthScore)
                .await()

            android.util.Log.d(TAG, "✅ Score guardado en Firestore")
        } catch (e: Exception) {
            android.util.Log.e(TAG, "❌ Error guardando en Firestore", e)
        }
    }

    /**
     * Guardar en local (caché)
     */
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

    /**
     * Obtener score actual
     */
    suspend fun getCurrentScore(): Result<HealthScore> = withContext(Dispatchers.IO) {
        return@withContext try {
            val userId = auth.currentUser?.uid ?: return@withContext Result.failure(
                IllegalStateException("Usuario no autenticado")
            )

            // Intentar desde Firestore primero
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

            // Si no hay en Firestore, intentar desde local
            val localJson = prefs.getString(KEY_LOCAL_SCORE, null)
            if (localJson != null) {
                val score = gson.fromJson(localJson, HealthScore::class.java)
                return@withContext Result.success(score)
            }

            // No hay score, calcular
            calculateAllScores()

        } catch (e: Exception) {
            android.util.Log.e(TAG, "❌ Error obteniendo score", e)
            Result.failure(e)
        }
    }

    /**
     * Limpiar scores (para testing)
     */
    fun clearScores() {
        prefs.edit().clear().apply()
    }
}