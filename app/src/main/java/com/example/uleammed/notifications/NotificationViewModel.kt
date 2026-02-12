package com.example.uleammed.notifications

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import com.example.uleammed.questionnaires.QuestionnaireInfo
import com.example.uleammed.questionnaires.QuestionnaireType
import com.example.uleammed.notifications.NotificationViewModel

/**
 * ViewModel de notificaciones usando Firestore
 *
 * Reemplaza el almacenamiento local (SharedPreferences) con Firestore
 * para persistencia y sincronización entre dispositivos.
 */
class NotificationViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = NotificationRepository()
    private val auth = FirebaseAuth.getInstance()
    private val userId: String? get() = auth.currentUser?.uid

    // ✅ CAMBIO PRINCIPAL: Ahora usa Flow de Firestore en lugar de SharedPreferences
    private val _scheduleConfig = MutableStateFlow<QuestionnaireScheduleConfig?>(null)
    val scheduleConfig: StateFlow<QuestionnaireScheduleConfig?> = _scheduleConfig.asStateFlow()

    private val _notifications = MutableStateFlow<List<QuestionnaireNotification>>(emptyList())
    val notifications: StateFlow<List<QuestionnaireNotification>> = _notifications.asStateFlow()

    private val _unreadCount = MutableStateFlow(0)
    val unreadCount: StateFlow<Int> = _unreadCount.asStateFlow()

    init {
        loadData()
    }

    /**
     * Carga los datos iniciales y establece listeners en tiempo real
     */
    private fun loadData() {
        val currentUserId = userId ?: return

        // Escuchar configuración en tiempo real
        viewModelScope.launch {
            repository.getScheduleConfigFlow(currentUserId)
                .collect { config ->
                    _scheduleConfig.value = config
                }
        }

        // Escuchar notificaciones en tiempo real
        viewModelScope.launch {
            repository.getNotificationsFlow(currentUserId)
                .collect { notificationsList ->
                    _notifications.value = notificationsList
                    _unreadCount.value = notificationsList.count { !it.isCompleted }
                }
        }
    }

    /**
     * Actualiza el período de días
     */
    fun updatePeriodDays(days: Int) {
        val currentUserId = userId ?: return

        viewModelScope.launch {
            repository.updatePeriodDays(currentUserId, days)
            // El Flow se actualizará automáticamente
        }
    }

    /**
     * Actualiza la hora preferida
     */
    fun updatePreferredTime(hour: Int, minute: Int) {
        val currentUserId = userId ?: return

        viewModelScope.launch {
            repository.updatePreferredTime(currentUserId, hour, minute)
            // El Flow se actualizará automáticamente
        }
    }

    /**
     * Marca un cuestionario como completado
     */
    fun markQuestionnaireCompleted(questionnaireType: QuestionnaireType) {
        val currentUserId = userId ?: return

        viewModelScope.launch {
            val now = System.currentTimeMillis()
            repository.markQuestionnaireCompleted(currentUserId, questionnaireType, now)

            // También marcar notificaciones relacionadas como completadas
            updateNotificationsForCompletedQuestionnaire(questionnaireType)
        }
    }

    /**
     * Actualiza las notificaciones cuando un cuestionario se completa
     */
    private suspend fun updateNotificationsForCompletedQuestionnaire(type: QuestionnaireType) {
        val currentUserId = userId ?: return
        val currentNotifications = _notifications.value

        // Buscar notificaciones del tipo completado
        val batch = mutableListOf<QuestionnaireNotification>()
        currentNotifications.forEach { notification ->
            if (notification.questionnaireType == type && !notification.isCompleted) {
                batch.add(notification.copy(isCompleted = true, isRead = true))
            }
        }

        // Guardar cambios
        if (batch.isNotEmpty()) {
            repository.saveNotifications(currentUserId, batch)
        }
    }

    /**
     * Marca una notificación como leída
     */
    fun markAsRead(notificationId: String) {
        val currentUserId = userId ?: return

        viewModelScope.launch {
            repository.markAsRead(currentUserId, notificationId)
            // El Flow se actualizará automáticamente
        }
    }

    /**
     * Marca todas las notificaciones de un tipo como leídas
     */
    fun markAsReadByType(questionnaireType: QuestionnaireType) {
        val currentUserId = userId ?: return

        viewModelScope.launch {
            repository.markAsReadByType(currentUserId, questionnaireType)
            // El Flow se actualizará automáticamente
        }
    }

    /**
     * Elimina una notificación
     */
    fun deleteNotification(notificationId: String) {
        val currentUserId = userId ?: return

        viewModelScope.launch {
            repository.deleteNotification(currentUserId, notificationId)
            // El Flow se actualizará automáticamente
        }
    }

    /**
     * Limpia notificaciones antiguas
     */
    fun cleanupOldNotifications() {
        val currentUserId = userId ?: return

        viewModelScope.launch {
            repository.cleanupOldNotifications(currentUserId)
        }
    }

    /**
     * Limpia notificaciones leídas
     */
    fun clearReadNotifications() {
        val currentUserId = userId ?: return

        viewModelScope.launch {
            repository.clearReadNotifications(currentUserId)
        }
    }

    /**
     * Genera notificaciones para cuestionarios pendientes
     * Debe llamarse cuando cambia la configuración o periódicamente
     */
    fun generateNotifications() {
        val currentUserId = userId ?: return
        val config = _scheduleConfig.value ?: return

        viewModelScope.launch {
            val newNotifications = mutableListOf<QuestionnaireNotification>()
            val currentNotifications = _notifications.value

            QuestionnaireType.values().forEach { type ->
                // Verificar si ya existe una notificación pendiente para este tipo
                val hasExisting = currentNotifications.any {
                    it.questionnaireType == type && !it.isCompleted
                }

                if (!hasExisting && config.isQuestionnaireEnabled(type)) {
                    val lastCompleted = config.lastCompletedDates[type.name]
                    val dueDate = if (lastCompleted != null) {
                        calculateNextDueDate(lastCompleted, config)
                    } else {
                        // Primera vez, vence en 1 día
                        System.currentTimeMillis() + (24 * 60 * 60 * 1000L)
                    }

                    val notification = QuestionnaireNotification(
                        questionnaireType = type,
                        title = "Cuestionario pendiente: ${type.displayName}",
                        message = "Es momento de completar tu evaluación de salud",
                        dueDate = dueDate
                    )

                    newNotifications.add(notification)
                }
            }

            // Guardar nuevas notificaciones
            if (newNotifications.isNotEmpty()) {
                repository.saveNotifications(currentUserId, newNotifications)
            }
        }
    }

    private fun calculateNextDueDate(lastCompleted: Long, config: QuestionnaireScheduleConfig): Long {
        val calendar = java.util.Calendar.getInstance().apply {
            timeInMillis = lastCompleted
            add(java.util.Calendar.DAY_OF_MONTH, config.periodDays)
            set(java.util.Calendar.HOUR_OF_DAY, config.preferredHour)
            set(java.util.Calendar.MINUTE, config.preferredMinute)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        return calendar.timeInMillis
    }
}

/**
 * Extensión para QuestionnaireType para obtener nombre de display
 */
val QuestionnaireType.displayName: String
    get() = when (this) {
        QuestionnaireType.ERGONOMIA -> "Ergonomía"
        QuestionnaireType.SINTOMAS_MUSCULARES -> "Síntomas Musculares"
        QuestionnaireType.SINTOMAS_VISUALES -> "Síntomas Visuales"
        QuestionnaireType.CARGA_TRABAJO -> "Carga de Trabajo"
        QuestionnaireType.ESTRES_SALUD_MENTAL -> "Estrés y Salud Mental"
        QuestionnaireType.HABITOS_SUENO -> "Hábitos de Sueño"
        QuestionnaireType.ACTIVIDAD_FISICA -> "Actividad Física"
        QuestionnaireType.BALANCE_VIDA_TRABAJO -> "Balance Vida-Trabajo"
    }