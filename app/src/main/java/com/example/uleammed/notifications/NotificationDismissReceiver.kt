package com.example.uleammed.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.uleammed.QuestionnaireType
import com.google.firebase.auth.FirebaseAuth

/**
 * Receiver que se activa cuando el usuario descarta una notificación push
 * desde la barra de notificaciones del sistema.
 *
 * Este componente es crítico para mantener sincronizado el contador de badges
 * entre las notificaciones push y las notificaciones in-app.
 */
class NotificationDismissReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_NOTIFICATION_DISMISSED = "com.example.uleammed.NOTIFICATION_DISMISSED"
        const val EXTRA_QUESTIONNAIRE_TYPE = "questionnaire_type"
        const val EXTRA_NOTIFICATION_ID = "notification_id"
        private const val TAG = "NotificationDismiss"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_NOTIFICATION_DISMISSED) {
            val questionnaireTypeString = intent.getStringExtra(EXTRA_QUESTIONNAIRE_TYPE)
            val notificationId = intent.getStringExtra(EXTRA_NOTIFICATION_ID)

            Log.d(TAG, """
                ============ NOTIFICACIÓN DESCARTADA ============
                Tipo: $questionnaireTypeString
                ID: $notificationId
                Usuario: ${FirebaseAuth.getInstance().currentUser?.uid}
                Timestamp: ${System.currentTimeMillis()}
                ================================================
            """.trimIndent())

            if (questionnaireTypeString != null) {
                try {
                    val questionnaireType = QuestionnaireType.valueOf(questionnaireTypeString)
                    val notificationManager = QuestionnaireNotificationManager(context)
                    val userId = FirebaseAuth.getInstance().currentUser?.uid

                    if (userId != null) {
                        // Marcar la notificación in-app como leída (no eliminar)
                        notificationManager.markAsReadByType(userId, questionnaireType)

                        Log.d(TAG, """
                            ✅ Sincronización exitosa
                            - Tipo: $questionnaireType
                            - Notificación in-app marcada como leída
                            - Badge actualizado
                        """.trimIndent())
                    } else {
                        Log.w(TAG, "⚠️ Usuario no autenticado, no se puede sincronizar")
                    }
                } catch (e: IllegalArgumentException) {
                    Log.e(TAG, "❌ Tipo de cuestionario inválido: $questionnaireTypeString", e)
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Error procesando dismissal", e)
                }
            } else {
                Log.w(TAG, "⚠️ Tipo de cuestionario null en intent")
            }
        }
    }
}