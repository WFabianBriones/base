package com.example.uleammed.notifications

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.uleammed.questionnaires.QuestionnaireType
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * ViewModel mejorado para gestión de notificaciones con soporte para
 * configuración de hora preferida y recordatorios
 *
 * ✅ ACTUALIZADO: Ahora incluye soporte para períodos diferenciados
 */
class NotificationViewModel(application: Application) : AndroidViewModel(application) {
    val notificationManager = QuestionnaireNotificationManager(application) // ✅ CAMBIO: Public para acceso desde Settings
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
        Log.d("NotificationViewModel", "🚀 ViewModel inicializado")
        loadNotifications()
        checkForNewNotifications()
    }

    fun loadNotifications() {
        viewModelScope.launch {
            try {
                _isLoading.value = true

                val notifications = withContext(Dispatchers.IO) {
                    notificationManager.getNotifications()
                        .sortedByDescending { it.createdAt }
                }

                val unread = withContext(Dispatchers.IO) {
                    notificationManager.getUnreadCount()
                }

                _notifications.value = notifications
                _unreadCount.value = unread

                val userId = auth.currentUser?.uid
                if (userId != null) {
                    val config = withContext(Dispatchers.IO) {
                        notificationManager.getScheduleConfig(userId)
                    }
                    _scheduleConfig.value = config
                }

                Log.d("NotificationViewModel", """
                    ✅ Notificaciones cargadas
                    - Total: ${notifications.size}
                    - No leídas: $unread
                """.trimIndent())

                _error.value = null
            } catch (e: Exception) {
                _error.value = "Error al cargar notificaciones: ${e.message}"
                Log.e("NotificationViewModel", "❌ Error loading notifications", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun checkForNewNotifications() {
        viewModelScope.launch {
            try {
                val userId = auth.currentUser?.uid
                if (userId == null) {
                    Log.w("NotificationViewModel", "⚠️ Usuario no autenticado")
                    return@launch
                }

                Log.d("NotificationViewModel", "🔍 Verificando nuevas notificaciones para userId: $userId")

                withContext(Dispatchers.IO) {
                    notificationManager.checkAndGenerateNotifications(userId)
                }

                // ✅ CRÍTICO: Recargar después de generar
                loadNotifications()
            } catch (e: Exception) {
                _error.value = "Error al verificar notificaciones: ${e.message}"
                Log.e("NotificationViewModel", "❌ Error checking notifications", e)
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
                Log.e("NotificationViewModel", "Error marking as read", e)
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
                Log.e("NotificationViewModel", "Error deleting notification", e)
            }
        }
    }

    /**
     * Actualizar período de días entre cuestionarios regulares
     */
    fun updatePeriodDays(days: Int) {
        viewModelScope.launch {
            try {
                val userId = auth.currentUser?.uid ?: return@launch

                // ✅ CRÍTICO: Actualizar StateFlow INMEDIATAMENTE
                val currentConfig = _scheduleConfig.value
                if (currentConfig != null) {
                    _scheduleConfig.value = currentConfig.copy(periodDays = days)
                    Log.d("NotificationViewModel", "✅ StateFlow actualizado inmediatamente a $days días")
                }

                // Actualizar en Firestore (en segundo plano)
                withContext(Dispatchers.IO) {
                    notificationManager.updatePeriodDays(userId, days)
                }

                // Recargar para sincronizar todo
                loadNotifications()

                Log.d("NotificationViewModel", "✅ Período completamente actualizado a $days días")
            } catch (e: Exception) {
                _error.value = "Error al actualizar período: ${e.message}"
                Log.e("NotificationViewModel", "❌ Error updating period", e)
            }
        }
    }

    /**
     * ✅ NUEVO: Actualizar período del cuestionario de salud general
     *
     * Este método actualiza solo el período de reevaluación de salud general
     * sin afectar los períodos de los otros 8 cuestionarios regulares
     *
     * @param days Nuevo período en días (30, 90 o 180)
     */
    fun updateSaludGeneralPeriodDays(days: Int) {
        viewModelScope.launch {
            try {
                val userId = auth.currentUser?.uid ?: return@launch

                // ✅ Actualizar StateFlow inmediatamente
                val currentConfig = _scheduleConfig.value
                if (currentConfig != null) {
                    _scheduleConfig.value = currentConfig.copy(
                        saludGeneralPeriodDays = days,
                        lastModified = System.currentTimeMillis()
                    )
                    Log.d("NotificationViewModel",
                        "✅ StateFlow salud general actualizado inmediatamente a $days días")
                }

                // Actualizar en backend
                withContext(Dispatchers.IO) {
                    notificationManager.updateSaludGeneralPeriodDays(userId, days)
                }

                // Recargar para sincronizar
                loadNotifications()

                Log.d("NotificationViewModel",
                    "✅ Período de salud general actualizado exitosamente a $days días")
            } catch (e: Exception) {
                _error.value = "Error al actualizar período de salud general: ${e.message}"
                Log.e("NotificationViewModel", "❌ Error updating salud general period", e)
            }
        }
    }

    /**
     * Actualizar hora preferida para notificaciones
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
                Log.d("NotificationViewModel", "Hora preferida actualizada a ${config.formatReadable()}")
            } catch (e: Exception) {
                _error.value = "Error al actualizar hora: ${e.message}"
                Log.e("NotificationViewModel", "Error updating preferred time", e)
            }
        }
    }

    /**
     * Actualizar si mostrar recordatorios en la app
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

                Log.d("NotificationViewModel", "Recordatorios in-app: ${if (show) "habilitados" else "deshabilitados"}")
            } catch (e: Exception) {
                _error.value = "Error al actualizar configuración: ${e.message}"
                Log.e("NotificationViewModel", "Error updating reminders config", e)
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

                Log.d("NotificationViewModel", "Cuestionario $questionnaireType completado")
            } catch (e: Exception) {
                _error.value = "Error al marcar cuestionario: ${e.message}"
                Log.e("NotificationViewModel", "Error marking questionnaire completed", e)
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

                Log.d("NotificationViewModel", "Notificaciones antiguas eliminadas")
            } catch (e: Exception) {
                _error.value = "Error al limpiar notificaciones: ${e.message}"
                Log.e("NotificationViewModel", "Error cleaning up notifications", e)
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

                Log.d("NotificationViewModel", "Notificaciones leídas eliminadas")
            } catch (e: Exception) {
                _error.value = "Error al limpiar notificaciones leídas: ${e.message}"
                Log.e("NotificationViewModel", "Error clearing read notifications", e)
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

                Log.d("NotificationViewModel", "Todas las notificaciones eliminadas")
            } catch (e: Exception) {
                _error.value = "Error al limpiar todas las notificaciones: ${e.message}"
                Log.e("NotificationViewModel", "Error clearing all notifications", e)
            }
        }
    }

    /**
     * Obtener estadísticas de un cuestionario
     */
    fun getQuestionnaireSummary(questionnaireType: QuestionnaireType): QuestionnaireStatsSummary? {
        return try {
            val userId = auth.currentUser?.uid ?: return null
            notificationManager.getStatsManager()
                .getQuestionnaireSummary(userId, questionnaireType, notificationManager)
        } catch (e: Exception) {
            Log.e("NotificationViewModel", "Error getting stats summary", e)
            null
        }
    }

    /**
     * Obtener estadísticas globales
     */
    fun getGlobalSummary(): GlobalStatsSummary? {
        return try {
            val userId = auth.currentUser?.uid ?: return null
            notificationManager.getStatsManager().getGlobalSummary(userId)
        } catch (e: Exception) {
            Log.e("NotificationViewModel", "Error getting global summary", e)
            null
        }
    }

    /**
     * Filtrar notificaciones
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
        Log.d("NotificationViewModel", "ViewModel cleared")
    }
}