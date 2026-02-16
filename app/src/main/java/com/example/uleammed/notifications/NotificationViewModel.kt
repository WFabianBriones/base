package com.example.uleammed.notifications

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.uleammed.questionnaires.QuestionnaireType
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
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

    // ✅ NUEVO: Estado para el dialog de Salud General
    private val _shouldShowSaludGeneralDialog = MutableStateFlow(false)
    val shouldShowSaludGeneralDialog: StateFlow<Boolean> = _shouldShowSaludGeneralDialog.asStateFlow()

    private val _isCheckingSaludGeneral = MutableStateFlow(false)
    val isCheckingSaludGeneral: StateFlow<Boolean> = _isCheckingSaludGeneral.asStateFlow()

    init {
        Log.d("NotificationViewModel", "🚀 ViewModel inicializado")
        loadNotifications()
    }

    /**
     * ✅ NUEVO: Función para regenerar notificaciones después de eliminar un cuestionario
     *
     * Esta función se debe llamar después de:
     * 1. Eliminar un cuestionario completado
     * 2. Cambiar la configuración de periodicidad
     * 3. Habilitar/deshabilitar cuestionarios
     *
     * Lo que hace:
     * 1. **Fuerza recarga de configuración desde Firebase**
     * 2. Verifica qué cuestionarios necesitan notificaciones
     * 3. Genera notificaciones para cuestionarios sin notificaciones pendientes
     * 4. Actualiza el estado local con las nuevas notificaciones
     */
    fun checkAndGenerateNotifications() {
        viewModelScope.launch {
            try {
                val userId = auth.currentUser?.uid ?: return@launch

                Log.d("NotificationViewModel", "🔄 Verificando y generando notificaciones...")

                withContext(Dispatchers.IO) {
                    // ✅ CRÍTICO: Primero forzar recarga de configuración desde Firebase
                    try {
                        val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                        val configDoc = firestore.collection("users")
                            .document(userId)
                            .collection("settings")
                            .document("notifications")
                            .get()
                            .await()

                        if (configDoc.exists()) {
                            // Convertir a QuestionnaireScheduleConfig y guardar en SharedPreferences
                            @Suppress("UNCHECKED_CAST")
                            val lastCompletedDates = configDoc.get("lastCompletedDates") as? Map<String, Long> ?: emptyMap()
                            val periodDays = configDoc.getLong("periodDays")?.toInt() ?: 7
                            val preferredHour = configDoc.getLong("preferredHour")?.toInt() ?: 9
                            val preferredMinute = configDoc.getLong("preferredMinute")?.toInt() ?: 0
                            val saludGeneralPeriodDays = configDoc.getLong("saludGeneralPeriodDays")?.toInt() ?: 90
                            val showRemindersInApp = configDoc.getBoolean("showRemindersInApp") ?: true
                            @Suppress("UNCHECKED_CAST")
                            val enabledQuestionnaires = (configDoc.get("enabledQuestionnaires") as? List<String>)?.toSet()
                                ?: com.example.uleammed.questionnaires.QuestionnaireType.values().map { it.name }.toSet()

                            val freshConfig = com.example.uleammed.notifications.QuestionnaireScheduleConfig(
                                userId = userId,
                                periodDays = periodDays,
                                preferredHour = preferredHour,
                                preferredMinute = preferredMinute,
                                lastCompletedDates = lastCompletedDates,
                                enabledQuestionnaires = enabledQuestionnaires,
                                showRemindersInApp = showRemindersInApp,
                                saludGeneralPeriodDays = saludGeneralPeriodDays,
                                lastModified = System.currentTimeMillis()
                            )

                            // Guardar en caché local
                            notificationManager.saveScheduleConfig(freshConfig)
                            Log.d("NotificationViewModel", "✅ Configuración recargada desde Firebase")
                        }
                    } catch (e: Exception) {
                        Log.e("NotificationViewModel", "⚠️ Error recargando config desde Firebase", e)
                    }

                    // Ahora sí, verificar y generar con la configuración actualizada
                    notificationManager.checkAndGenerateNotifications(userId)
                }

                // Actualizar el estado local con las nuevas notificaciones
                _notifications.value = notificationManager.getNotifications()
                _unreadCount.value = notificationManager.getUnreadCount()

                Log.d("NotificationViewModel", "✅ Notificaciones verificadas y generadas")
            } catch (e: Exception) {
                Log.e("NotificationViewModel", "❌ Error generando notificaciones", e)
                _error.value = "Error al generar notificaciones: ${e.message}"
            }
        }
    }

    // ✅ NUEVA FUNCIÓN: Verificar si debe mostrar el dialog de Salud General
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

    // ✅ NUEVA FUNCIÓN: Cerrar el dialog de Salud General
    fun dismissSaludGeneralDialog() {
        Log.d("NotificationViewModel", "🚪 Dialog de Salud General cerrado")
        _shouldShowSaludGeneralDialog.value = false
    }

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

                _scheduleConfig.value = notificationManager.getScheduleConfig(userId)

                // Paso 1: Sincronizar con Firebase (elimina notificaciones obsoletas)
                withContext(Dispatchers.IO) {
                    notificationManager.syncWithFirebase(userId)
                }

                // Paso 2: Generar notificaciones faltantes
                notificationManager.checkAndGenerateNotifications(userId)

                // Paso 3: Actualizar UI
                _notifications.value = notificationManager.getNotifications()
                _unreadCount.value = notificationManager.getUnreadCount()

                Log.d("NotificationViewModel", "✅ Notificaciones cargadas y sincronizadas")

            } catch (e: Exception) {
                _error.value = "Error al cargar notificaciones: ${e.message}"
                Log.e("NotificationViewModel", "❌ Error", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun checkForNewNotifications() {
        viewModelScope.launch {
            try {
                _isLoading.value = true

                val userId = auth.currentUser?.uid ?: return@launch
                Log.d("NotificationViewModel", "🔍 Verificando nuevas notificaciones")

                // Paso 1: Sincronizar con Firebase
                withContext(Dispatchers.IO) {
                    notificationManager.syncWithFirebase(userId)
                }

                // Paso 2: Generar notificaciones faltantes
                notificationManager.checkAndGenerateNotifications(userId)

                // Paso 3: Actualizar estado local
                _notifications.value = notificationManager.getNotifications()
                _unreadCount.value = notificationManager.getUnreadCount()

                Log.d("NotificationViewModel", "✅ Notificaciones verificadas sin recargar todo")

            } catch (e: Exception) {
                _error.value = "Error al verificar notificaciones: ${e.message}"
                Log.e("NotificationViewModel", "❌ Error", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun markAsRead(notificationId: String) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    notificationManager.markAsRead(notificationId)
                }

                _notifications.value = notificationManager.getNotifications()
                _unreadCount.value = notificationManager.getUnreadCount()

                Log.d("NotificationViewModel", "📩 Notificación marcada como leída: $notificationId")
            } catch (e: Exception) {
                _error.value = "Error al marcar como leída: ${e.message}"
                Log.e("NotificationViewModel", "Error marking as read", e)
            }
        }
    }

    fun deleteNotification(notificationId: String) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    notificationManager.deleteNotification(notificationId)
                }

                _notifications.value = notificationManager.getNotifications()
                _unreadCount.value = notificationManager.getUnreadCount()

                Log.d("NotificationViewModel", "🗑️ Notificación eliminada: $notificationId")
            } catch (e: Exception) {
                _error.value = "Error al eliminar notificación: ${e.message}"
                Log.e("NotificationViewModel", "Error deleting notification", e)
            }
        }
    }

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

    fun updatePeriodDays(days: Int) {
        viewModelScope.launch {
            try {
                val userId = auth.currentUser?.uid ?: return@launch

                val currentConfig = _scheduleConfig.value
                if (currentConfig != null) {
                    _scheduleConfig.value = currentConfig.copy(periodDays = days)
                    Log.d("NotificationViewModel", "✅ StateFlow actualizado inmediatamente a $days días")
                }

                withContext(Dispatchers.IO) {
                    notificationManager.updatePeriodDays(userId, days)
                }

                loadNotifications()

                Log.d("NotificationViewModel", "✅ Período completamente actualizado a $days días")
            } catch (e: Exception) {
                _error.value = "Error al actualizar período: ${e.message}"
                Log.e("NotificationViewModel", "❌ Error updating period", e)
            }
        }
    }

    fun updateSaludGeneralPeriodDays(days: Int) {
        viewModelScope.launch {
            try {
                val userId = auth.currentUser?.uid ?: return@launch

                val currentConfig = _scheduleConfig.value
                if (currentConfig != null) {
                    _scheduleConfig.value = currentConfig.copy(
                        saludGeneralPeriodDays = days,
                        lastModified = System.currentTimeMillis()
                    )
                    Log.d("NotificationViewModel",
                        "✅ StateFlow salud general actualizado inmediatamente a $days días")
                }

                withContext(Dispatchers.IO) {
                    notificationManager.updateSaludGeneralPeriodDays(userId, days)
                }

                loadNotifications()

                Log.d("NotificationViewModel",
                    "✅ Período de salud general actualizado exitosamente a $days días")
            } catch (e: Exception) {
                _error.value = "Error al actualizar período de salud general: ${e.message}"
                Log.e("NotificationViewModel", "❌ Error updating salud general period", e)
            }
        }
    }

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

    fun getGlobalSummary(): GlobalStatsSummary? {
        return try {
            val userId = auth.currentUser?.uid ?: return null
            notificationManager.getStatsManager().getGlobalSummary(userId)
        } catch (e: Exception) {
            Log.e("NotificationViewModel", "Error getting global summary", e)
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
        Log.d("NotificationViewModel", "ViewModel cleared")
    }
}