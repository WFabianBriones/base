package com.example.uleammed

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class NotificationViewModel(application: Application) : AndroidViewModel(application) {
    private val notificationManager = QuestionnaireNotificationManager(application)
    private val auth = FirebaseAuth.getInstance()

    private val _notifications = MutableStateFlow<List<QuestionnaireNotification>>(emptyList())
    val notifications: StateFlow<List<QuestionnaireNotification>> = _notifications.asStateFlow()

    private val _unreadCount = MutableStateFlow(0)
    val unreadCount: StateFlow<Int> = _unreadCount.asStateFlow()

    private val _scheduleConfig = MutableStateFlow<QuestionnaireScheduleConfig?>(null)
    val scheduleConfig: StateFlow<QuestionnaireScheduleConfig?> = _scheduleConfig.asStateFlow()

    init {
        loadNotifications()
        checkForNewNotifications()
    }

    fun loadNotifications() {
        viewModelScope.launch {
            val notifications = notificationManager.getNotifications()
                .sortedByDescending { it.createdAt }
            _notifications.value = notifications
            _unreadCount.value = notificationManager.getUnreadCount()

            val userId = auth.currentUser?.uid
            if (userId != null) {
                _scheduleConfig.value = notificationManager.getScheduleConfig(userId)
            }
        }
    }

    fun checkForNewNotifications() {
        viewModelScope.launch {
            val userId = auth.currentUser?.uid ?: return@launch
            notificationManager.checkAndGenerateNotifications(userId)
            loadNotifications()
        }
    }

    fun markAsRead(notificationId: String) {
        viewModelScope.launch {
            notificationManager.markAsRead(notificationId)
            loadNotifications()
        }
    }

    fun deleteNotification(notificationId: String) {
        viewModelScope.launch {
            notificationManager.deleteNotification(notificationId)
            loadNotifications()
        }
    }

    fun updatePeriodDays(days: Int) {
        viewModelScope.launch {
            val userId = auth.currentUser?.uid ?: return@launch
            notificationManager.updatePeriodDays(userId, days)
            loadNotifications()
        }
    }

    fun markQuestionnaireCompleted(questionnaireType: QuestionnaireType) {
        viewModelScope.launch {
            val userId = auth.currentUser?.uid ?: return@launch
            notificationManager.markQuestionnaireCompleted(userId, questionnaireType)
            loadNotifications()
        }
    }

    fun cleanupOldNotifications() {
        viewModelScope.launch {
            notificationManager.cleanupOldNotifications()
            loadNotifications()
        }
    }

    // NUEVAS FUNCIONES
    fun clearReadNotifications() {
        viewModelScope.launch {
            notificationManager.clearReadNotifications()
            loadNotifications()
        }
    }

    fun clearAllNotifications() {
        viewModelScope.launch {
            notificationManager.clearAllNotifications()
            loadNotifications()
        }
    }
}