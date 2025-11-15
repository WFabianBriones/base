package com.example.uleammed

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.concurrent.TimeUnit

class QuestionnaireNotificationManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(
        "questionnaire_notifications",
        Context.MODE_PRIVATE
    )
    private val gson = Gson()
    private val statsManager = QuestionnaireStatsManager(context) // ✅ NUEVO

    companion object {
        private const val KEY_NOTIFICATIONS = "notifications"
        private const val KEY_SCHEDULE_CONFIG = "schedule_config"
        private const val KEY_LAST_CHECK = "last_check"
    }

    // Obtener todas las notificaciones
    fun getNotifications(): List<QuestionnaireNotification> {
        val json = prefs.getString(KEY_NOTIFICATIONS, null) ?: return emptyList()
        val type = object : TypeToken<List<QuestionnaireNotification>>() {}.type
        return gson.fromJson(json, type)
    }

    // Guardar notificaciones
    private fun saveNotifications(notifications: List<QuestionnaireNotification>) {
        val json = gson.toJson(notifications)
        prefs.edit().putString(KEY_NOTIFICATIONS, json).apply()
    }

    // Obtener configuración de periodicidad
    fun getScheduleConfig(userId: String): QuestionnaireScheduleConfig {
        val json = prefs.getString("${KEY_SCHEDULE_CONFIG}_$userId", null)
        return if (json != null) {
            gson.fromJson(json, QuestionnaireScheduleConfig::class.java)
        } else {
            QuestionnaireScheduleConfig(userId = userId)
        }
    }

    // Guardar configuración de periodicidad
    fun saveScheduleConfig(config: QuestionnaireScheduleConfig) {
        val json = gson.toJson(config)
        prefs.edit().putString("${KEY_SCHEDULE_CONFIG}_${config.userId}", json).apply()
    }

    // Actualizar período de días
    fun updatePeriodDays(userId: String, days: Int) {
        val config = getScheduleConfig(userId)
        val updatedConfig = config.copy(periodDays = days)
        saveScheduleConfig(updatedConfig)

        // Regenerar notificaciones con el nuevo período
        checkAndGenerateNotifications(userId)
    }

    // Marcar cuestionario como completado
    fun markQuestionnaireCompleted(userId: String, questionnaireType: QuestionnaireType) {
        val config = getScheduleConfig(userId)
        val now = System.currentTimeMillis()
        val updatedDates = config.lastCompletedDates.toMutableMap()

        // ✅ NUEVO: Calcular fecha de vencimiento (si existe) para estadísticas
        val previousCompleted = updatedDates[questionnaireType.name] ?: 0L
        val dueDate = if (previousCompleted > 0L) {
            previousCompleted + TimeUnit.DAYS.toMillis(config.periodDays.toLong())
        } else {
            now // Primera vez, se considera a tiempo
        }

        // ✅ NUEVO: Registrar en estadísticas
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

        // ✅ CORRECCIÓN: Eliminar TODAS las notificaciones de este tipo (completadas o no)
        val notifications = getNotifications().toMutableList()
        notifications.removeAll { it.questionnaireType == questionnaireType }

        // ✅ Calcular la próxima fecha de vencimiento
        val nextDueDate = now + TimeUnit.DAYS.toMillis(config.periodDays.toLong())

        // 🔍 DEBUG: Imprimir fechas para verificación
        android.util.Log.d("NotificationManager", """
            ============ CUESTIONARIO COMPLETADO ============
            Tipo: $questionnaireType
            Fecha completado: ${java.util.Date(now)}
            Periodicidad: ${config.periodDays} días
            Próxima notificación: ${java.util.Date(nextDueDate)}
            Días hasta próxima: ${TimeUnit.MILLISECONDS.toDays(nextDueDate - now)} días
            ============================================
        """.trimIndent())

        // ✅ NUEVO: Programar recordatorio 1 día antes (si la periodicidad es mayor a 1 día)
        if (config.periodDays > 1) {
            val reminderDate = nextDueDate - TimeUnit.DAYS.toMillis(1)

            android.util.Log.d("NotificationManager", """
                ============ PROGRAMANDO RECORDATORIO PREVIO ============
                Fecha recordatorio: ${java.util.Date(reminderDate)}
                Días hasta recordatorio: ${TimeUnit.MILLISECONDS.toDays(reminderDate - now)} días
            """.trimIndent())

            LocalNotificationScheduler.scheduleNotification(
                questionnaireType = questionnaireType,
                dueDate = reminderDate,
                title = "📅 Recordatorio: ${getQuestionnaireInfo(questionnaireType).title}",
                message = "Mañana es el día de completar tu cuestionario ${getPeriodText(config.periodDays)}. ¡Prepárate!",
                isReminder = true // Flag para identificar que es recordatorio
            )
        }

        // ✅ NO crear notificación en la app todavía
        // Solo programar la notificación push que se disparará en la fecha futura
        saveNotifications(notifications)

        // Programar notificación push principal
        LocalNotificationScheduler.scheduleNotification(
            questionnaireType = questionnaireType,
            dueDate = nextDueDate,
            title = "⏰ Cuestionario pendiente: ${getQuestionnaireInfo(questionnaireType).title}",
            message = "Es momento de completar tu cuestionario ${getPeriodText(config.periodDays)}.",
            isReminder = false
        )
    }

    // Verificar y generar notificaciones pendientes
    fun checkAndGenerateNotifications(userId: String) {
        val config = getScheduleConfig(userId)
        val currentNotifications = getNotifications().toMutableList()
        val now = System.currentTimeMillis()

        QuestionnaireType.values().forEach { type ->
            // Verificar si está habilitado
            if (!config.enabledQuestionnaires.contains(type.name)) {
                return@forEach
            }

            // Verificar si ya existe una notificación activa para este tipo
            val existingNotification = currentNotifications.find {
                it.questionnaireType == type && !it.isCompleted
            }

            if (existingNotification == null) {
                // Obtener última fecha de completado
                val lastCompleted = config.lastCompletedDates[type.name] ?: 0L

                // ✅ Solo si ya se completó al menos una vez
                if (lastCompleted > 0L) {
                    val nextDueDate = lastCompleted + TimeUnit.DAYS.toMillis(config.periodDays.toLong())

                    // ✅ CAMBIO CRÍTICO: Solo crear notificación si YA LLEGÓ la fecha de vencimiento
                    if (now >= nextDueDate) {
                        android.util.Log.d("NotificationManager", """
                            ============ GENERANDO NOTIFICACIÓN ============
                            Tipo: $type
                            Última completada: ${java.util.Date(lastCompleted)}
                            Fecha vencimiento: ${java.util.Date(nextDueDate)}
                            Fecha actual: ${java.util.Date(now)}
                            Estado: VENCIDA - Creando notificación
                            ============================================
                        """.trimIndent())

                        val notification = createNotification(
                            type = type,
                            periodDays = config.periodDays,
                            dueDate = nextDueDate
                        )
                        currentNotifications.add(notification)
                    } else {
                        // ✅ Aún no es tiempo, solo log
                        val daysRemaining = TimeUnit.MILLISECONDS.toDays(nextDueDate - now)
                        android.util.Log.d("NotificationManager", """
                            ============ VERIFICANDO NOTIFICACIÓN ============
                            Tipo: $type
                            Última completada: ${java.util.Date(lastCompleted)}
                            Próxima fecha: ${java.util.Date(nextDueDate)}
                            Días restantes: $daysRemaining días
                            Estado: AÚN NO ES TIEMPO
                            ============================================
                        """.trimIndent())
                    }
                }
            }
        }

        // Actualizar última verificación
        prefs.edit().putLong(KEY_LAST_CHECK, now).apply()
        saveNotifications(currentNotifications)
    }

    // ✅ CORRECCIÓN: Crear notificación con fecha de vencimiento específica
    private fun createNotification(
        type: QuestionnaireType,
        periodDays: Int,
        dueDate: Long // ✅ Recibe la fecha de vencimiento
    ): QuestionnaireNotification {
        val info = getQuestionnaireInfo(type)
        return QuestionnaireNotification(
            questionnaireType = type,
            title = "Cuestionario pendiente: ${info.title}",
            message = "Es momento de completar tu cuestionario ${getPeriodText(periodDays)}. ${info.estimatedTime}",
            dueDate = dueDate, // ✅ Usa la fecha de vencimiento proporcionada
            createdAt = System.currentTimeMillis() // Fecha de creación es ahora
        )
    }

    // Marcar notificación como leída
    fun markAsRead(notificationId: String) {
        val notifications = getNotifications().toMutableList()
        val index = notifications.indexOfFirst { it.id == notificationId }
        if (index != -1) {
            notifications[index] = notifications[index].copy(isRead = true)
            saveNotifications(notifications)
        }
    }

    // Eliminar notificación
    fun deleteNotification(notificationId: String) {
        val notifications = getNotifications().toMutableList()
        val notification = notifications.find { it.id == notificationId }

        // Cancelar notificación push si existe
        notification?.let {
            LocalNotificationScheduler.cancelNotification(it.questionnaireType)
        }

        notifications.removeAll { it.id == notificationId }
        saveNotifications(notifications)
    }

    // Obtener notificaciones no leídas
    fun getUnreadCount(): Int {
        return getNotifications().count { !it.isRead && !it.isCompleted }
    }

    // Limpiar notificaciones antiguas completadas
    fun cleanupOldNotifications() {
        val notifications = getNotifications()
        val thirtyDaysAgo = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(30)
        val filtered = notifications.filter {
            !it.isCompleted || it.createdAt > thirtyDaysAgo
        }
        saveNotifications(filtered)
    }

    // Limpiar todas las notificaciones leídas
    fun clearReadNotifications() {
        val notifications = getNotifications()
        val toCancel = notifications.filter { it.isRead }

        // Cancelar notificaciones push
        toCancel.forEach {
            LocalNotificationScheduler.cancelNotification(it.questionnaireType)
        }

        val filtered = notifications.filter { !it.isRead }
        saveNotifications(filtered)
    }

    // Limpiar TODAS las notificaciones
    fun clearAllNotifications() {
        // Cancelar todas las notificaciones push
        QuestionnaireType.values().forEach {
            LocalNotificationScheduler.cancelNotification(it)
        }

        saveNotifications(emptyList())
    }

    // ✅ NUEVO: Obtener gestor de estadísticas
    fun getStatsManager(): QuestionnaireStatsManager {
        return statsManager
    }

    private fun getPeriodText(days: Int): String {
        return when (days) {
            7 -> "semanal"
            15 -> "quincenal"
            30 -> "mensual"
            else -> "periódico"
        }
    }

    private fun getQuestionnaireInfo(type: QuestionnaireType): QuestionnaireInfo {
        return when (type) {
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
    }
}