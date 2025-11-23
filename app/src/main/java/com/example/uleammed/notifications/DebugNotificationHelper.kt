package com.example.uleammed.notifications

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DebugNotificationHelper {

    fun diagnoseNotifications(context: Context) {
        val manager = QuestionnaireNotificationManager(context)
        val userId = FirebaseAuth.getInstance().currentUser?.uid

        val report = buildString {
            appendLine("============ DIAGNÓSTICO DE NOTIFICACIONES ============")
            appendLine()

            // Estado del usuario
            appendLine("👤 USUARIO:")
            appendLine("  - Autenticado: ${userId != null}")
            appendLine("  - UserID: ${userId ?: "N/A"}")
            appendLine()

            // SharedPreferences
            val prefs = context.getSharedPreferences("questionnaire_notifications", Context.MODE_PRIVATE)
            val notificationsJson = prefs.getString("notifications", null)
            appendLine("💾 SHARED PREFERENCES:")
            appendLine("  - Archivo existe: ${notificationsJson != null}")
            appendLine("  - Tamaño: ${notificationsJson?.length ?: 0} chars")
            appendLine()

            // Notificaciones in-app
            val notifications = manager.getNotifications()
            appendLine("📱 NOTIFICACIONES IN-APP:")
            appendLine("  - Total: ${notifications.size}")
            appendLine("  - No leídas: ${notifications.count { !it.isRead }}")
            appendLine("  - Completadas: ${notifications.count { it.isCompleted }}")
            appendLine()

            if (notifications.isNotEmpty()) {
                appendLine("📋 DETALLE:")
                notifications.forEach { notif ->
                    appendLine("  - ${notif.questionnaireType}: ${if (notif.isRead) "✓ Leída" else "✗ No leída"}")
                }
                appendLine()
            }

            // Configuración
            if (userId != null) {
                val config = manager.getScheduleConfig(userId)
                appendLine("⚙️ CONFIGURACIÓN:")
                appendLine("  - Período: ${config.periodDays} días")
                appendLine("  - Hora preferida: ${config.preferredHour}:${config.preferredMinute}")
                appendLine("  - Cuestionarios completados: ${config.lastCompletedDates.size}")
                appendLine()

                if (config.lastCompletedDates.isNotEmpty()) {
                    appendLine("✅ COMPLETADOS:")
                    config.lastCompletedDates.forEach { (type, timestamp) ->
                        val date = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                            .format(Date(timestamp))
                        appendLine("  - $type: $date")
                    }
                }
            }

            appendLine("====================================================")
        }

        Log.d("DebugNotifications", report)
        Toast.makeText(context, "Diagnóstico en Logcat", Toast.LENGTH_SHORT).show()
    }
}