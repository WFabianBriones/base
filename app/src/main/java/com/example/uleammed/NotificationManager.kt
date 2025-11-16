package com.example.uleammed

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.concurrent.TimeUnit

/**
 * Gestor de notificaciones de cuestionarios con soporte para programación periódica,
 * estadísticas y recordatorios previos.
 *
 * @property context Contexto de la aplicación
 */
class QuestionnaireNotificationManager(private val context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(
        "questionnaire_notifications",
        Context.MODE_PRIVATE
    )
    private val gson = Gson()
    private val statsManager = QuestionnaireStatsManager(context)

    // ✅ NUEVO: Lock para evitar race conditions
    private val lock = Any()

    companion object {
        private const val KEY_NOTIFICATIONS = "notifications"
        private const val KEY_SCHEDULE_CONFIG = "schedule_config"
        private const val KEY_LAST_CHECK = "last_check"
        private const val TAG = "NotificationManager"
    }

    /**
     * Obtener todas las notificaciones del usuario actual
     */
    fun getNotifications(): List<QuestionnaireNotification> {
        val json = prefs.getString(KEY_NOTIFICATIONS, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<QuestionnaireNotification>>() {}.type
            gson.fromJson(json, type)
        } catch (e: Exception) {
            logError("getNotifications", e)
            emptyList()
        }
    }

    /**
     * Guardar lista de notificaciones
     */
    private fun saveNotifications(notifications: List<QuestionnaireNotification>) {
        try {
            val json = gson.toJson(notifications)
            prefs.edit().putString(KEY_NOTIFICATIONS, json).apply()
            logDebug("saveNotifications", mapOf(
                "count" to notifications.size,
                "unread" to notifications.count { !it.isRead }
            ))
        } catch (e: Exception) {
            logError("saveNotifications", e)
        }
    }

    /**
     * Obtener configuración de periodicidad del usuario
     */
    fun getScheduleConfig(userId: String): QuestionnaireScheduleConfig {
        val json = prefs.getString("${KEY_SCHEDULE_CONFIG}_$userId", null)
        return if (json != null) {
            try {
                gson.fromJson(json, QuestionnaireScheduleConfig::class.java)
            } catch (e: Exception) {
                logError("getScheduleConfig", e)
                QuestionnaireScheduleConfig(userId = userId)
            }
        } else {
            QuestionnaireScheduleConfig(userId = userId)
        }
    }

    /**
     * Guardar configuración de periodicidad
     */
    fun saveScheduleConfig(config: QuestionnaireScheduleConfig) {
        try {
            val json = gson.toJson(config)
            prefs.edit().putString("${KEY_SCHEDULE_CONFIG}_${config.userId}", json).apply()
            logDebug("saveScheduleConfig", mapOf(
                "userId" to config.userId,
                "periodDays" to config.periodDays,
                "preferredHour" to config.preferredHour
            ))
        } catch (e: Exception) {
            logError("saveScheduleConfig", e)
        }
    }

    /**
     * Actualizar período de días entre cuestionarios
     */
    fun updatePeriodDays(userId: String, days: Int) {
        synchronized(lock) {
            val config = getScheduleConfig(userId)
            val updatedConfig = config.copy(periodDays = days)
            saveScheduleConfig(updatedConfig)

            logDebug("updatePeriodDays", mapOf(
                "userId" to userId,
                "oldPeriod" to config.periodDays,
                "newPeriod" to days
            ))

            // Regenerar notificaciones con el nuevo período
            checkAndGenerateNotifications(userId)
        }
    }

    /**
     * ✅ NUEVO: Actualizar hora preferida para notificaciones
     */
    fun updatePreferredTime(userId: String, hour: Int, minute: Int) {
        synchronized(lock) {
            require(hour in 0..23) { "Hora debe estar entre 0 y 23" }
            require(minute in 0..59) { "Minutos deben estar entre 0 y 59" }

            val config = getScheduleConfig(userId)
            val updatedConfig = config.copy(
                preferredHour = hour,
                preferredMinute = minute
            )
            saveScheduleConfig(updatedConfig)

            logDebug("updatePreferredTime", mapOf(
                "userId" to userId,
                "time" to "$hour:${minute.toString().padStart(2, '0')}"
            ))

            // Regenerar notificaciones con la nueva hora
            checkAndGenerateNotifications(userId)
        }
    }

    /**
     * Marcar cuestionario como completado y programar el siguiente
     */
    fun markQuestionnaireCompleted(userId: String, questionnaireType: QuestionnaireType) {
        synchronized(lock) {
            val config = getScheduleConfig(userId)
            val now = System.currentTimeMillis()
            val updatedDates = config.lastCompletedDates.toMutableMap()

            // Calcular fecha de vencimiento para estadísticas
            val previousCompleted = updatedDates[questionnaireType.name] ?: 0L
            val dueDate = if (previousCompleted > 0L) {
                calculateNextDueDate(previousCompleted, config.periodDays, config.preferredHour, config.preferredMinute)
            } else {
                now // Primera vez, se considera a tiempo
            }

            // ✅ Registrar en estadísticas
            statsManager.recordCompletion(
                userId = userId,
                questionnaireType = questionnaireType,
                completedAt = now,
                dueDate = dueDate,
                periodDays = config.periodDays
            )

            updatedDates[questionnaireType.name] = now
            val updatedConfig = config.copy(lastCompletedDates = updatedDates)
            saveScheduleConfig(updatedConfig)

            // ✅ Eliminar TODAS las notificaciones de este tipo
            val notifications = getNotifications().toMutableList()
            val removedCount = notifications.count { it.questionnaireType == questionnaireType }
            notifications.removeAll { it.questionnaireType == questionnaireType }
            saveNotifications(notifications)

            // Calcular próxima fecha de vencimiento
            val nextDueDate = calculateNextDueDate(now, config.periodDays, config.preferredHour, config.preferredMinute)

            logDebug("markQuestionnaireCompleted", mapOf(
                "type" to questionnaireType.name,
                "completedAt" to formatDate(now),
                "nextDueDate" to formatDate(nextDueDate),
                "daysUntilNext" to TimeUnit.MILLISECONDS.toDays(nextDueDate - now),
                "removedNotifications" to removedCount
            ))

            // ✅ Programar recordatorio 1 día antes (si la periodicidad > 1 día)
            if (config.periodDays > 1) {
                val reminderDate = nextDueDate - TimeUnit.DAYS.toMillis(1)

                if (reminderDate > now) {
                    LocalNotificationScheduler.scheduleNotification(
                        questionnaireType = questionnaireType,
                        dueDate = reminderDate,
                        title = "📅 Recordatorio: ${getQuestionnaireInfo(questionnaireType).title}",
                        message = "Mañana es el día de completar tu cuestionario ${getPeriodText(config.periodDays)}. ¡Prepárate!",
                        isReminder = true,
                        createInAppNotification = config.showRemindersInApp // ✅ NUEVO: Configurable
                    )

                    logDebug("scheduleReminder", mapOf(
                        "type" to questionnaireType.name,
                        "reminderDate" to formatDate(reminderDate),
                        "daysUntilReminder" to TimeUnit.MILLISECONDS.toDays(reminderDate - now)
                    ))
                }
            }

            // ✅ Validar que la fecha sea futura antes de programar
            if (nextDueDate > now) {
                LocalNotificationScheduler.scheduleNotification(
                    questionnaireType = questionnaireType,
                    dueDate = nextDueDate,
                    title = "⏰ Cuestionario pendiente: ${getQuestionnaireInfo(questionnaireType).title}",
                    message = "Es momento de completar tu cuestionario ${getPeriodText(config.periodDays)}.",
                    isReminder = false,
                    createInAppNotification = true
                )
            } else {
                logWarning("markQuestionnaireCompleted", "Fecha de vencimiento en el pasado ignorada")
            }
        }
    }

    /**
     * ✅ MEJORADO: Verificar y generar notificaciones pendientes con lock
     */
    fun checkAndGenerateNotifications(userId: String) {
        synchronized(lock) {
            val config = getScheduleConfig(userId)
            val currentNotifications = getNotifications().toMutableList()
            val now = System.currentTimeMillis()
            var generatedCount = 0

            logDebug("checkAndGenerateNotifications", mapOf(
                "userId" to userId,
                "existingNotifications" to currentNotifications.size
            ))

            QuestionnaireType.values().forEach { type ->
                // Verificar si está habilitado
                if (!config.enabledQuestionnaires.contains(type.name)) {
                    return@forEach
                }

                // Verificar si ya existe una notificación activa
                val existingNotification = currentNotifications.find {
                    it.questionnaireType == type && !it.isCompleted
                }

                if (existingNotification == null) {
                    val lastCompleted = config.lastCompletedDates[type.name] ?: 0L

                    // Solo si ya se completó al menos una vez
                    if (lastCompleted > 0L) {
                        val nextDueDate = calculateNextDueDate(
                            lastCompleted,
                            config.periodDays,
                            config.preferredHour,
                            config.preferredMinute
                        )

                        // ✅ Solo crear notificación si YA LLEGÓ la fecha
                        if (now >= nextDueDate) {
                            val notification = createNotification(
                                type = type,
                                periodDays = config.periodDays,
                                dueDate = nextDueDate
                            )
                            currentNotifications.add(notification)
                            generatedCount++

                            logDebug("generatedNotification", mapOf(
                                "type" to type.name,
                                "dueDate" to formatDate(nextDueDate),
                                "overdueDays" to TimeUnit.MILLISECONDS.toDays(now - nextDueDate)
                            ))
                        } else {
                            val daysRemaining = TimeUnit.MILLISECONDS.toDays(nextDueDate - now)
                            logDebug("notificationNotDue", mapOf(
                                "type" to type.name,
                                "daysRemaining" to daysRemaining,
                                "nextDueDate" to formatDate(nextDueDate)
                            ))
                        }
                    }
                }
            }

            // Actualizar última verificación
            prefs.edit().putLong(KEY_LAST_CHECK, now).apply()
            saveNotifications(currentNotifications)

            logDebug("checkComplete", mapOf(
                "generatedCount" to generatedCount,
                "totalNotifications" to currentNotifications.size
            ))
        }
    }

    /**
     * ✅ NUEVO: Calcular próxima fecha de vencimiento con hora preferida
     */
    private fun calculateNextDueDate(
        lastCompleted: Long,
        periodDays: Int,
        preferredHour: Int,
        preferredMinute: Int
    ): Long {
        val calendar = java.util.Calendar.getInstance().apply {
            timeInMillis = lastCompleted
            add(java.util.Calendar.DAY_OF_MONTH, periodDays)
            set(java.util.Calendar.HOUR_OF_DAY, preferredHour)
            set(java.util.Calendar.MINUTE, preferredMinute)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        return calendar.timeInMillis
    }

    /**
     * Crear una notificación con fecha de vencimiento específica
     */
    private fun createNotification(
        type: QuestionnaireType,
        periodDays: Int,
        dueDate: Long
    ): QuestionnaireNotification {
        val info = getQuestionnaireInfo(type)
        return QuestionnaireNotification(
            questionnaireType = type,
            title = "Cuestionario pendiente: ${info.title}",
            message = "Es momento de completar tu cuestionario ${getPeriodText(periodDays)}. ${info.estimatedTime}",
            dueDate = dueDate,
            createdAt = System.currentTimeMillis()
        )
    }

    /**
     * Marcar notificación como leída
     */
    fun markAsRead(notificationId: String) {
        synchronized(lock) {
            val notifications = getNotifications().toMutableList()
            val index = notifications.indexOfFirst { it.id == notificationId }
            if (index != -1) {
                notifications[index] = notifications[index].copy(isRead = true)
                saveNotifications(notifications)

                logDebug("markAsRead", mapOf("notificationId" to notificationId))
            }
        }
    }

    /**
     * Eliminar notificación específica
     */
    fun deleteNotification(notificationId: String) {
        synchronized(lock) {
            val notifications = getNotifications().toMutableList()
            val notification = notifications.find { it.id == notificationId }

            // Cancelar notificación push si existe
            notification?.let {
                LocalNotificationScheduler.cancelNotification(it.questionnaireType)
                logDebug("deleteNotification", mapOf(
                    "id" to notificationId,
                    "type" to it.questionnaireType.name
                ))
            }

            notifications.removeAll { it.id == notificationId }
            saveNotifications(notifications)
        }
    }

    /**
     * Obtener cantidad de notificaciones no leídas
     */
    fun getUnreadCount(): Int {
        return getNotifications().count { !it.isRead && !it.isCompleted }
    }

    /**
     * Limpiar notificaciones antiguas completadas (30+ días)
     */
    fun cleanupOldNotifications() {
        synchronized(lock) {
            val notifications = getNotifications()
            val thirtyDaysAgo = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(30)
            val initialCount = notifications.size

            val filtered = notifications.filter {
                !it.isCompleted || it.createdAt > thirtyDaysAgo
            }

            saveNotifications(filtered)

            logDebug("cleanupOldNotifications", mapOf(
                "removed" to (initialCount - filtered.size),
                "remaining" to filtered.size
            ))
        }
    }

    /**
     * Limpiar todas las notificaciones leídas
     */
    fun clearReadNotifications() {
        synchronized(lock) {
            val notifications = getNotifications()
            val toCancel = notifications.filter { it.isRead }

            // Cancelar notificaciones push
            toCancel.forEach {
                LocalNotificationScheduler.cancelNotification(it.questionnaireType)
            }

            val filtered = notifications.filter { !it.isRead }
            saveNotifications(filtered)

            logDebug("clearReadNotifications", mapOf(
                "cleared" to toCancel.size,
                "remaining" to filtered.size
            ))
        }
    }

    /**
     * Limpiar TODAS las notificaciones
     */
    fun clearAllNotifications() {
        synchronized(lock) {
            val notifications = getNotifications()

            // Cancelar todas las notificaciones push
            QuestionnaireType.values().forEach {
                LocalNotificationScheduler.cancelNotification(it)
            }

            saveNotifications(emptyList())

            logDebug("clearAllNotifications", mapOf(
                "cleared" to notifications.size
            ))
        }
    }

    /**
     * Obtener gestor de estadísticas
     */
    fun getStatsManager(): QuestionnaireStatsManager = statsManager

    // ============ UTILIDADES PRIVADAS ============

    private fun getPeriodText(days: Int): String = when (days) {
        7 -> "semanal"
        15 -> "quincenal"
        30 -> "mensual"
        else -> "periódico"
    }

    private fun getQuestionnaireInfo(type: QuestionnaireType): QuestionnaireInfo = when (type) {
        QuestionnaireType.ERGONOMIA -> QuestionnaireInfo(
            type = type,
            title = "Ergonomía y Ambiente de Trabajo",
            description = "Evalúa tu espacio de trabajo",
            icon = Icons.Filled.Computer,
            estimatedTime = "8-10 min",
            totalQuestions = 22
        )
        QuestionnaireType.SINTOMAS_MUSCULARES -> QuestionnaireInfo(
            type = type,
            title = "Síntomas Músculo-Esqueléticos",
            description = "Identifica dolores y molestias",
            icon = Icons.Filled.MonitorHeart,
            estimatedTime = "6-8 min",
            totalQuestions = 18
        )
        QuestionnaireType.SINTOMAS_VISUALES -> QuestionnaireInfo(
            type = type,
            title = "Síntomas Visuales",
            description = "Evalúa fatiga ocular",
            icon = Icons.Filled.RemoveRedEye,
            estimatedTime = "4-5 min",
            totalQuestions = 14
        )
        QuestionnaireType.CARGA_TRABAJO -> QuestionnaireInfo(
            type = type,
            title = "Carga de Trabajo",
            description = "Analiza demanda laboral",
            icon = Icons.Filled.Work,
            estimatedTime = "5-7 min",
            totalQuestions = 15
        )
        QuestionnaireType.ESTRES_SALUD_MENTAL -> QuestionnaireInfo(
            type = type,
            title = "Estrés y Salud Mental",
            description = "Identifica niveles de estrés",
            icon = Icons.Filled.Psychology,
            estimatedTime = "7-9 min",
            totalQuestions = 19
        )
        QuestionnaireType.HABITOS_SUENO -> QuestionnaireInfo(
            type = type,
            title = "Hábitos de Sueño",
            description = "Evalúa calidad de descanso",
            icon = Icons.Filled.NightlightRound,
            estimatedTime = "3-4 min",
            totalQuestions = 9
        )
        QuestionnaireType.ACTIVIDAD_FISICA -> QuestionnaireInfo(
            type = type,
            title = "Actividad Física y Nutrición",
            description = "Analiza hábitos de ejercicio",
            icon = Icons.Filled.SportsGymnastics,
            estimatedTime = "4-5 min",
            totalQuestions = 10
        )
        QuestionnaireType.BALANCE_VIDA_TRABAJO -> QuestionnaireInfo(
            type = type,
            title = "Balance Vida-Trabajo",
            description = "Evalúa equilibrio personal",
            icon = Icons.Filled.Scale,
            estimatedTime = "3-4 min",
            totalQuestions = 8
        )
    }

    // ============ LOGGING ============

    private fun logDebug(event: String, data: Map<String, Any>) {
        if (BuildConfig.DEBUG) {
            android.util.Log.d(TAG, """
                ┌────────────────────────────────
                │ Event: $event
                ${data.entries.joinToString("\n") { "│   ${it.key}: ${it.value}" }}
                └────────────────────────────────
            """.trimIndent())
        }
    }

    private fun logWarning(event: String, message: String) {
        android.util.Log.w(TAG, "⚠️ $event: $message")
    }

    private fun logError(event: String, exception: Exception) {
        android.util.Log.e(TAG, "❌ $event", exception)
    }

    private fun formatDate(timestamp: Long): String {
        val sdf = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault())
        return sdf.format(java.util.Date(timestamp))
    }
}