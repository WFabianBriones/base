package com.example.uleammed.scoring

import android.content.Context
import android.content.SharedPreferences
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
     * Calcula scores de todas las encuestas completadas
     */
    // En ScoringRepository.kt, método calculateAllScores():

    suspend fun calculateAllScores(): Result<HealthScore> = withContext(Dispatchers.IO) {
        return@withContext try {
            val userId = auth.currentUser?.uid ?: return@withContext Result.failure(
                IllegalStateException("Usuario no autenticado")
            )

            val db = firestore.collection("users").document(userId)

            // ✅ AÑADIR: Obtener cuestionario inicial de salud
            val saludGeneralDoc = firestore.collection("questionnaires")
                .document(userId)
                .get()
                .await()

            val ergonomiaDoc = db.collection("questionnaires").document("ergonomia").get().await()
            val muscularesDoc = db.collection("questionnaires").document("sintomas_musculares").get().await()
            val visualesDoc = db.collection("questionnaires").document("sintomas_visuales").get().await()
            val cargaDoc = db.collection("questionnaires").document("carga_trabajo").get().await()
            val estresDoc = db.collection("questionnaires").document("estres_salud_mental").get().await()
            val suenoDoc = db.collection("questionnaires").document("habitos_sueno").get().await()
            val actividadDoc = db.collection("questionnaires").document("actividad_fisica").get().await()
            val balanceDoc = db.collection("questionnaires").document("balance_vida_trabajo").get().await()

            val scores = mutableMapOf<String, Pair<Int, RiskLevel>>()

            // ✅ CALCULAR: Salud General
            var saludGeneralScore = 0
            var saludGeneralRisk = RiskLevel.BAJO
            if (saludGeneralDoc.exists()) {
                val q = saludGeneralDoc.toObject(com.example.uleammed.HealthQuestionnaire::class.java)
                if (q != null) {
                    val result = ScoreCalculator.calculateHealthQuestionnaireScore(q)
                    saludGeneralScore = result.first
                    saludGeneralRisk = result.second
                    scores["salud_general"] = result
                }
            }

            // ... resto del código existente para las otras 8 áreas ...

            // ✅ ACTUALIZAR pesos para 9 áreas
            val weights = mapOf(
                "salud_general" to 0.10,    // ✅ NUEVO
                "estres" to 0.18,
                "sintomas_musculares" to 0.16,
                "carga_trabajo" to 0.14,
                "sueno" to 0.11,
                "balance" to 0.11,
                "ergonomia" to 0.09,
                "sintomas_visuales" to 0.07,
                "actividad_fisica" to 0.04
            )

            val (overallScore, overallRisk) = ScoreCalculator.calculateOverallScore(scores)
            val topConcerns = identifyTopConcerns(scores)
            val recommendations = generateRecommendations(scores)

            val healthScore = HealthScore(
                userId = userId,
                timestamp = System.currentTimeMillis(),
                saludGeneralScore = saludGeneralScore,         // ✅ NUEVO
                saludGeneralRisk = saludGeneralRisk,           // ✅ NUEVO
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

            saveToFirestore(healthScore)
            saveToLocal(healthScore)

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
     * Genera recomendaciones personalizadas basadas en scores
     */
    private fun generateRecommendations(scores: Map<String, Pair<Int, RiskLevel>>): List<String> {
        val recommendations = mutableListOf<String>()

        scores.forEach { (key, pair) ->
            val score = pair.first
            val risk = pair.second

            when (key) {
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

        // Recomendación general si todo está bien
        if (recommendations.isEmpty()) {
            recommendations.add("✅ ¡Excelente! Mantén tus hábitos saludables")
        }

        return recommendations.take(5) // Máximo 5 recomendaciones
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
                    saveToLocal(score) // Actualizar caché
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