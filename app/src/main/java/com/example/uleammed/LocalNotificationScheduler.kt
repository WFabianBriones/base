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
 * con manejo mejorado de errores y logging
 */
object LocalNotificationScheduler {

    const val CHANNEL_ID = "questionnaire_reminders"
    private const val CHANNEL_NAME = "Recordatorios de Cuestionarios"
    private const val CHANNEL_DESCRIPTION = "Notificaciones para recordarte completar los cuestionarios de salud"
    private const val TAG = "NotificationScheduler"

    /**
     * Inicializar el canal de notificaciones
     * Debe llamarse al inicio de la aplicación
     */
    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val importance = NotificationManager.IMPORTANCE_DEFAULT
                val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                    description = CHANNEL_DESCRIPTION
                    enableVibration(true)
                    enableLights(true)
                    setShowBadge(true)
                }

                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.createNotificationChannel(channel)

                android.util.Log.d(TAG, "✅ Canal de notificaciones creado exitosamente")
            } catch (e: Exception) {
                android.util.Log.e(TAG, "❌ Error creando canal de notificaciones", e)
            }
        }
    }

    /**
     * Programar una notificación para un cuestionario específico
     *
     * @param questionnaireType Tipo de cuestionario
     * @param dueDate Timestamp de cuando debe mostrarse (debe ser futuro)
     * @param title Título de la notificación
     * @param message Mensaje de la notificación
     * @param isReminder Si es un recordatorio previo
     * @param createInAppNotification Si debe crear notificación en la app también
     */
    fun scheduleNotification(
        questionnaireType: QuestionnaireType,
        dueDate: Long,
        title: String,
        message: String,
        isReminder: Boolean = false,
        createInAppNotification: Boolean = true
    ) {
        val currentTime = System.currentTimeMillis()
        val delay = dueDate - currentTime

        // ✅ Validación estricta: solo programar si la fecha es futura
        if (delay <= 0) {
            android.util.Log.w(TAG, """
                ⚠️ Intento de programar notificación en el pasado ignorado
                - Tipo: $questionnaireType
                - Fecha solicitada: ${formatDate(dueDate)}
                - Fecha actual: ${formatDate(currentTime)}
                - Diferencia: ${TimeUnit.MILLISECONDS.toMinutes(delay)} minutos
            """.trimIndent())
            return
        }

        // ✅ Validación: delay no debe ser mayor a 1 año
        val maxDelay = TimeUnit.DAYS.toMillis(365)
        if (delay > maxDelay) {
            android.util.Log.w(TAG, """
                ⚠️ Delay excede el máximo permitido (1 año)
                - Tipo: $questionnaireType
                - Delay solicitado: ${TimeUnit.MILLISECONDS.toDays(delay)} días
            """.trimIndent())
            return
        }

        try {
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
                .putBoolean("createInAppNotification", createInAppNotification)
                .putLong("scheduledFor", dueDate)
                .build()

            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.NOT_REQUIRED) // No requiere red
                .setRequiresBatteryNotLow(false) // Puede ejecutarse con batería baja
                .build()

            val notificationWork = OneTimeWorkRequestBuilder<NotificationWorker>()
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .setInputData(data)
                .setConstraints(constraints)
                .addTag(workTag)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    WorkRequest.MIN_BACKOFF_MILLIS,
                    TimeUnit.MILLISECONDS
                )
                .build()

            WorkManager.getInstance().enqueueUniqueWork(
                workTag,
                ExistingWorkPolicy.REPLACE,
                notificationWork
            )

            android.util.Log.d(TAG, """
                ✅ Notificación programada exitosamente
                - Tipo: ${if (isReminder) "Recordatorio" else "Principal"}
                - Cuestionario: $questionnaireType
                - Fecha: ${formatDate(dueDate)}
                - Delay: ${formatDelay(delay)}
                - In-App: $createInAppNotification
            """.trimIndent())
        } catch (e: Exception) {
            android.util.Log.e(TAG, "❌ Error programando notificación para $questionnaireType", e)
        }
    }

    /**
     * Cancelar notificación programada (principal y recordatorio)
     */
    fun cancelNotification(questionnaireType: QuestionnaireType) {
        try {
            val workTag = "notification_${questionnaireType.name}"
            val reminderTag = "reminder_${questionnaireType.name}"

            WorkManager.getInstance().cancelAllWorkByTag(workTag)
            WorkManager.getInstance().cancelAllWorkByTag(reminderTag)

            android.util.Log.d(TAG, "✅ Notificaciones canceladas para $questionnaireType")
        } catch (e: Exception) {
            android.util.Log.e(TAG, "❌ Error cancelando notificaciones para $questionnaireType", e)
        }
    }

    /**
     * Cancelar todas las notificaciones programadas
     */
    fun cancelAllNotifications() {
        try {
            QuestionnaireType.values().forEach { type ->
                cancelNotification(type)
            }
            android.util.Log.d(TAG, "✅ Todas las notificaciones canceladas")
        } catch (e: Exception) {
            android.util.Log.e(TAG, "❌ Error cancelando todas las notificaciones", e)
        }
    }

    /**
     * Programar verificación periódica diaria
     */
    fun schedulePeriodicCheck(context: Context) {
        try {
            val checkRequest = PeriodicWorkRequestBuilder<NotificationCheckWorker>(
                1, TimeUnit.DAYS // Verificar una vez al día
            )
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                        .setRequiresBatteryNotLow(false)
                        .build()
                )
                .setInitialDelay(1, TimeUnit.HOURS) // Primera verificación en 1 hora
                .addTag("periodic_notification_check")
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "daily_notification_check",
                ExistingPeriodicWorkPolicy.KEEP,
                checkRequest
            )

            android.util.Log.d(TAG, "✅ Verificación periódica programada (cada 24 horas)")
        } catch (e: Exception) {
            android.util.Log.e(TAG, "❌ Error programando verificación periódica", e)
        }
    }

    /**
     * Cancelar verificación periódica
     */
    fun cancelPeriodicCheck(context: Context) {
        try {
            WorkManager.getInstance(context).cancelUniqueWork("daily_notification_check")
            android.util.Log.d(TAG, "✅ Verificación periódica cancelada")
        } catch (e: Exception) {
            android.util.Log.e(TAG, "❌ Error cancelando verificación periódica", e)
        }
    }

    // ============ UTILIDADES ============

    private fun formatDate(timestamp: Long): String {
        val sdf = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault())
        return sdf.format(java.util.Date(timestamp))
    }

    private fun formatDelay(delayMs: Long): String {
        val days = TimeUnit.MILLISECONDS.toDays(delayMs)
        val hours = TimeUnit.MILLISECONDS.toHours(delayMs) % 24
        val minutes = TimeUnit.MILLISECONDS.toMinutes(delayMs) % 60

        return when {
            days > 0 -> "$days día${if (days > 1) "s" else ""}, $hours hora${if (hours > 1) "s" else ""}"
            hours > 0 -> "$hours hora${if (hours > 1) "s" else ""}, $minutes minuto${if (minutes > 1) "s" else ""}"
            else -> "$minutes minuto${if (minutes > 1) "s" else ""}"
        }
    }
}

