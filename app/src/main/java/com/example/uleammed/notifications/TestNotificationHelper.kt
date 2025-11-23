package com.example.uleammed.notifications

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.example.uleammed.MainActivity
import com.example.uleammed.QuestionnaireType
import com.example.uleammed.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

object TestNotificationHelper {

    fun scheduleTestNotification10Seconds(context: Context) {
        val now = System.currentTimeMillis()
        val testDate = now + TimeUnit.SECONDS.toMillis(10)

        LocalNotificationScheduler.scheduleNotification(
            questionnaireType = QuestionnaireType.ERGONOMIA,
            dueDate = testDate,
            title = "🧪 TEST: Notificación de Prueba",
            message = "Esta es una notificación de prueba programada para 10 segundos",
            isReminder = false,
            createInAppNotification = false
        )

        Toast.makeText(
            context,
            "✅ Notificación de prueba programada para dentro de 10 segundos",
            Toast.LENGTH_LONG
        ).show()

        Log.d("TestNotification", """
            ✅ Notificación de prueba programada
            - Ahora: ${formatDate(now)}
            - Programada para: ${formatDate(testDate)}
            - Delay: 10 segundos
        """.trimIndent())
    }

    fun scheduleTestNotification1Minute(context: Context) {
        val now = System.currentTimeMillis()
        val testDate = now + TimeUnit.MINUTES.toMillis(1)

        LocalNotificationScheduler.scheduleNotification(
            questionnaireType = QuestionnaireType.SINTOMAS_MUSCULARES,
            dueDate = testDate,
            title = "🧪 TEST: Notificación de Prueba (1 min)",
            message = "Esta es una notificación de prueba programada para 1 minuto",
            isReminder = false,
            createInAppNotification = false
        )

        Toast.makeText(
            context,
            "✅ Notificación de prueba programada para dentro de 1 minuto",
            Toast.LENGTH_LONG
        ).show()

        Log.d("TestNotification", """
            ✅ Notificación de prueba programada
            - Ahora: ${formatDate(now)}
            - Programada para: ${formatDate(testDate)}
            - Delay: 1 minuto
        """.trimIndent())
    }

    fun showImmediateTestNotification(context: Context) {
        try {
            val notificationManager = NotificationManager::class.java.cast(
                context.getSystemService(Context.NOTIFICATION_SERVICE)
            )

            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                putExtra("questionnaire_type", QuestionnaireType.ERGONOMIA.name)
                putExtra("open_from_notification", true)
            }

            val pendingIntent = PendingIntent.getActivity(
                context,
                9999,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(context, LocalNotificationScheduler.CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("🧪 TEST INMEDIATO: Notificación")
                .setContentText("Esta notificación se muestra inmediatamente para probar permisos")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setVibrate(longArrayOf(0, 500, 200, 500))
                .setLights(0xFF00FF00.toInt(), 1000, 3000)
                .build()

            notificationManager?.notify(9999, notification)

            Toast.makeText(
                context,
                "✅ Notificación de prueba mostrada INMEDIATAMENTE",
                Toast.LENGTH_LONG
            ).show()

            Log.d("TestNotification", "✅ Notificación inmediata mostrada con ID 9999")
        } catch (e: SecurityException) {
            Toast.makeText(
                context,
                "❌ ERROR: Permiso de notificaciones denegado",
                Toast.LENGTH_LONG
            ).show()
            Log.e("TestNotification", "❌ Permiso denegado", e)
        } catch (e: Exception) {
            Toast.makeText(
                context,
                "❌ ERROR: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
            Log.e("TestNotification", "❌ Error mostrando notificación", e)
        }
    }

    fun cancelAllTestNotifications(context: Context) {
        LocalNotificationScheduler.cancelAllNotifications()

        Toast.makeText(
            context,
            "✅ Todas las notificaciones de prueba canceladas",
            Toast.LENGTH_SHORT
        ).show()

        Log.d("TestNotification", "✅ Notificaciones de prueba canceladas")
    }

    fun checkNotificationStatus(context: Context): String {
        val hasPermission = context.hasNotificationPermission()
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager

        val channelExists = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager?.getNotificationChannel(LocalNotificationScheduler.CHANNEL_ID) != null
        } else {
            true
        }

        return """
            ============ ESTADO DE NOTIFICACIONES ============
            Permiso concedido: $hasPermission
            Canal creado: $channelExists
            SDK Version: ${Build.VERSION.SDK_INT}
            Device: ${Build.MODEL}
            Emulator: ${isEmulator()}
            ================================================
        """.trimIndent()
    }

    private fun isEmulator(): Boolean {
        return (Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.startsWith("unknown")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for x86")
                || Build.MANUFACTURER.contains("Genymotion")
                || (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic"))
                || "google_sdk" == Build.PRODUCT)
    }

    private fun formatDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
}