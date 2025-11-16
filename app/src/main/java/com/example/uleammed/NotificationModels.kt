package com.example.uleammed

import java.util.UUID

/**
 * Modelo de notificación de cuestionario
 *
 * @property id Identificador único de la notificación
 * @property questionnaireType Tipo de cuestionario asociado
 * @property title Título de la notificación
 * @property message Mensaje descriptivo
 * @property dueDate Fecha de vencimiento (timestamp futuro)
 * @property createdAt Fecha de creación (timestamp actual)
 * @property isRead Si la notificación ha sido leída
 * @property isCompleted Si el cuestionario ya fue completado
 */
data class QuestionnaireNotification(
    val id: String = UUID.randomUUID().toString(),
    val questionnaireType: QuestionnaireType,
    val title: String,
    val message: String,
    val dueDate: Long, // ✅ Fecha de vencimiento (FUTURA)
    val createdAt: Long = System.currentTimeMillis(), // Fecha de creación (HOY)
    val isRead: Boolean = false,
    val isCompleted: Boolean = false
) {
    /**
     * Verifica si la notificación está vencida
     */
    fun isOverdue(): Boolean = System.currentTimeMillis() > dueDate

    /**
     * Obtiene los días restantes hasta el vencimiento (negativo si está vencida)
     */
    fun daysUntilDue(): Long {
        val diff = dueDate - System.currentTimeMillis()
        return java.util.concurrent.TimeUnit.MILLISECONDS.toDays(diff)
    }
}

/**
 * Configuración de periodicidad de cuestionarios
 *
 * @property userId ID del usuario
 * @property periodDays Días entre cuestionarios (7, 15, 30)
 * @property preferredHour Hora preferida para notificaciones (0-23)
 * @property preferredMinute Minuto preferido para notificaciones (0-59)
 * @property lastCompletedDates Mapa de últimas fechas de completación por cuestionario
 * @property enabledQuestionnaires Set de cuestionarios habilitados
 * @property showRemindersInApp Si los recordatorios previos deben aparecer en la app
 */
data class QuestionnaireScheduleConfig(
    val userId: String = "",
    val periodDays: Int = 7, // Por defecto 7 días
    val preferredHour: Int = 9, // ✅ NUEVO: 9 AM por defecto
    val preferredMinute: Int = 0, // ✅ NUEVO: En punto
    val lastCompletedDates: Map<String, Long> = emptyMap(),
    val enabledQuestionnaires: Set<String> = QuestionnaireType.values().map { it.name }.toSet(),
    val showRemindersInApp: Boolean = true, // ✅ NUEVO: Mostrar recordatorios en Avisos
    val lastModified: Long = System.currentTimeMillis()
) {
    /**
     * Obtiene la próxima fecha de vencimiento para un cuestionario
     */
    fun getNextDueDate(questionnaireType: QuestionnaireType): Long? {
        val lastCompleted = lastCompletedDates[questionnaireType.name] ?: return null
        return calculateNextDueDate(lastCompleted)
    }

    /**
     * Calcula la próxima fecha de vencimiento desde una fecha base
     */
    private fun calculateNextDueDate(fromDate: Long): Long {
        val calendar = java.util.Calendar.getInstance().apply {
            timeInMillis = fromDate
            add(java.util.Calendar.DAY_OF_MONTH, periodDays)
            set(java.util.Calendar.HOUR_OF_DAY, preferredHour)
            set(java.util.Calendar.MINUTE, preferredMinute)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        return calendar.timeInMillis
    }

    /**
     * Verifica si un cuestionario está habilitado
     */
    fun isQuestionnaireEnabled(type: QuestionnaireType): Boolean {
        return enabledQuestionnaires.contains(type.name)
    }
}

/**
 * Estado de las notificaciones en el ViewModel
 */
sealed class NotificationState {
    object Idle : NotificationState()
    object Loading : NotificationState()
    data class Success(val notifications: List<QuestionnaireNotification>) : NotificationState()
    data class Error(val message: String) : NotificationState()
}

/**
 * Periodicidad disponible para cuestionarios
 *
 * @property days Número de días entre cuestionarios
 * @property displayName Nombre para mostrar al usuario
 * @property description Descripción detallada
 */
enum class QuestionnaireFrequency(
    val days: Int,
    val displayName: String,
    val description: String
) {
    WEEKLY(
        days = 7,
        displayName = "Semanal",
        description = "Completa los cuestionarios cada 7 días"
    ),
    BIWEEKLY(
        days = 15,
        displayName = "Quincenal",
        description = "Completa los cuestionarios cada 15 días"
    ),
    MONTHLY(
        days = 30,
        displayName = "Mensual",
        description = "Completa los cuestionarios cada 30 días"
    );

    companion object {
        /**
         * Obtiene la frecuencia desde el número de días
         */
        fun fromDays(days: Int): QuestionnaireFrequency = when (days) {
            7 -> WEEKLY
            15 -> BIWEEKLY
            30 -> MONTHLY
            else -> WEEKLY // Valor por defecto
        }
    }
}

/**
 * ✅ NUEVO: Configuración de hora preferida para notificaciones
 */
data class PreferredTimeConfig(
    val hour: Int = 9, // 0-23
    val minute: Int = 0, // 0-59
    val enabled: Boolean = true
) {
    init {
        require(hour in 0..23) { "Hora debe estar entre 0 y 23" }
        require(minute in 0..59) { "Minutos deben estar entre 0 y 59" }
    }

    /**
     * Formatea la hora en formato HH:mm
     */
    fun format(): String = "${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}"

    /**
     * Formatea la hora en formato legible (ej: "9:00 AM")
     */
    fun formatReadable(): String {
        val period = if (hour < 12) "AM" else "PM"
        val displayHour = when {
            hour == 0 -> 12
            hour > 12 -> hour - 12
            else -> hour
        }
        return "$displayHour:${minute.toString().padStart(2, '0')} $period"
    }
}

/**
 * ✅ NUEVO: Evento de notificación para logging
 */
sealed class NotificationEvent {
    data class Created(val type: QuestionnaireType, val dueDate: Long) : NotificationEvent()
    data class Completed(val type: QuestionnaireType, val onTime: Boolean) : NotificationEvent()
    data class Read(val notificationId: String) : NotificationEvent()
    data class Deleted(val notificationId: String) : NotificationEvent()
    data class Scheduled(val type: QuestionnaireType, val dueDate: Long, val isReminder: Boolean) : NotificationEvent()
    data class Error(val message: String, val exception: Exception?) : NotificationEvent()
}

/**
 * ✅ NUEVO: Resultado de operación de notificación
 */
sealed class NotificationResult<out T> {
    data class Success<T>(val data: T) : NotificationResult<T>()
    data class Error(val message: String, val exception: Exception? = null) : NotificationResult<Nothing>()
    object Loading : NotificationResult<Nothing>()
}

/**
 * ✅ NUEVO: Filtro de notificaciones
 */
enum class NotificationFilter {
    ALL,        // Todas las notificaciones
    UNREAD,     // Solo no leídas
    OVERDUE,    // Solo vencidas
    UPCOMING;   // Próximas (no vencidas)

    /**
     * Aplica el filtro a una lista de notificaciones
     */
    fun apply(notifications: List<QuestionnaireNotification>): List<QuestionnaireNotification> {
        return when (this) {
            ALL -> notifications
            UNREAD -> notifications.filter { !it.isRead }
            OVERDUE -> notifications.filter { it.isOverdue() }
            UPCOMING -> notifications.filter { !it.isOverdue() }
        }
    }
}