/**
 * Worker que ejecuta la notificación en el momento programado
 * con manejo robusto de errores
 */
class NotificationWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : Worker(context, workerParams) {

    companion object {
        private const val NOTIFICATION_ID_BASE = 1000
        private const val TAG = "NotificationWorker"
    }

    override fun doWork(): Result {
        return try {
            val type = inputData.getString("type") ?: return Result.failure()
            val title = inputData.getString("title") ?: "Recordatorio de cuestionario"
            val message = inputData.getString("message") ?: "Es momento de completar tu cuestionario"
            val isReminder = inputData.getBoolean("isReminder", false)
            val createInAppNotification = inputData.getBoolean("createInAppNotification", true)
            val scheduledFor = inputData.getLong("scheduledFor", 0L)

            android.util.Log.d(TAG, """
                ▶️ Ejecutando notificación
                - Tipo: $type
                - Es recordatorio: $isReminder
                - Programada para: ${formatDate(scheduledFor)}
                - In-App: $createInAppNotification
            """.trimIndent())

            // ✅ Mostrar notificación push
            showNotification(type, title, message, isReminder)

            // ✅ Crear notificación en la app si está configurado
            if (createInAppNotification && !isReminder) {
                createInAppNotification(type)
            }

            android.util.Log.d(TAG, "✅ Notificación ejecutada exitosamente: $type")
            Result.success()

        } catch (e: SecurityException) {
            android.util.Log.e(TAG, "❌ Permiso de notificaciones denegado", e)
            Result.failure()
        } catch (e: IllegalArgumentException) {
            android.util.Log.e(TAG, "❌ Argumento inválido en notificación", e)
            Result.failure()
        } catch (e: Exception) {
            android.util.Log.e(TAG, "❌ Error inesperado en notificación", e)
            // ✅ Reintentar en caso de error temporal
            if (runAttemptCount < 3) {
                android.util.Log.d(TAG, "🔄 Reintentando... (intento ${runAttemptCount + 1}/3)")
                Result.retry()
            } else {
                android.util.Log.e(TAG, "❌ Máximo de reintentos alcanzado")
                Result.failure()
            }
        }
    }

