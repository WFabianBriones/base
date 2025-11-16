package com.example.uleammed

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * ViewModel mejorado para gestión de notificaciones con soporte para
 * configuración de hora preferida y recordatorios
 */
class NotificationViewModel(application: Application) : AndroidViewModel(application) {
    private val notificationManager = QuestionnaireNotificationManager(application)
    private val auth = FirebaseAuth.getInstance()

    private val _notifications = MutableStateFlow<List<QuestionnaireNotification>>(emptyList())
    val notifications: StateFlow<List<QuestionnaireNotification>> = _notifications.asStateFlow()

    private val _unreadCount = MutableStateFlow(0)
    val unreadCount: StateFlow<Int> = _unreadCount.asStateFlow()

    private val _scheduleConfig = MutableStateFlow<QuestionnaireScheduleConfig?>(null)
    val scheduleConfig: StateFlow<QuestionnaireScheduleConfig?> = _scheduleConfig.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        loadNotifications()
        checkForNewNotifications()
    }

    /**
     * Cargar notificaciones del usuario actual
     */
    fun loadNotifications() {
        viewModelScope.launch {
            try {
                _isLoading.value = true

                val notifications = withContext(Dispatchers.IO) {
                    notificationManager.getNotifications()
                        .sortedByDescending { it.createdAt }
                }

                _notifications.value = notifications
                _unreadCount.value = notificationManager.getUnreadCount()

                val userId = auth.currentUser?.uid
                if (userId != null) {
                    val config = withContext(Dispatchers.IO) {
                        notificationManager.getScheduleConfig(userId)
                    }
                    _scheduleConfig.value = config
                }

                _error.value = null
            } catch (e: Exception) {
                _error.value = "Error al cargar notificaciones: ${e.message}"
                android.util.Log.e("NotificationViewModel", "Error loading notifications", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Verificar y generar nuevas notificaciones
     */
    fun checkForNewNotifications() {
        viewModelScope.launch {
            try {
                val userId = auth.currentUser?.uid ?: return@launch

                withContext(Dispatchers.IO) {
                    notificationManager.checkAndGenerateNotifications(userId)
                }

                loadNotifications()
            } catch (e: Exception) {
                _error.value = "Error al verificar notificaciones: ${e.message}"
                android.util.Log.e("NotificationViewModel", "Error checking notifications", e)
            }
        }
    }

    /**
     * Marcar notificación como leída
     */
    fun markAsRead(notificationId: String) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    notificationManager.markAsRead(notificationId)
                }
                loadNotifications()
            } catch (e: Exception) {
                _error.value = "Error al marcar como leída: ${e.message}"
                android.util.Log.e("NotificationViewModel", "Error marking as read", e)
            }
        }
    }

    /**
     * Eliminar notificación específica
     */
    fun deleteNotification(notificationId: String) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    notificationManager.deleteNotification(notificationId)
                }
                loadNotifications()
            } catch (e: Exception) {
                _error.value = "Error al eliminar notificación: ${e.message}"
                android.util.Log.e("NotificationViewModel", "Error deleting notification", e)
            }
        }
    }

    /**
     * Actualizar período de días entre cuestionarios
     */
    fun updatePeriodDays(days: Int) {
        viewModelScope.launch {
            try {
                val userId = auth.currentUser?.uid ?: return@launch

                withContext(Dispatchers.IO) {
                    notificationManager.updatePeriodDays(userId, days)
                }

                loadNotifications()

                android.util.Log.d("NotificationViewModel", "Período actualizado a $days días")
            } catch (e: Exception) {
                _error.value = "Error al actualizar período: ${e.message}"
                android.util.Log.e("NotificationViewModel", "Error updating period", e)
            }
        }
    }

    /**
     * ✅ NUEVO: Actualizar hora preferida para notificaciones
     */
    fun updatePreferredTime(hour: Int, minute: Int) {
        viewModelScope.launch {
            try {
                require(hour in 0..23) { "Hora debe estar entre 0 y 23" }
                require(minute in 0..59) { "Minutos deben estar entre 0 y 59" }

                val userId = auth.currentUser?.uid ?: return@launch

                withContext(Dispatchers.IO) {
                    notificationManager.updatePreferredTime(userId, hour, minute)
                }

                loadNotifications()

                val config = PreferredTimeConfig(hour, minute)
                android.util.Log.d("NotificationViewModel", "Hora preferida actualizada a ${config.formatReadable()}")
            } catch (e: Exception) {
                _error.value = "Error al actualizar hora: ${e.message}"
                android.util.Log.e("NotificationViewModel", "Error updating preferred time", e)
            }
        }
    }

    /**
     * ✅ NUEVO: Actualizar si mostrar recordatorios en la app
     */
    fun updateRemindersInApp(show: Boolean) {
        viewModelScope.launch {
            try {
                val userId = auth.currentUser?.uid ?: return@launch
                val config = _scheduleConfig.value ?: return@launch

                val updatedConfig = config.copy(showRemindersInApp = show)

                withContext(Dispatchers.IO) {
                    notificationManager.saveScheduleConfig(updatedConfig)
                }

                _scheduleConfig.value = updatedConfig

                android.util.Log.d("NotificationViewModel", "Recordatorios in-app: ${if (show) "habilitados" else "deshabilitados"}")
            } catch (e: Exception) {
                _error.value = "Error al actualizar configuración: ${e.message}"
                android.util.Log.e("NotificationViewModel", "Error updating reminders config", e)
            }
        }
    }

    /**
     * Marcar cuestionario como completado
     */
    fun markQuestionnaireCompleted(questionnaireType: QuestionnaireType) {
        viewModelScope.launch {
            try {
                val userId = auth.currentUser?.uid ?: return@launch

                withContext(Dispatchers.IO) {
                    notificationManager.markQuestionnaireCompleted(userId, questionnaireType)
                }

                loadNotifications()

                android.util.Log.d("NotificationViewModel", "Cuestionario $questionnaireType completado")
            } catch (e: Exception) {
                _error.value = "Error al marcar cuestionario: ${e.message}"
                android.util.Log.e("NotificationViewModel", "Error marking questionnaire completed", e)
            }
        }
    }

    /**
     * Limpiar notificaciones antiguas (30+ días)
     */
    fun cleanupOldNotifications() {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    notificationManager.cleanupOldNotifications()
                }
                loadNotifications()

                android.util.Log.d("NotificationViewModel", "Notificaciones antiguas eliminadas")
            } catch (e: Exception) {
                _error.value = "Error al limpiar notificaciones: ${e.message}"
                android.util.Log.e("NotificationViewModel", "Error cleaning up notifications", e)
            }
        }
    }

    /**
     * Limpiar notificaciones leídas
     */
    fun clearReadNotifications() {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    notificationManager.clearReadNotifications()
                }
                loadNotifications()

                android.util.Log.d("NotificationViewModel", "Notificaciones leídas eliminadas")
            } catch (e: Exception) {
                _error.value = "Error al limpiar notificaciones leídas: ${e.message}"
                android.util.Log.e("NotificationViewModel", "Error clearing read notifications", e)
            }
        }
    }

    /**
     * Limpiar TODAS las notificaciones
     */
    fun clearAllNotifications() {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    notificationManager.clearAllNotifications()
                }
                loadNotifications()

                android.util.Log.d("NotificationViewModel", "Todas las notificaciones eliminadas")
            } catch (e: Exception) {
                _error.value = "Error al limpiar todas las notificaciones: ${e.message}"
                android.util.Log.e("NotificationViewModel", "Error clearing all notifications", e)
            }
        }
    }

    /**
     * ✅ NUEVO: Obtener estadísticas de un cuestionario
     */
    fun getQuestionnaireSummary(questionnaireType: QuestionnaireType): QuestionnaireStatsSummary? {
        return try {
            val userId = auth.currentUser?.uid ?: return null
            notificationManager.getStatsManager()
                .getQuestionnaireSummary(userId, questionnaireType, notificationManager)
        } catch (e: Exception) {
            android.util.Log.e("NotificationViewModel", "Error getting stats summary", e)
            null
        }
    }

    /**
     * ✅ NUEVO: Obtener estadísticas globales
     */
    fun getGlobalSummary(): GlobalStatsSummary? {
        return try {
            val userId = auth.currentUser?.uid ?: return null
            notificationManager.getStatsManager().getGlobalSummary(userId)
        } catch (e: Exception) {
            android.util.Log.e("NotificationViewModel", "Error getting global summary", e)
            null
        }
    }

    /**
     * ✅ NUEVO: Filtrar notificaciones
     */
    fun filterNotifications(filter: NotificationFilter): List<QuestionnaireNotification> {
        return filter.apply(_notifications.value)
    }

    /**
     * Limpiar error
     */
    fun clearError() {
        _error.value = null
    }

    override fun onCleared() {
        super.onCleared()
        android.util.Log.d("NotificationViewModel", "ViewModel cleared")
    }
}