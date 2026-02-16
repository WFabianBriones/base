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

class NotificationViewModel(application: Application) : AndroidViewModel(application) {
    val notificationManager = QuestionnaireNotificationManager(application)
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

    private val _shouldShowSaludGeneralDialog = MutableStateFlow(false)
    val shouldShowSaludGeneralDialog: StateFlow<Boolean> = _shouldShowSaludGeneralDialog.asStateFlow()

    private val _isCheckingSaludGeneral = MutableStateFlow(false)
    val isCheckingSaludGeneral: StateFlow<Boolean> = _isCheckingSaludGeneral.asStateFlow()

    init {
        Log.d("NotificationViewModel", "🚀 ViewModel inicializado")
        // ✅ SOLO UNA CARGA INICIAL
        loadNotifications()
    }

    fun checkShouldShowSaludGeneralDialog(userId: String) {
        viewModelScope.launch {
            try {
                _isCheckingSaludGeneral.value = true
                Log.d("NotificationViewModel", "🔍 Verificando dialog de Salud General para: $userId")

                val shouldShow = withContext(Dispatchers.IO) {
                    notificationManager.shouldShowSaludGeneralDialog(userId)
                }

                Log.d("NotificationViewModel", "✅ Resultado verificación: $shouldShow")
                _shouldShowSaludGeneralDialog.value = shouldShow

            } catch (e: Exception) {
                Log.e("NotificationViewModel", "❌ Error verificando Salud General", e)
                _shouldShowSaludGeneralDialog.value = false
            } finally {
                _isCheckingSaludGeneral.value = false
            }
        }
    }

    fun dismissSaludGeneralDialog() {
        Log.d("NotificationViewModel", "🚪 Dialog de Salud General cerrado")
        _shouldShowSaludGeneralDialog.value = false
    }

    // ✅ OPTIMIZADO: Solo para carga inicial o refresh manual
    fun loadNotifications() {
        viewModelScope.launch {
            try {
                _isLoading.value = true

                val userId = auth.currentUser?.uid
                if (userId == null) {
                    _error.value = "Usuario no autenticado"
                    _isLoading.value = false
                    return@launch
                }

                // Cargar config
                _scheduleConfig.value = notificationManager.getScheduleConfig(userId)

                // Sincronizar con Firebase
                withContext(Dispatchers.IO) {
                    notificationManager.syncWithFirebase(userId)
                }

                // Actualizar UI
                refreshLocalState()

                Log.d("NotificationViewModel", "✅ Notificaciones cargadas y sincronizadas")

            } catch (e: Exception) {
                _error.value = "Error al cargar notificaciones: ${e.message}"
                Log.e("NotificationViewModel", "❌ Error", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    // ✅ NUEVO: Método interno para actualizar solo el estado local (sin sync)
    private fun refreshLocalState() {
        _notifications.value = notificationManager.getNotifications()
        _unreadCount.value = notificationManager.getUnreadCount()
    }

    // ✅ OPTIMIZADO: Verifica nuevas notificaciones en background
    fun checkForNewNotifications() {
        viewModelScope.launch {
            try {
                val userId = auth.currentUser?.uid ?: return@launch
                Log.d("NotificationViewModel", "🔍 Verificando nuevas notificaciones en background")

                withContext(Dispatchers.IO) {
                    notificationManager.syncWithFirebase(userId)
                }

                refreshLocalState()

                Log.d("NotificationViewModel", "✅ Notificaciones verificadas")

            } catch (e: Exception) {
                Log.e("NotificationViewModel", "❌ Error verificando notificaciones", e)
                // No mostrar error al usuario para verificaciones en background
            }
        }
    }

    fun markAsRead(notificationId: String) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    notificationManager.markAsRead(notificationId)
                }

                refreshLocalState()

                Log.d("NotificationViewModel", "📩 Notificación marcada como leída: $notificationId")
            } catch (e: Exception) {
                _error.value = "Error al marcar como leída: ${e.message}"
                Log.e("NotificationViewModel", "❌ Error marking as read", e)
            }
        }
    }