    private fun showNotification(typeString: String, title: String, message: String, isReminder: Boolean) {
        val type = try {
            QuestionnaireType.valueOf(typeString)
        } catch (e: Exception) {
            android.util.Log.e(TAG, "❌ Tipo de cuestionario inválido: $typeString", e)
            return
        }

        // ✅ Usar ID diferente para recordatorios
        val notificationId = if (isReminder) {
            NOTIFICATION_ID_BASE + type.ordinal + 100
        } else {
            NOTIFICATION_ID_BASE + type.ordinal
        }

        // Intent para abrir la app
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
            .setCategory(NotificationCompat.CATEGORY_REMINDER)

        // ✅ Solo vibrar en notificación principal
        if (!isReminder) {
            notificationBuilder.setVibrate(longArrayOf(0, 500, 200, 500))
        }

        try {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(notificationId, notificationBuilder.build())

            android.util.Log.d(TAG, "✅ Push notification mostrada (ID: $notificationId)")
        } catch (e: SecurityException) {
            android.util.Log.e(TAG, "❌ Permiso de notificaciones denegado", e)
            throw e
        }
    }

    private fun createInAppNotification(typeString: String) {
        try {
            val questionnaireType = QuestionnaireType.valueOf(typeString)
            val notificationManager = QuestionnaireNotificationManager(context)
            val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
            val userId = auth.currentUser?.uid

            if (userId != null) {
                notificationManager.checkAndGenerateNotifications(userId)
                android.util.Log.d(TAG, "✅ Notificación in-app creada para $typeString")
            } else {
                android.util.Log.w(TAG, "⚠️ Usuario no autenticado, no se puede crear notificación in-app")
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "❌ Error creando notificación in-app", e)
        }
    }

    private fun formatDate(timestamp: Long): String {
        if (timestamp == 0L) return "N/A"
        val sdf = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault())
        return sdf.format(java.util.Date(timestamp))
    }
}

/**
 * Worker para verificación periódica diaria
 */
class NotificationCheckWorker(
    private val context: Context,
    params: WorkerParameters
) : Worker(context, params) {

    companion object {
        private const val TAG = "NotificationCheckWorker"
    }

    override fun doWork(): Result {
        return try {
            val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
            val userId = auth.currentUser?.uid

            if (userId != null) {
                android.util.Log.d(TAG, "🔍 Iniciando verificación periódica")

                val manager = QuestionnaireNotificationManager(applicationContext)
                manager.checkAndGenerateNotifications(userId)

                android.util.Log.d(TAG, "✅ Verificación completada exitosamente")
                Result.success()
            } else {
                android.util.Log.w(TAG, "⚠️ Usuario no autenticado")
                Result.failure()
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "❌ Error en verificación periódica", e)

            // ✅ Reintentar hasta 3 veces
            if (runAttemptCount < 3) {
                android.util.Log.d(TAG, "🔄 Reintentando verificación... (intento ${runAttemptCount + 1}/3)")
                Result.retry()
            } else {
                android.util.Log.e(TAG, "❌ Máximo de reintentos alcanzado en verificación")
                Result.failure()
            }
        }
    }
}

/**
 * Receiver para reiniciar notificaciones después de reiniciar el dispositivo
 */
class BootReceiver : android.content.BroadcastReceiver() {

    companion object {
        private const val TAG = "BootReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            try {
                android.util.Log.d(TAG, "📱 Dispositivo reiniciado, reprogramando notificaciones")

                val notificationManager = QuestionnaireNotificationManager(context)
                val userId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid

                if (userId != null) {
                    notificationManager.checkAndGenerateNotifications(userId)
                    LocalNotificationScheduler.schedulePeriodicCheck(context)

                    android.util.Log.d(TAG, "✅ Notificaciones reprogramadas exitosamente")
                } else {
                    android.util.Log.w(TAG, "⚠️ Usuario no autenticado tras reinicio")
                }
            } catch (e: Exception) {
                android.util.Log.e(TAG, "❌ Error reprogramando notificaciones tras reinicio", e)
            }
        }
    }
}