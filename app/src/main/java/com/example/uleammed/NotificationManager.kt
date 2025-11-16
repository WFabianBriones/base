package com.example.uleammed

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.concurrent.TimeUnit

class QuestionnaireNotificationManager(private val context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(
        "questionnaire_notifications",
        Context.MODE_PRIVATE
    )
    private val gson = Gson()
    private val statsManager = QuestionnaireStatsManager(context)
    private val lock = Any()

    companion object {
        private const val KEY_NOTIFICATIONS = "notifications"
        private const val KEY_SCHEDULE_CONFIG = "schedule_config"
        private const val KEY_LAST_CHECK = "last_check"
        private const val TAG = "NotificationManager"
    }

    fun getNotifications(): List<QuestionnaireNotification> {
        val json = prefs.getString(KEY_NOTIFICATIONS, null)

        android.util.Log.d(TAG, """
        📂 Leyendo notificaciones de SharedPreferences
        - JSON existe: ${json != null}
        - Tamaño JSON: ${json?.length ?: 0} caracteres
    """.trimIndent())

        if (json == null) {
            android.util.Log.w(TAG, "⚠️ No hay notificaciones guardadas en SharedPreferences")
            return emptyList()
        }

        return try {
            val type = object : TypeToken<List<QuestionnaireNotification>>() {}.type
            val notifications = gson.fromJson<List<QuestionnaireNotification>>(json, type)

            android.util.Log.d(TAG, """
            ✅ Notificaciones parseadas
            - Total: ${notifications.size}
            - No leídas: ${notifications.count { !it.isRead }}
        """.trimIndent())

            notifications
        } catch (e: Exception) {
            logError("getNotifications", e)
            emptyList()
        }
    }

    private fun saveNotifications(notifications: List<QuestionnaireNotification>) {
        try {
            val json = gson.toJson(notifications)
            val success = prefs.edit().putString(KEY_NOTIFICATIONS, json).commit() // ✅ Usar commit() en vez de apply()

            android.util.Log.d(TAG, """
            💾 Guardando notificaciones
            - Total: ${notifications.size}
            - No leídas: ${notifications.count { !it.isRead }}
            - Guardado exitoso: $success
            - Tamaño JSON: ${json.length} caracteres
        """.trimIndent())

            if (!success) {
                android.util.Log.e(TAG, "❌ ERROR: No se pudo guardar en SharedPreferences")
            }
        } catch (e: Exception) {
            logError("saveNotifications", e)
        }
    }

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
            checkAndGenerateNotifications(userId)
        }
    }

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
            checkAndGenerateNotifications(userId)
        }
    }

    fun markQuestionnaireCompleted(userId: String, questionnaireType: QuestionnaireType) {
        synchronized(lock) {
            val config = getScheduleConfig(userId)
            val now = System.currentTimeMillis()
            val updatedDates = config.lastCompletedDates.toMutableMap()

            val previousCompleted = updatedDates[questionnaireType.name] ?: 0L
            val dueDate = if (previousCompleted > 0L) {
                calculateNextDueDate(previousCompleted, config.periodDays, config.preferredHour, config.preferredMinute)
            } else {
                now
            }

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

            val notifications = getNotifications().toMutableList()
            val removedCount = notifications.count { it.questionnaireType == questionnaireType }
            notifications.removeAll { it.questionnaireType == questionnaireType }
            saveNotifications(notifications)

            val nextDueDate = calculateNextDueDate(now, config.periodDays, config.preferredHour, config.preferredMinute)

            logDebug("markQuestionnaireCompleted", mapOf(
                "type" to questionnaireType.name,
                "completedAt" to formatDate(now),
                "nextDueDate" to formatDate(nextDueDate),
                "daysUntilNext" to TimeUnit.MILLISECONDS.toDays(nextDueDate - now),
                "removedNotifications" to removedCount
            ))

            if (config.periodDays > 1) {
                val reminderDate = nextDueDate - TimeUnit.DAYS.toMillis(1)
                if (reminderDate > now) {
                    LocalNotificationScheduler.scheduleNotification(
                        questionnaireType = questionnaireType,
                        dueDate = reminderDate,
                        title = "📅 Recordatorio: ${getQuestionnaireInfo(questionnaireType).title}",
                        message = "Mañana es el día de completar tu cuestionario ${getPeriodText(config.periodDays)}. ¡Prepárate!",
                        isReminder = true,
                        createInAppNotification = config.showRemindersInApp
                    )
                    logDebug("scheduleReminder", mapOf(
                        "type" to questionnaireType.name,
                        "reminderDate" to formatDate(reminderDate),
                        "daysUntilReminder" to TimeUnit.MILLISECONDS.toDays(reminderDate - now)
                    ))
                }
            }

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

            checkAndGenerateNotifications(userId)
        }
    }

    /**
     * ✅ CORREGIDO: Generar TODOS los 8 cuestionarios cuando completa el inicial
     */
    fun checkAndGenerateNotifications(userId: String) {
        synchronized(lock) {
            val config = getScheduleConfig(userId)
            val currentNotifications = getNotifications().toMutableList()
            val now = System.currentTimeMillis()
            var generatedCount = 0

            logDebug("checkAndGenerateNotifications", mapOf(
                "userId" to userId,
                "existingNotifications" to currentNotifications.size,
                "completedCount" to config.lastCompletedDates.size
            ))

            val hasCompletedAny = config.lastCompletedDates.isNotEmpty()

            QuestionnaireType.values().forEach { type ->
                if (!config.enabledQuestionnaires.contains(type.name)) {
                    logDebug("skipDisabled", mapOf("type" to type.name))
                    return@forEach
                }

                val existingNotification = currentNotifications.find {
                    it.questionnaireType == type && !it.isCompleted
                }

                if (existingNotification != null) {
                    logDebug("skipExisting", mapOf(
                        "type" to type.name,
                        "existingId" to existingNotification.id
                    ))
                    return@forEach
                }

                val lastCompleted = config.lastCompletedDates[type.name] ?: 0L

                val shouldShow = if (lastCompleted > 0L) {
                    val nextDueDate = calculateNextDueDate(
                        lastCompleted,
                        config.periodDays,
                        config.preferredHour,
                        config.preferredMinute
                    )
                    now >= nextDueDate
                } else {
                    hasCompletedAny
                }

                if (shouldShow) {
                    val nextDueDate = if (lastCompleted > 0L) {
                        calculateNextDueDate(
                            lastCompleted,
                            config.periodDays,
                            config.preferredHour,
                            config.preferredMinute
                        )
                    } else {
                        now
                    }

                    val notification = createNotification(
                        type = type,
                        periodDays = config.periodDays,
                        dueDate = nextDueDate,
                        isFirstTime = lastCompleted == 0L
                    )
                    currentNotifications.add(notification)
                    generatedCount++

                    logDebug("✅ notificationGenerated", mapOf(
                        "type" to type.name,
                        "dueDate" to formatDate(nextDueDate),
                        "isFirstTime" to (lastCompleted == 0L),
                        "isAvailableNow" to (nextDueDate <= now),
                        "reason" to if (lastCompleted == 0L) "Primera vez - disponible ahora" else "Período vencido"
                    ))
                } else {
                    if (lastCompleted > 0L) {
                        val nextDueDate = calculateNextDueDate(
                            lastCompleted,
                            config.periodDays,
                            config.preferredHour,
                            config.preferredMinute
                        )
                        val daysRemaining = TimeUnit.MILLISECONDS.toDays(nextDueDate - now)
                        logDebug("notificationNotDue", mapOf(
                            "type" to type.name,
                            "daysRemaining" to daysRemaining,
                            "nextDueDate" to formatDate(nextDueDate)
                        ))
                    }
                }
            }

            prefs.edit().putLong(KEY_LAST_CHECK, now).apply()
            saveNotifications(currentNotifications)

            logDebug("✅ checkComplete", mapOf(
                "generatedCount" to generatedCount,
                "totalNotifications" to currentNotifications.size,
                "unreadCount" to currentNotifications.count { !it.isRead },
                "hasCompletedAny" to hasCompletedAny
            ))
        }
    }

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

    private fun createNotification(
        type: QuestionnaireType,
        periodDays: Int,
        dueDate: Long,
        isFirstTime: Boolean = false
    ): QuestionnaireNotification {
        val info = getQuestionnaireInfo(type)

        val (title, message) = if (isFirstTime) {
            "🆕 Cuestionario pendiente: ${info.title}" to
                    "Completa este cuestionario para establecer tu línea base de salud. ${info.estimatedTime}"
        } else {
            "⏰ Cuestionario ${getPeriodText(periodDays)}: ${info.title}" to
                    "Es momento de completar tu cuestionario ${getPeriodText(periodDays)}. ${info.estimatedTime}"
        }

        return QuestionnaireNotification(
            questionnaireType = type,
            title = title,
            message = message,
            dueDate = dueDate,
            createdAt = System.currentTimeMillis()
        )
    }

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

    fun markAsReadByType(userId: String, questionnaireType: QuestionnaireType) {
        synchronized(lock) {
            val notifications = getNotifications().toMutableList()
            var markedCount = 0

            notifications.forEachIndexed { index, notification ->
                if (notification.questionnaireType == questionnaireType &&
                    !notification.isRead &&
                    !notification.isCompleted) {
                    notifications[index] = notification.copy(isRead = true)
                    markedCount++
                }
            }

            if (markedCount > 0) {
                saveNotifications(notifications)
                logDebug("markAsReadByType", mapOf(
                    "userId" to userId,
                    "type" to questionnaireType.name,
                    "markedCount" to markedCount
                ))
            }
        }
    }

    fun deleteNotification(notificationId: String) {
        synchronized(lock) {
            val notifications = getNotifications().toMutableList()
            val notification = notifications.find { it.id == notificationId }
            notification?.let {
                LocalNotificationScheduler.cancelNotification(it.questionnaireType)
            }
            notifications.removeAll { it.id == notificationId }
            saveNotifications(notifications)
        }
    }

    fun getUnreadCount(): Int {
        return getNotifications().count { !it.isRead && !it.isCompleted }
    }

    fun cleanupOldNotifications() {
        synchronized(lock) {
            val notifications = getNotifications()
            val thirtyDaysAgo = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(30)
            val filtered = notifications.filter {
                !it.isCompleted || it.createdAt > thirtyDaysAgo
            }
            saveNotifications(filtered)
        }
    }

    fun clearReadNotifications() {
        synchronized(lock) {
            val notifications = getNotifications()
            notifications.filter { it.isRead }.forEach {
                LocalNotificationScheduler.cancelNotification(it.questionnaireType)
            }
            val filtered = notifications.filter { !it.isRead }
            saveNotifications(filtered)
        }
    }

    fun clearAllNotifications() {
        synchronized(lock) {
            QuestionnaireType.values().forEach {
                LocalNotificationScheduler.cancelNotification(it)
            }
            saveNotifications(emptyList())
        }
    }

    fun getStatsManager(): QuestionnaireStatsManager = statsManager

    private fun getPeriodText(days: Int): String = when (days) {
        7 -> "semanal"
        15 -> "quincenal"
        30 -> "mensual"
        else -> "periódico"
    }

    private fun getQuestionnaireInfo(type: QuestionnaireType): QuestionnaireInfo = when (type) {
        QuestionnaireType.ERGONOMIA -> QuestionnaireInfo(type, "Ergonomía y Ambiente de Trabajo", "Evalúa tu espacio de trabajo", Icons.Filled.Computer, "8-10 min", 22)
        QuestionnaireType.SINTOMAS_MUSCULARES -> QuestionnaireInfo(type, "Síntomas Músculo-Esqueléticos", "Identifica dolores y molestias", Icons.Filled.MonitorHeart, "6-8 min", 18)
        QuestionnaireType.SINTOMAS_VISUALES -> QuestionnaireInfo(type, "Síntomas Visuales", "Evalúa fatiga ocular", Icons.Filled.RemoveRedEye, "4-5 min", 14)
        QuestionnaireType.CARGA_TRABAJO -> QuestionnaireInfo(type, "Carga de Trabajo", "Analiza demanda laboral", Icons.Filled.Work, "5-7 min", 15)
        QuestionnaireType.ESTRES_SALUD_MENTAL -> QuestionnaireInfo(type, "Estrés y Salud Mental", "Identifica niveles de estrés", Icons.Filled.Psychology, "7-9 min", 19)
        QuestionnaireType.HABITOS_SUENO -> QuestionnaireInfo(type, "Hábitos de Sueño", "Evalúa calidad de descanso", Icons.Filled.NightlightRound, "3-4 min", 9)
        QuestionnaireType.ACTIVIDAD_FISICA -> QuestionnaireInfo(type, "Actividad Física y Nutrición", "Analiza hábitos de ejercicio", Icons.Filled.SportsGymnastics, "4-5 min", 10)
        QuestionnaireType.BALANCE_VIDA_TRABAJO -> QuestionnaireInfo(type, "Balance Vida-Trabajo", "Evalúa equilibrio personal", Icons.Filled.Scale, "3-4 min", 8)
    }

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