package com.example.uleammed.notifications

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import com.example.uleammed.BuildConfig
import com.example.uleammed.questionnaires.QuestionnaireInfo
import com.example.uleammed.questionnaires.QuestionnaireType
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

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

        Log.d(TAG, """
        📂 Leyendo notificaciones de SharedPreferences
        - JSON existe: ${json != null}
        - Tamaño JSON: ${json?.length ?: 0} caracteres
    """.trimIndent())

        if (json == null) {
            Log.w(TAG, "⚠️ No hay notificaciones guardadas en SharedPreferences")
            return emptyList()
        }

        return try {
            val type = object : TypeToken<List<QuestionnaireNotification>>() {}.type
            val notifications = gson.fromJson<List<QuestionnaireNotification>>(json, type)

            Log.d(TAG, """
            ✅ Notificaciones parseadas
            - Total: ${notifications.size}
            - No leídas: ${notifications.count { !it.isRead }}
            - Pendientes (no completadas): ${notifications.count { !it.isCompleted }}
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
            val success = prefs.edit().putString(KEY_NOTIFICATIONS, json).commit()

            Log.d(TAG, """
            💾 Guardando notificaciones
            - Total: ${notifications.size}
            - No leídas: ${notifications.count { !it.isRead }}
            - Pendientes: ${notifications.count { !it.isCompleted }}
            - Guardado exitoso: $success
        """.trimIndent())

            if (!success) {
                Log.e(TAG, "❌ ERROR: No se pudo guardar en SharedPreferences")
            }
        } catch (e: Exception) {
            logError("saveNotifications", e)
        }
    }

    fun getScheduleConfig(userId: String): QuestionnaireScheduleConfig {
        val json = prefs.getString("${KEY_SCHEDULE_CONFIG}_$userId", null)
        val config = if (json != null) {
            try {
                val loadedConfig = gson.fromJson(json, QuestionnaireScheduleConfig::class.java)

                // ✅ MIGRACIÓN AUTOMÁTICA: Sincronizar con todos los tipos del enum
                val allTypes = QuestionnaireType.values().map { it.name }.toSet()
                val needsMigration = loadedConfig.enabledQuestionnaires != allTypes

                if (needsMigration) {
                    val newTypes = allTypes - loadedConfig.enabledQuestionnaires
                    Log.d(TAG, """
                    🔄 Migrando configuración automáticamente
                    - Tipos anteriores: ${loadedConfig.enabledQuestionnaires.size}
                    - Tipos actuales: ${allTypes.size}
                    - Nuevos agregados: $newTypes
                """.trimIndent())

                    val migratedConfig = loadedConfig.copy(enabledQuestionnaires = allTypes)
                    // Guardar inmediatamente la versión migrada
                    saveScheduleConfig(migratedConfig)
                    migratedConfig
                } else {
                    loadedConfig
                }
            } catch (e: Exception) {
                logError("getScheduleConfig", e)
                QuestionnaireScheduleConfig(userId = userId)
            }
        } else {
            QuestionnaireScheduleConfig(userId = userId)
        }

        return config
    }

    fun saveScheduleConfig(config: QuestionnaireScheduleConfig) {
        try {
            val json = gson.toJson(config)
            prefs.edit().putString("${KEY_SCHEDULE_CONFIG}_${config.userId}", json).apply()
            logDebug("saveScheduleConfig", mapOf(
                "userId" to config.userId,
                "periodDays" to config.periodDays,
                "saludGeneralPeriodDays" to config.saludGeneralPeriodDays,
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

            // ✅ Reprogramar notificaciones de cuestionarios regulares
            config.lastCompletedDates.forEach { (typeName, completedAt) ->
                val type = QuestionnaireType.valueOf(typeName)

                // ✅ Solo reprogramar cuestionarios regulares, NO salud general
                if (type != QuestionnaireType.SALUD_GENERAL) {
                    val newDueDate = calculateNextDueDate(
                        completedAt,
                        days,
                        config.preferredHour,
                        config.preferredMinute
                    )

                    LocalNotificationScheduler.cancelNotification(type)

                    if (newDueDate > System.currentTimeMillis()) {
                        if (days > 1) {
                            val reminderDate = newDueDate - TimeUnit.DAYS.toMillis(1)
                            LocalNotificationScheduler.scheduleNotification(
                                questionnaireType = type,
                                dueDate = reminderDate,
                                title = "📅 Recordatorio: ...",
                                message = "Mañana es el día...",
                                isReminder = true,
                                createInAppNotification = config.showRemindersInApp
                            )
                        }

                        LocalNotificationScheduler.scheduleNotification(
                            questionnaireType = type,
                            dueDate = newDueDate,
                            title = "⏰ Cuestionario pendiente: ...",
                            message = "Es momento de completar...",
                            isReminder = false,
                            createInAppNotification = true
                        )
                    }
                }
            }

            logDebug("updatePeriodDays", mapOf(
                "userId" to userId,
                "oldPeriod" to config.periodDays,
                "newPeriod" to days,
                "reprogrammedCount" to config.lastCompletedDates.filter {
                    QuestionnaireType.valueOf(it.key) != QuestionnaireType.SALUD_GENERAL
                }.size
            ))

            checkAndGenerateNotifications(userId)
        }
    }

    // ✅ NUEVO: Actualizar período de salud general
    fun updateSaludGeneralPeriodDays(userId: String, days: Int) {
        synchronized(lock) {
            val config = getScheduleConfig(userId)
            val updatedConfig = config.copy(saludGeneralPeriodDays = days)
            saveScheduleConfig(updatedConfig)

            // Reprogramar solo el cuestionario de salud general
            config.lastCompletedDates["SALUD_GENERAL"]?.let { completedAt ->
                val newDueDate = calculateNextDueDate(
                    completedAt,
                    days,
                    config.preferredHour,
                    config.preferredMinute
                )

                LocalNotificationScheduler.cancelNotification(QuestionnaireType.SALUD_GENERAL)

                // ✅ CAMBIO: NO crear notificación in-app, solo notificación push
                if (newDueDate > System.currentTimeMillis()) {
                    if (days > 1) {
                        val reminderDate = newDueDate - TimeUnit.DAYS.toMillis(1)
                        LocalNotificationScheduler.scheduleNotification(
                            questionnaireType = QuestionnaireType.SALUD_GENERAL,
                            dueDate = reminderDate,
                            title = "📅 Recordatorio: Cuestionario de Salud General",
                            message = "Mañana es el día de reevaluar tu salud general",
                            isReminder = true,
                            createInAppNotification = false // ✅ No crear en Avisos
                        )
                    }

                    LocalNotificationScheduler.scheduleNotification(
                        questionnaireType = QuestionnaireType.SALUD_GENERAL,
                        dueDate = newDueDate,
                        title = "⏰ Cuestionario de Salud General",
                        message = "Es momento de reevaluar tu estado de salud base",
                        isReminder = false,
                        createInAppNotification = false // ✅ No crear en Avisos
                    )
                }
            }

            logDebug("updateSaludGeneralPeriodDays", mapOf(
                "userId" to userId,
                "oldPeriod" to config.saludGeneralPeriodDays,
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

            // ✅ CAMBIO: Usar getPeriodForQuestionnaire para obtener el período correcto
            val periodDays = config.getPeriodForQuestionnaire(questionnaireType)

            val dueDate = if (previousCompleted > 0L) {
                calculateNextDueDate(
                    previousCompleted,
                    periodDays,
                    config.preferredHour,
                    config.preferredMinute
                )
            } else {
                now
            }

            statsManager.recordCompletion(
                userId = userId,
                questionnaireType = questionnaireType,
                completedAt = now,
                dueDate = dueDate,
                periodDays = periodDays
            )

            updatedDates[questionnaireType.name] = now
            val updatedConfig = config.copy(lastCompletedDates = updatedDates)
            saveScheduleConfig(updatedConfig)

            // ELIMINAR notificación en lugar de marcarla como completada
            val notifications = getNotifications().toMutableList()
            val notificationToRemove = notifications.find {
                it.questionnaireType == questionnaireType && !it.isCompleted
            }

            if (notificationToRemove != null) {
                notifications.remove(notificationToRemove)
                saveNotifications(notifications)

                Log.d(TAG, """
        🗑️ Notificación eliminada
        - Tipo: ${questionnaireType.name}
        - ID: ${notificationToRemove.id}
        - Total restantes: ${notifications.size}
        - Pendientes: ${notifications.count { !it.isCompleted }}
    """.trimIndent())
            } else {
                Log.w(TAG, "⚠️ No se encontró notificación pendiente para eliminar: ${questionnaireType.name}")
            }

            // ✅ CAMBIO: Usar periodDays ya calculado
            val nextDueDate = calculateNextDueDate(
                now,
                periodDays,
                config.preferredHour,
                config.preferredMinute
            )

            logDebug("markQuestionnaireCompleted", mapOf(
                "type" to questionnaireType.name,
                "completedAt" to formatDate(now),
                "periodDays" to periodDays,
                "nextDueDate" to formatDate(nextDueDate),
                "daysUntilNext" to TimeUnit.MILLISECONDS.toDays(nextDueDate - now)
            ))

            // ✅ CAMBIO: SALUD_GENERAL no crea notificaciones in-app
            val createInAppNotification = questionnaireType != QuestionnaireType.SALUD_GENERAL

            // ✅ CAMBIO: Usar periodDays para programar notificaciones
            if (periodDays > 1) {
                val reminderDate = nextDueDate - TimeUnit.DAYS.toMillis(1)
                if (reminderDate > now) {
                    LocalNotificationScheduler.scheduleNotification(
                        questionnaireType = questionnaireType,
                        dueDate = reminderDate,
                        title = "📅 Recordatorio: ${getQuestionnaireInfo(questionnaireType).title}",
                        message = "Mañana es el día de completar tu cuestionario ${getPeriodText(periodDays)}. ¡Prepárate!",
                        isReminder = true,
                        createInAppNotification = if (questionnaireType == QuestionnaireType.SALUD_GENERAL) false else config.showRemindersInApp
                    )
                    logDebug("scheduleReminder", mapOf(
                        "type" to questionnaireType.name,
                        "reminderDate" to formatDate(reminderDate),
                        "daysUntilReminder" to TimeUnit.MILLISECONDS.toDays(reminderDate - now),
                        "createInApp" to createInAppNotification
                    ))
                }
            }

            if (nextDueDate > now) {
                LocalNotificationScheduler.scheduleNotification(
                    questionnaireType = questionnaireType,
                    dueDate = nextDueDate,
                    title = "⏰ Cuestionario pendiente: ${getQuestionnaireInfo(questionnaireType).title}",
                    message = "Es momento de completar tu cuestionario ${getPeriodText(periodDays)}.",
                    isReminder = false,
                    createInAppNotification = createInAppNotification
                )
            } else {
                logWarning("markQuestionnaireCompleted", "Fecha de vencimiento en el pasado ignorada")
            }

            checkAndGenerateNotifications(userId)
        }
    }

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

            QuestionnaireType.values().forEach { type ->
                // ✅ SKIP SALUD_GENERAL - Se maneja como dialog automático obligatorio
                if (type == QuestionnaireType.SALUD_GENERAL) {
                    logDebug("skipSaludGeneral", mapOf(
                        "reason" to "Se muestra como dialog obligatorio, no como notificación en Avisos"
                    ))
                    return@forEach
                }

                if (!config.enabledQuestionnaires.contains(type.name)) {
                    logDebug("skipDisabled", mapOf("type" to type.name))
                    return@forEach
                }

                // Buscar notificaciones NO completadas
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

                // ✅ CAMBIO CRÍTICO: Usar getPeriodForQuestionnaire
                val periodDays = config.getPeriodForQuestionnaire(type)

                val shouldShow = if (lastCompleted > 0L) {
                    val nextDueDate = calculateNextDueDate(
                        lastCompleted,
                        periodDays,
                        config.preferredHour,
                        config.preferredMinute
                    )
                    now >= nextDueDate
                } else {
                    true
                }

                if (shouldShow) {
                    val nextDueDate = if (lastCompleted > 0L) {
                        calculateNextDueDate(
                            lastCompleted,
                            periodDays,
                            config.preferredHour,
                            config.preferredMinute
                        )
                    } else {
                        now
                    }

                    val notification = createNotification(
                        type = type,
                        periodDays = periodDays,
                        dueDate = nextDueDate,
                        isFirstTime = lastCompleted == 0L
                    )
                    currentNotifications.add(notification)
                    generatedCount++

                    logDebug("✅ notificationGenerated", mapOf(
                        "type" to type.name,
                        "periodDays" to periodDays,
                        "dueDate" to formatDate(nextDueDate),
                        "isFirstTime" to (lastCompleted == 0L),
                        "isAvailableNow" to (nextDueDate <= now),
                        "reason" to if (lastCompleted == 0L) "Primera vez - disponible ahora" else "Período vencido"
                    ))
                } else {
                    if (lastCompleted > 0L) {
                        val nextDueDate = calculateNextDueDate(
                            lastCompleted,
                            periodDays,
                            config.preferredHour,
                            config.preferredMinute
                        )
                        val daysRemaining = TimeUnit.MILLISECONDS.toDays(nextDueDate - now)
                        logDebug("notificationNotDue", mapOf(
                            "type" to type.name,
                            "periodDays" to periodDays,
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
                "pendingCount" to currentNotifications.count { !it.isCompleted }
            ))
        }
    }

    /**
     * ✅ MODIFICADO: Verifica si el cuestionario de salud general está pendiente
     * Ahora revisa Firebase para obtener la fecha de última completación
     *
     * @return true si debe mostrarse el dialog ahora, false si aún no
     */
    suspend fun shouldShowSaludGeneralDialog(userId: String): Boolean {
        // ✅ AGREGAR ESTE LOG AL INICIO
        Log.d(TAG, """
        🔍 Verificando dialog de Salud General
        - userId: $userId
    """.trimIndent())

        val config = getScheduleConfig(userId)

        // ✅ NUEVO: Obtener última completación desde Firebase
        val lastCompleted = try {
            val firestore = FirebaseFirestore.getInstance()
            val doc = firestore.collection("users")
                .document(userId)
                .collection("questionnaires")
                .document("salud_general")
                .get()
                .await()

            if (doc.exists()) {
                doc.getLong("completedAt") ?: 0L
            } else {
                0L
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error obteniendo salud_general de Firebase", e)
            0L
        }

        // ✅ AGREGAR LOG DESPUÉS DE OBTENER DE FIREBASE
        Log.d(TAG, """
        📊 Estado de Salud General
        - lastCompleted: ${if (lastCompleted > 0) formatDate(lastCompleted) else "Nunca completado"}
        - Firebase doc existe: ${lastCompleted > 0}
        - periodDays: ${config.saludGeneralPeriodDays}
    """.trimIndent())

        if (lastCompleted == 0L) {
            Log.d(TAG, "⏭️ Primera vez - no mostrar dialog (se maneja en onboarding)")
            return false
        }

        val periodDays = config.saludGeneralPeriodDays
        val nextDueDate = calculateNextDueDate(
            lastCompleted,
            periodDays,
            config.preferredHour,
            config.preferredMinute
        )

        val now = System.currentTimeMillis()
        val shouldShow = now >= nextDueDate

        Log.d(TAG, """
        🎯 Decisión final
        - shouldShow: $shouldShow
        - nextDueDate: ${formatDate(nextDueDate)}
        - daysOverdue: ${TimeUnit.MILLISECONDS.toDays(now - nextDueDate)}
    """.trimIndent())

        if (shouldShow) {
            logDebug("saludGeneralDue", mapOf(
                "lastCompleted" to formatDate(lastCompleted),
                "periodDays" to periodDays,
                "nextDueDate" to formatDate(nextDueDate),
                "daysOverdue" to TimeUnit.MILLISECONDS.toDays(now - nextDueDate)
            ))
        }

        return shouldShow
    }

    private fun calculateNextDueDate(
        lastCompleted: Long,
        periodDays: Int,
        preferredHour: Int,
        preferredMinute: Int
    ): Long {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = lastCompleted
            add(Calendar.DAY_OF_MONTH, periodDays)
            set(Calendar.HOUR_OF_DAY, preferredHour)
            set(Calendar.MINUTE, preferredMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
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
        val notifications = getNotifications()
        val count = notifications.count { !it.isCompleted }

        Log.d(TAG, """
            📊 Badge Count (PERSISTENTE)
            - Total pendientes: $count
            - Fuente: SharedPreferences (sobrevive al cierre de app)
            - Lógica: Cuenta notificaciones NO completadas
            - Detalle:
              * Total notificaciones: ${notifications.size}
              * Completadas: ${notifications.count { it.isCompleted }}
              * Pendientes: $count
        """.trimIndent())

        return count
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
            notifications.filter { it.isRead && it.isCompleted }.forEach {
                LocalNotificationScheduler.cancelNotification(it.questionnaireType)
            }
            val filtered = notifications.filter { !(it.isRead && it.isCompleted) }
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
        90 -> "trimestral"
        180 -> "semestral"
        else -> "periódico"
    }

    private fun getQuestionnaireInfo(type: QuestionnaireType): QuestionnaireInfo = when (type) {
        // ✅ NUEVO: Cuestionario de Salud General
        QuestionnaireType.SALUD_GENERAL -> QuestionnaireInfo(
            type,
            "Cuestionario de Salud General",
            "Reevaluación de tu estado de salud base",
            Icons.Filled.HealthAndSafety,
            "5-7 min",
            21
        )
        QuestionnaireType.ERGONOMIA -> QuestionnaireInfo(
            type,
            "Ergonomía y Ambiente de Trabajo",
            "Evalúa tu espacio de trabajo",
            Icons.Filled.Computer,
            "8-10 min",
            22
        )
        QuestionnaireType.SINTOMAS_MUSCULARES -> QuestionnaireInfo(
            type,
            "Síntomas Músculo-Esqueléticos",
            "Identifica dolores y molestias",
            Icons.Filled.MonitorHeart,
            "6-8 min",
            18
        )
        QuestionnaireType.SINTOMAS_VISUALES -> QuestionnaireInfo(
            type,
            "Síntomas Visuales",
            "Evalúa fatiga ocular",
            Icons.Filled.RemoveRedEye,
            "4-5 min",
            14
        )
        QuestionnaireType.CARGA_TRABAJO -> QuestionnaireInfo(
            type,
            "Carga de Trabajo",
            "Analiza demanda laboral",
            Icons.Filled.Work,
            "5-7 min",
            15
        )
        QuestionnaireType.ESTRES_SALUD_MENTAL -> QuestionnaireInfo(
            type,
            "Estrés y Salud Mental",
            "Identifica niveles de estrés",
            Icons.Filled.Psychology,
            "7-9 min",
            19
        )
        QuestionnaireType.HABITOS_SUENO -> QuestionnaireInfo(
            type,
            "Hábitos de Sueño",
            "Evalúa calidad de descanso",
            Icons.Filled.NightlightRound,
            "3-4 min",
            9
        )
        QuestionnaireType.ACTIVIDAD_FISICA -> QuestionnaireInfo(
            type,
            "Actividad Física y Nutrición",
            "Analiza hábitos de ejercicio",
            Icons.Filled.SportsGymnastics,
            "4-5 min",
            10
        )
        QuestionnaireType.BALANCE_VIDA_TRABAJO -> QuestionnaireInfo(
            type,
            "Balance Vida-Trabajo",
            "Evalúa equilibrio personal",
            Icons.Filled.Scale,
            "3-4 min",
            8
        )
    }

    private fun logDebug(event: String, data: Map<String, Any>) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, """
                ┌────────────────────────────────
                │ Event: $event
                ${data.entries.joinToString("\n") { "│   ${it.key}: ${it.value}" }}
                └────────────────────────────────
            """.trimIndent())
        }
    }

    private fun logWarning(event: String, message: String) {
        Log.w(TAG, "⚠️ $event: $message")
    }

    private fun logError(event: String, exception: Exception) {
        Log.e(TAG, "❌ $event", exception)
    }

    private fun formatDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
}