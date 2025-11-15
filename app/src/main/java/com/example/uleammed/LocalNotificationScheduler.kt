package com.example.uleammed

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.*
import java.util.concurrent.TimeUnit

/**
 * Programador de notificaciones push locales usando WorkManager
 */
object LocalNotificationScheduler {

    const val CHANNEL_ID = "questionnaire_reminders" // ✅ Quitado private
    private const val CHANNEL_NAME = "Recordatorios de Cuestionarios"
    private const val CHANNEL_DESCRIPTION = "Notificaciones para recordarte completar los cuestionarios de salud"

    /**
     * Inicializar el canal de notificaciones (llamar al inicio de la app)
     */
    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESCRIPTION
                enableVibration(true)
                enableLights(true)
            }

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Programar una notificación para un cuestionario específico
     */
    fun scheduleNotification(
        questionnaireType: QuestionnaireType,
        dueDate: Long,
        title: String,
        message: String,
        isReminder: Boolean = false // ✅ NUEVO: Flag para diferenciar recordatorios
    ) {
        val currentTime = System.currentTimeMillis()
        val delay = dueDate - currentTime

        // Solo programar si la fecha es futura
        if (delay <= 0) return

        val workTag = if (isReminder) {
            "reminder_${questionnaireType.name}"
        } else {
            "notification_${questionnaireType.name}"
        }

        val data = Data.Builder()
            .putString("type", questionnaireType.name)
            .putString("title", title)
            .putString("message", message)
            .putBoolean("isReminder", isReminder)
            .build()

        val notificationWork = OneTimeWorkRequestBuilder<NotificationWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(data)
            .addTag(workTag)
            .build()

        WorkManager.getInstance().enqueueUniqueWork(
            workTag,
            ExistingWorkPolicy.REPLACE,
            notificationWork
        )

        android.util.Log.d("LocalNotificationScheduler", """
            Notificación programada:
            - Tipo: ${if (isReminder) "Recordatorio" else "Principal"}
            - Cuestionario: $questionnaireType
            - Fecha: ${java.util.Date(dueDate)}
            - Delay: ${TimeUnit.MILLISECONDS.toHours(delay)} horas
        """.trimIndent())
    }

    /**
     * Cancelar notificación programada (principal y recordatorio)
     */
    fun cancelNotification(questionnaireType: QuestionnaireType) {
        val workTag = "notification_${questionnaireType.name}"
        val reminderTag = "reminder_${questionnaireType.name}"

        WorkManager.getInstance().cancelAllWorkByTag(workTag)
        WorkManager.getInstance().cancelAllWorkByTag(reminderTag)
    }

    /**
     * Cancelar todas las notificaciones programadas
     */
    fun cancelAllNotifications() {
        QuestionnaireType.values().forEach { type ->
            cancelNotification(type)
        }
    }

    /**
     * ✅ NUEVO: Programar verificación periódica diaria
     */
    fun schedulePeriodicCheck(context: Context) {
        val checkRequest = PeriodicWorkRequestBuilder<NotificationCheckWorker>(
            1, java.util.concurrent.TimeUnit.DAYS // Verificar una vez al día
        )
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                    .setRequiresBatteryNotLow(false)
                    .build()
            )
            .setInitialDelay(1, java.util.concurrent.TimeUnit.HOURS) // Primera verificación en 1 hora
            .addTag("periodic_notification_check")
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "daily_notification_check",
            ExistingPeriodicWorkPolicy.KEEP,
            checkRequest
        )

        android.util.Log.d("LocalNotificationScheduler", "Verificación periódica programada")
    }

    /**
     * Cancelar verificación periódica
     */
    fun cancelPeriodicCheck(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork("daily_notification_check")
    }
}

/**
 * Worker que ejecuta la notificación en el momento programado
 */
class NotificationWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : Worker(context, workerParams) {

    companion object {
        private const val NOTIFICATION_ID_BASE = 1000
    }

    override fun doWork(): Result {
        val type = inputData.getString("type") ?: return Result.failure()
        val title = inputData.getString("title") ?: "Recordatorio de cuestionario"
        val message = inputData.getString("message") ?: "Es momento de completar tu cuestionario"
        val isReminder = inputData.getBoolean("isReminder", false) // ✅ NUEVO

        // ✅ Mostrar notificación push
        showNotification(type, title, message, isReminder)

        // ✅ IMPORTANTE: Solo crear notificación en la app si NO es recordatorio
        // Los recordatorios son solo push, no aparecen en la sección Avisos
        if (!isReminder) {
            try {
                val questionnaireType = QuestionnaireType.valueOf(type)
                val notificationManager = QuestionnaireNotificationManager(context)
                val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
                val userId = auth.currentUser?.uid

                if (userId != null) {
                    // Forzar la verificación y generación de notificaciones
                    notificationManager.checkAndGenerateNotifications(userId)
                }
            } catch (e: Exception) {
                android.util.Log.e("NotificationWorker", "Error creando notificación en app", e)
            }
        }

        return Result.success()
    }

    private fun showNotification(typeString: String, title: String, message: String, isReminder: Boolean = false) {
        val type = try {
            QuestionnaireType.valueOf(typeString)
        } catch (e: Exception) {
            return
        }

        // ✅ NUEVO: Usar ID diferente para recordatorios
        val notificationId = if (isReminder) {
            NOTIFICATION_ID_BASE + type.ordinal + 100
        } else {
            NOTIFICATION_ID_BASE + type.ordinal
        }

        // Intent para abrir la app al tocar la notificación
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("questionnaire_type", type.name)
            putExtra("open_from_notification", true)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationBuilder = NotificationCompat.Builder(context, LocalNotificationScheduler.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(if (isReminder) NotificationCompat.PRIORITY_DEFAULT else NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        // ✅ Solo vibrar en notificación principal, no en recordatorios
        if (!isReminder) {
            notificationBuilder.setVibrate(longArrayOf(0, 500, 200, 500))
        }

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(notificationId, notificationBuilder.build())
    }
}

/**
 * Receiver para reiniciar las notificaciones después de reiniciar el dispositivo
 */
class BootReceiver : android.content.BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // Reprogramar todas las notificaciones pendientes
            val notificationManager = QuestionnaireNotificationManager(context)
            val userId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid

            if (userId != null) {
                notificationManager.checkAndGenerateNotifications(userId)

                // ✅ Reprogramar verificación periódica
                LocalNotificationScheduler.schedulePeriodicCheck(context)
            }
        }
    }
}

/**
 * ✅ NUEVO: Worker para verificación periódica diaria
 */
class NotificationCheckWorker(
    private val context: Context,
    params: WorkerParameters
) : Worker(context, params) {

    override fun doWork(): Result {
        return try {
            val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
            val userId = auth.currentUser?.uid

            if (userId != null) {
                android.util.Log.d("NotificationCheckWorker", "Iniciando verificación periódica")

                val manager = QuestionnaireNotificationManager(applicationContext)
                manager.checkAndGenerateNotifications(userId)

                android.util.Log.d("NotificationCheckWorker", "Verificación completada exitosamente")
                Result.success()
            } else {
                android.util.Log.w("NotificationCheckWorker", "Usuario no autenticado")
                Result.failure()
            }
        } catch (e: Exception) {
            android.util.Log.e("NotificationCheckWorker", "Error en verificación periódica", e)
            Result.retry()
        }
    }
}