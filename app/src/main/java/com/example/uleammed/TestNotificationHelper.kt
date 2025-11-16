package com.example.uleammed

import android.content.Context
import android.widget.Toast
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

        android.util.Log.d("TestNotification", """
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

        android.util.Log.d("TestNotification", """
            ✅ Notificación de prueba programada
            - Ahora: ${formatDate(now)}
            - Programada para: ${formatDate(testDate)}
            - Delay: 1 minuto
        """.trimIndent())
    }

    fun showImmediateTestNotification(context: Context) {
        try {
            val notificationManager = android.app.NotificationManager::class.java.cast(
                context.getSystemService(Context.NOTIFICATION_SERVICE)
            )

            val intent = android.content.Intent(context, MainActivity::class.java).apply {
                flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
                putExtra("questionnaire_type", QuestionnaireType.ERGONOMIA.name)
                putExtra("open_from_notification", true)
            }

            val pendingIntent = android.app.PendingIntent.getActivity(
                context,
                9999,
                intent,
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
            )

            val notification = androidx.core.app.NotificationCompat.Builder(context, LocalNotificationScheduler.CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("🧪 TEST INMEDIATO: Notificación")
                .setContentText("Esta notificación se muestra inmediatamente para probar permisos")
                .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
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

            android.util.Log.d("TestNotification", "✅ Notificación inmediata mostrada con ID 9999")
        } catch (e: SecurityException) {
            Toast.makeText(
                context,
                "❌ ERROR: Permiso de notificaciones denegado",
                Toast.LENGTH_LONG
            ).show()
            android.util.Log.e("TestNotification", "❌ Permiso denegado", e)
        } catch (e: Exception) {
            Toast.makeText(
                context,
                "❌ ERROR: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
            android.util.Log.e("TestNotification", "❌ Error mostrando notificación", e)
        }
    }

    fun cancelAllTestNotifications(context: Context) {
        LocalNotificationScheduler.cancelAllNotifications()

        Toast.makeText(
            context,
            "✅ Todas las notificaciones de prueba canceladas",
            Toast.LENGTH_SHORT
        ).show()

        android.util.Log.d("TestNotification", "✅ Notificaciones de prueba canceladas")
    }

    fun checkNotificationStatus(context: Context): String {
        val hasPermission = context.hasNotificationPermission()
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? android.app.NotificationManager

        val channelExists = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            notificationManager?.getNotificationChannel(LocalNotificationScheduler.CHANNEL_ID) != null
        } else {
            true
        }

        return """
            ============ ESTADO DE NOTIFICACIONES ============
            Permiso concedido: $hasPermission
            Canal creado: $channelExists
            SDK Version: ${android.os.Build.VERSION.SDK_INT}
            Device: ${android.os.Build.MODEL}
            Emulator: ${isEmulator()}
            ================================================
        """.trimIndent()
    }

    private fun isEmulator(): Boolean {
        return (android.os.Build.FINGERPRINT.startsWith("generic")
                || android.os.Build.FINGERPRINT.startsWith("unknown")
                || android.os.Build.MODEL.contains("google_sdk")
                || android.os.Build.MODEL.contains("Emulator")
                || android.os.Build.MODEL.contains("Android SDK built for x86")
                || android.os.Build.MANUFACTURER.contains("Genymotion")
                || (android.os.Build.BRAND.startsWith("generic") && android.os.Build.DEVICE.startsWith("generic"))
                || "google_sdk" == android.os.Build.PRODUCT)
    }

    private fun formatDate(timestamp: Long): String {
        val sdf = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm:ss", java.util.Locale.getDefault())
        return sdf.format(java.util.Date(timestamp))
    }
}