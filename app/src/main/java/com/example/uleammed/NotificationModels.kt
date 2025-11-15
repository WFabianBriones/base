package com.example.uleammed

import java.util.UUID

// Modelo de notificación
data class QuestionnaireNotification(
    val id: String = UUID.randomUUID().toString(),
    val questionnaireType: QuestionnaireType,
    val title: String,
    val message: String,
    val dueDate: Long, // ✅ Fecha de vencimiento (FUTURA)
    val createdAt: Long = System.currentTimeMillis(), // Fecha de creación (HOY)
    val isRead: Boolean = false,
    val isCompleted: Boolean = false
)

// Configuración de periodicidad
data class QuestionnaireScheduleConfig(
    val userId: String = "",
    val periodDays: Int = 7, // Por defecto 7 días
    val lastCompletedDates: Map<String, Long> = emptyMap(), // questionnaireType.name -> timestamp
    val enabledQuestionnaires: Set<String> = QuestionnaireType.values().map { it.name }.toSet()
)

// Estado de las notificaciones
sealed class NotificationState {
    object Idle : NotificationState()
    object Loading : NotificationState()
    data class Success(val notifications: List<QuestionnaireNotification>) : NotificationState()
    data class Error(val message: String) : NotificationState()
}

// Periodicidad disponible
enum class QuestionnaireFrequency(val days: Int, val displayName: String) {
    WEEKLY(7, "Semanal (7 días)"),
    BIWEEKLY(15, "Quincenal (15 días)"),
    MONTHLY(30, "Mensual (30 días)")
}