package com.example.uleammed

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.concurrent.TimeUnit

class QuestionnaireNotificationManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(
        "questionnaire_notifications",
        Context.MODE_PRIVATE
    )
    private val gson = Gson()

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
        val updatedDates = config.lastCompletedDates.toMutableMap()
        updatedDates[questionnaireType.name] = System.currentTimeMillis()

        val updatedConfig = config.copy(lastCompletedDates = updatedDates)
        saveScheduleConfig(updatedConfig)

        // Eliminar notificaciones de este tipo y crear la siguiente
        val notifications = getNotifications().toMutableList()
        notifications.removeAll { it.questionnaireType == questionnaireType }

        // Crear siguiente notificación
        val nextNotification = createNotification(questionnaireType, config.periodDays)
        notifications.add(nextNotification)

        saveNotifications(notifications)
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

            // Verificar si ya existe una notificación para este tipo
            val existingNotification = currentNotifications.find {
                it.questionnaireType == type && !it.isCompleted
            }

            if (existingNotification == null) {
                // Obtener última fecha de completado
                val lastCompleted = config.lastCompletedDates[type.name] ?: 0L
                val daysSinceCompleted = TimeUnit.MILLISECONDS.toDays(now - lastCompleted)

                // Si han pasado suficientes días o nunca se ha completado
                if (lastCompleted == 0L || daysSinceCompleted >= config.periodDays) {
                    val notification = createNotification(type, config.periodDays)
                    currentNotifications.add(notification)
                }
            }
        }

        // Actualizar última verificación
        prefs.edit().putLong(KEY_LAST_CHECK, now).apply()
        saveNotifications(currentNotifications)
    }

    // Crear una notificación
    private fun createNotification(
        type: QuestionnaireType,
        periodDays: Int
    ): QuestionnaireNotification {
        val info = getQuestionnaireInfo(type)
        return QuestionnaireNotification(
            questionnaireType = type,
            title = "Cuestionario pendiente: ${info.title}",
            message = "Es momento de completar tu cuestionario ${getPeriodText(periodDays)}. ${info.estimatedTime}",
            dueDate = System.currentTimeMillis()
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
                icon = androidx.compose.material.icons.Icons.Filled.Chair,
                estimatedTime = "8-10 min",
                totalQuestions = 22
            )
            QuestionnaireType.SINTOMAS_MUSCULARES -> QuestionnaireInfo(
                type = type,
                title = "Síntomas Músculo-Esqueléticos",
                description = "Identifica dolores y molestias",
                icon = androidx.compose.material.icons.Icons.Filled.Healing,
                estimatedTime = "6-8 min",
                totalQuestions = 18
            )
            QuestionnaireType.SINTOMAS_VISUALES -> QuestionnaireInfo(
                type = type,
                title = "Síntomas Visuales",
                description = "Evalúa fatiga ocular",
                icon = androidx.compose.material.icons.Icons.Filled.Visibility,
                estimatedTime = "4-5 min",
                totalQuestions = 14
            )
            QuestionnaireType.CARGA_TRABAJO -> QuestionnaireInfo(
                type = type,
                title = "Carga de Trabajo",
                description = "Analiza demanda laboral",
                icon = androidx.compose.material.icons.Icons.Filled.WorkHistory,
                estimatedTime = "5-7 min",
                totalQuestions = 15
            )
            QuestionnaireType.ESTRES_SALUD_MENTAL -> QuestionnaireInfo(
                type = type,
                title = "Estrés y Salud Mental",
                description = "Identifica niveles de estrés",
                icon = androidx.compose.material.icons.Icons.Filled.Psychology,
                estimatedTime = "7-9 min",
                totalQuestions = 19
            )
            QuestionnaireType.HABITOS_SUENO -> QuestionnaireInfo(
                type = type,
                title = "Hábitos de Sueño",
                description = "Evalúa calidad de descanso",
                icon = androidx.compose.material.icons.Icons.Filled.Bedtime,
                estimatedTime = "3-4 min",
                totalQuestions = 9
            )
            QuestionnaireType.ACTIVIDAD_FISICA -> QuestionnaireInfo(
                type = type,
                title = "Actividad Física y Nutrición",
                description = "Analiza hábitos de ejercicio",
                icon = androidx.compose.material.icons.Icons.Filled.FitnessCenter,
                estimatedTime = "4-5 min",
                totalQuestions = 10
            )
            QuestionnaireType.BALANCE_VIDA_TRABAJO -> QuestionnaireInfo(
                type = type,
                title = "Balance Vida-Trabajo",
                description = "Evalúa equilibrio personal",
                icon = androidx.compose.material.icons.Icons.Filled.Balance,
                estimatedTime = "3-4 min",
                totalQuestions = 8
            )
        }
    }
}