package com.example.uleammed

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.concurrent.TimeUnit

/**
 * Modelo de estadísticas de cuestionarios
 */
data class QuestionnaireStats(
    val userId: String = "",
    val completionHistory: Map<String, List<CompletionRecord>> = emptyMap(), // questionnaireType -> records
    val streaks: Map<String, Int> = emptyMap(), // questionnaireType -> racha actual
    val bestStreaks: Map<String, Int> = emptyMap(), // questionnaireType -> mejor racha
    val totalCompleted: Int = 0,
    val onTimeCompletions: Int = 0,
    val lateCompletions: Int = 0,
    val lastUpdated: Long = System.currentTimeMillis()
)

/**
 * Registro individual de completación
 */
data class CompletionRecord(
    val questionnaireType: String,
    val completedAt: Long,
    val dueDate: Long,
    val wasOnTime: Boolean,
    val daysEarly: Int = 0, // Días de anticipación (positivo) o retraso (negativo)
    val periodDays: Int = 7 // Periodicidad configurada en ese momento
)

/**
 * Resumen de estadísticas por cuestionario
 */
data class QuestionnaireStatsSummary(
    val type: QuestionnaireType,
    val totalCompletions: Int,
    val currentStreak: Int,
    val bestStreak: Int,
    val onTimeRate: Float, // Porcentaje de completaciones a tiempo (0.0 - 1.0)
    val lastCompleted: Long?,
    val nextDue: Long?
)

/**
 * Gestor de estadísticas de cuestionarios
 */
class QuestionnaireStatsManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(
        "questionnaire_stats",
        Context.MODE_PRIVATE
    )
    private val gson = Gson()

    companion object {
        private const val KEY_STATS = "stats"
    }

    /**
     * Obtener estadísticas del usuario
     */
    fun getStats(userId: String): QuestionnaireStats {
        val json = prefs.getString("${KEY_STATS}_$userId", null)
        return if (json != null) {
            gson.fromJson(json, QuestionnaireStats::class.java)
        } else {
            QuestionnaireStats(userId = userId)
        }
    }

    /**
     * Guardar estadísticas
     */
    private fun saveStats(stats: QuestionnaireStats) {
        val json = gson.toJson(stats)
        prefs.edit().putString("${KEY_STATS}_${stats.userId}", json).apply()
    }

    /**
     * Registrar completación de cuestionario
     */
    fun recordCompletion(
        userId: String,
        questionnaireType: QuestionnaireType,
        completedAt: Long,
        dueDate: Long,
        periodDays: Int
    ) {
        val stats = getStats(userId)

        // Calcular si fue a tiempo
        val daysEarly = TimeUnit.MILLISECONDS.toDays(dueDate - completedAt).toInt()
        val wasOnTime = daysEarly >= 0 // A tiempo si completó antes o en la fecha

        // Crear registro
        val record = CompletionRecord(
            questionnaireType = questionnaireType.name,
            completedAt = completedAt,
            dueDate = dueDate,
            wasOnTime = wasOnTime,
            daysEarly = daysEarly,
            periodDays = periodDays
        )

        // Actualizar historial
        val history = stats.completionHistory.toMutableMap()
        val typeHistory = history[questionnaireType.name]?.toMutableList() ?: mutableListOf()
        typeHistory.add(record)
        history[questionnaireType.name] = typeHistory

        // Actualizar rachas
        val streaks = stats.streaks.toMutableMap()
        val bestStreaks = stats.bestStreaks.toMutableMap()

        val currentStreak = if (wasOnTime) {
            (streaks[questionnaireType.name] ?: 0) + 1
        } else {
            0 // Rompe la racha si es tarde
        }

        streaks[questionnaireType.name] = currentStreak

        // Actualizar mejor racha
        val currentBest = bestStreaks[questionnaireType.name] ?: 0
        if (currentStreak > currentBest) {
            bestStreaks[questionnaireType.name] = currentStreak
        }

        // Actualizar contadores globales
        val newStats = stats.copy(
            completionHistory = history,
            streaks = streaks,
            bestStreaks = bestStreaks,
            totalCompleted = stats.totalCompleted + 1,
            onTimeCompletions = if (wasOnTime) stats.onTimeCompletions + 1 else stats.onTimeCompletions,
            lateCompletions = if (!wasOnTime) stats.lateCompletions + 1 else stats.lateCompletions,
            lastUpdated = System.currentTimeMillis()
        )

        saveStats(newStats)

        android.util.Log.d("QuestionnaireStats", """
            ============ ESTADÍSTICA REGISTRADA ============
            Tipo: $questionnaireType
            A tiempo: $wasOnTime
            Días de ${if (daysEarly >= 0) "anticipación" else "retraso"}: ${kotlin.math.abs(daysEarly)}
            Racha actual: $currentStreak
            Mejor racha: ${bestStreaks[questionnaireType.name]}
            ============================================
        """.trimIndent())
    }

    /**
     * Obtener resumen de un cuestionario específico
     */
    fun getQuestionnaireSummary(
        userId: String,
        questionnaireType: QuestionnaireType,
        notificationManager: QuestionnaireNotificationManager
    ): QuestionnaireStatsSummary {
        val stats = getStats(userId)
        val config = notificationManager.getScheduleConfig(userId)

        val history = stats.completionHistory[questionnaireType.name] ?: emptyList()
        val totalCompletions = history.size
        val onTimeCompletions = history.count { it.wasOnTime }
        val onTimeRate = if (totalCompletions > 0) {
            onTimeCompletions.toFloat() / totalCompletions
        } else {
            0f
        }

        val lastCompleted = config.lastCompletedDates[questionnaireType.name]
        val nextDue = lastCompleted?.let {
            it + TimeUnit.DAYS.toMillis(config.periodDays.toLong())
        }

        return QuestionnaireStatsSummary(
            type = questionnaireType,
            totalCompletions = totalCompletions,
            currentStreak = stats.streaks[questionnaireType.name] ?: 0,
            bestStreak = stats.bestStreaks[questionnaireType.name] ?: 0,
            onTimeRate = onTimeRate,
            lastCompleted = lastCompleted,
            nextDue = nextDue
        )
    }

    /**
     * Obtener resumen global de todas las estadísticas
     */
    fun getGlobalSummary(userId: String): GlobalStatsSummary {
        val stats = getStats(userId)

        val completionRate = if (stats.totalCompleted > 0) {
            stats.onTimeCompletions.toFloat() / stats.totalCompleted
        } else {
            0f
        }

        return GlobalStatsSummary(
            totalCompleted = stats.totalCompleted,
            onTimeCompletions = stats.onTimeCompletions,
            lateCompletions = stats.lateCompletions,
            completionRate = completionRate,
            activeStreaks = stats.streaks.values.sum(),
            bestStreak = stats.bestStreaks.values.maxOrNull() ?: 0
        )
    }

    /**
     * Limpiar estadísticas (para testing o reset)
     */
    fun clearStats(userId: String) {
        prefs.edit().remove("${KEY_STATS}_$userId").apply()
    }
}

/**
 * Resumen global de estadísticas
 */
data class GlobalStatsSummary(
    val totalCompleted: Int,
    val onTimeCompletions: Int,
    val lateCompletions: Int,
    val completionRate: Float, // 0.0 - 1.0
    val activeStreaks: Int,
    val bestStreak: Int
)