    fun deleteNotification(notificationId: String) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    notificationManager.deleteNotification(notificationId)
                }

                refreshLocalState()

                Log.d("NotificationViewModel", "🗑️ Notificación eliminada: $notificationId")
            } catch (e: Exception) {
                _error.value = "Error al eliminar notificación: ${e.message}"
                Log.e("NotificationViewModel", "❌ Error deleting notification", e)
            }
        }
    }

    // ✅ OPTIMIZADO: Sin loadNotifications() al final
    fun updatePeriodDays(days: Int) {
        viewModelScope.launch {
            try {
                val userId = auth.currentUser?.uid ?: return@launch

                // Actualización optimista de UI
                val currentConfig = _scheduleConfig.value
                if (currentConfig != null) {
                    _scheduleConfig.value = currentConfig.copy(
                        periodDays = days,
                        lastModified = System.currentTimeMillis()
                    )
                    Log.d("NotificationViewModel", "✅ UI actualizada inmediatamente a $days días")
                }

                // Guardar en background
                withContext(Dispatchers.IO) {
                    notificationManager.updatePeriodDays(userId, days)
                }

                // Solo refrescar el estado local (sin sync Firebase)
                refreshLocalState()

                Log.d("NotificationViewModel", "✅ Período actualizado a $days días")
            } catch (e: Exception) {
                _error.value = "Error al actualizar período: ${e.message}"
                Log.e("NotificationViewModel", "❌ Error updating period", e)

                // Revertir cambio optimista si falla
                loadNotifications()
            }
        }
    }

    // ✅ OPTIMIZADO: Sin loadNotifications() al final
    fun updateSaludGeneralPeriodDays(days: Int) {
        viewModelScope.launch {
            try {
                val userId = auth.currentUser?.uid ?: return@launch

                // Actualización optimista
                val currentConfig = _scheduleConfig.value
                if (currentConfig != null) {
                    _scheduleConfig.value = currentConfig.copy(
                        saludGeneralPeriodDays = days,
                        lastModified = System.currentTimeMillis()
                    )
                    Log.d("NotificationViewModel", "✅ UI salud general actualizada a $days días")
                }

                // Guardar en background
                withContext(Dispatchers.IO) {
                    notificationManager.updateSaludGeneralPeriodDays(userId, days)
                }

                refreshLocalState()

                Log.d("NotificationViewModel", "✅ Período de salud general actualizado a $days días")
            } catch (e: Exception) {
                _error.value = "Error al actualizar período de salud general: ${e.message}"
                Log.e("NotificationViewModel", "❌ Error updating salud general period", e)

                // Revertir cambio optimista si falla
                loadNotifications()
            }
        }
    }

    // ✅ OPTIMIZADO: Sin loadNotifications() al final
    fun updatePreferredTime(hour: Int, minute: Int) {
        viewModelScope.launch {
            try {
                require(hour in 0..23) { "Hora debe estar entre 0 y 23" }
                require(minute in 0..59) { "Minutos deben estar entre 0 y 59" }

                val userId = auth.currentUser?.uid ?: return@launch

                // Actualización optimista
                val currentConfig = _scheduleConfig.value
                if (currentConfig != null) {
                    _scheduleConfig.value = currentConfig.copy(
                        preferredHour = hour,
                        preferredMinute = minute,
                        lastModified = System.currentTimeMillis()
                    )
                }

                // Guardar en background
                withContext(Dispatchers.IO) {
                    notificationManager.updatePreferredTime(userId, hour, minute)
                }

                val config = PreferredTimeConfig(hour, minute)
                Log.d("NotificationViewModel", "✅ Hora preferida actualizada a ${config.formatReadable()}")
            } catch (e: Exception) {
                _error.value = "Error al actualizar hora: ${e.message}"
                Log.e("NotificationViewModel", "❌ Error updating preferred time", e)

                loadNotifications()
            }
        }
    }

    fun updateRemindersInApp(show: Boolean) {
        viewModelScope.launch {
            try {
                val userId = auth.currentUser?.uid ?: return@launch
                val config = _scheduleConfig.value ?: return@launch

                val updatedConfig = config.copy(
                    showRemindersInApp = show,
                    lastModified = System.currentTimeMillis()
                )

                // Actualización optimista
                _scheduleConfig.value = updatedConfig

                // Guardar en background
                withContext(Dispatchers.IO) {
                    notificationManager.saveScheduleConfig(updatedConfig)
                }

                Log.d("NotificationViewModel", "✅ Recordatorios in-app: ${if (show) "habilitados" else "deshabilitados"}")
            } catch (e: Exception) {
                _error.value = "Error al actualizar configuración: ${e.message}"
                Log.e("NotificationViewModel", "❌ Error updating reminders config", e)

                loadNotifications()
            }
        }
    }

    // ✅ OPTIMIZADO: Sin loadNotifications()
    fun markQuestionnaireCompleted(questionnaireType: QuestionnaireType) {
        viewModelScope.launch {
            try {
                val userId = auth.currentUser?.uid ?: return@launch

                withContext(Dispatchers.IO) {
                    notificationManager.markQuestionnaireCompleted(userId, questionnaireType)
                }

                refreshLocalState()

                Log.d("NotificationViewModel", "✅ Cuestionario $questionnaireType completado")
            } catch (e: Exception) {
                _error.value = "Error al marcar cuestionario: ${e.message}"
                Log.e("NotificationViewModel", "❌ Error marking questionnaire completed", e)
            }
        }
    }

    // ✅ OPTIMIZADO: Sin loadNotifications()
    fun cleanupOldNotifications() {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    notificationManager.cleanupOldNotifications()
                }

                refreshLocalState()

                Log.d("NotificationViewModel", "✅ Notificaciones antiguas eliminadas")
            } catch (e: Exception) {
                _error.value = "Error al limpiar notificaciones: ${e.message}"
                Log.e("NotificationViewModel", "❌ Error cleaning up notifications", e)
            }
        }
    }

    // ✅ OPTIMIZADO: Sin loadNotifications()
    fun clearReadNotifications() {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    notificationManager.clearReadNotifications()
                }

                refreshLocalState()

                Log.d("NotificationViewModel", "✅ Notificaciones leídas eliminadas")
            } catch (e: Exception) {
                _error.value = "Error al limpiar notificaciones leídas: ${e.message}"
                Log.e("NotificationViewModel", "❌ Error clearing read notifications", e)
            }
        }
    }

    // ✅ OPTIMIZADO: Sin loadNotifications()
    fun clearAllNotifications() {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    notificationManager.clearAllNotifications()
                }

                refreshLocalState()

                Log.d("NotificationViewModel", "✅ Todas las notificaciones eliminadas")
            } catch (e: Exception) {
                _error.value = "Error al limpiar todas las notificaciones: ${e.message}"
                Log.e("NotificationViewModel", "❌ Error clearing all notifications", e)
            }
        }
    }

    fun getQuestionnaireSummary(questionnaireType: QuestionnaireType): QuestionnaireStatsSummary? {
        return try {
            val userId = auth.currentUser?.uid ?: return null
            notificationManager.getStatsManager()
                .getQuestionnaireSummary(userId, questionnaireType, notificationManager)
        } catch (e: Exception) {
            Log.e("NotificationViewModel", "❌ Error getting stats summary", e)
            null
        }
    }

    fun getGlobalSummary(): GlobalStatsSummary? {
        return try {
            val userId = auth.currentUser?.uid ?: return null
            notificationManager.getStatsManager().getGlobalSummary(userId)
        } catch (e: Exception) {
            Log.e("NotificationViewModel", "❌ Error getting global summary", e)
            null
        }
    }

    fun filterNotifications(filter: NotificationFilter): List<QuestionnaireNotification> {
        return filter.apply(_notifications.value)
    }

    fun clearError() {
        _error.value = null
    }

    override fun onCleared() {
        super.onCleared()
        Log.d("NotificationViewModel", "🧹 ViewModel cleared")
    }
}