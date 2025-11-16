package com.example.uleammed

import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Inicializar canal de notificaciones
        LocalNotificationScheduler.createNotificationChannel(this)

        // Iniciar verificación periódica automática
        LocalNotificationScheduler.schedulePeriodicCheck(this)

        // ✅ NUEVO: Sincronizar notificaciones al abrir la app
        syncNotificationsOnResume()

        setContent {
            UleamAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    UleamApp(
                        openFromNotification = intent.getBooleanExtra("open_from_notification", false),
                        questionnaireType = intent.getStringExtra("questionnaire_type")
                    )
                }
            }
        }
    }

    /**
     * ✅ NUEVO: Se llama cada vez que la app vuelve al foreground
     */
    override fun onResume() {
        super.onResume()
        syncNotificationsOnResume()
    }

    /**
     * ✅ NUEVO: Sincronizar notificaciones in-app con las push del sistema
     *
     * Esta función detecta cuando el usuario ha descartado notificaciones push
     * manualmente desde la barra de notificaciones, y marca las notificaciones
     * in-app correspondientes como leídas para mantener el badge actualizado.
     */
    private fun syncNotificationsOnResume() {
        try {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                android.util.Log.d("MainActivity", "⚠️ API < 23, sincronización no disponible")
                return
            }

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val activeNotifications = notificationManager.activeNotifications
            val activeIds = activeNotifications.map { it.id }.toSet()

            android.util.Log.d("MainActivity", """
            🔄 Sincronizando notificaciones
            - Activas en sistema: ${activeIds.size}
            - IDs: $activeIds
        """.trimIndent())

            val appNotificationManager = QuestionnaireNotificationManager(this)
            val userId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid

            if (userId != null) {
                val inAppNotifications = appNotificationManager.getNotifications()
                    .filter { !it.isRead && !it.isCompleted }

                // ✅ CORREGIDO: Solo marcar como leídas si NO están en la barra de notificaciones
                inAppNotifications.forEach { notification ->
                    val notificationId = 1000 + notification.questionnaireType.ordinal

                    // ✅ CRÍTICO: Solo marcar como leída si la push notification fue descartada
                    if (!activeIds.contains(notificationId)) {
                        android.util.Log.d("MainActivity", """
                        ⚠️ Notificación push descartada, marcando in-app como leída
                        - Tipo: ${notification.questionnaireType}
                        - ID: $notificationId
                    """.trimIndent())

                        appNotificationManager.markAsRead(notification.id)
                    } else {
                        android.util.Log.d("MainActivity", """
                        ✅ Notificación push activa, manteniendo in-app sin leer
                        - Tipo: ${notification.questionnaireType}
                        - ID: $notificationId
                    """.trimIndent())
                    }
                }

                android.util.Log.d("MainActivity", "✅ Sincronización completada")
            } else {
                android.util.Log.w("MainActivity", "⚠️ Usuario no autenticado, no se puede sincronizar")
            }
        } catch (e: SecurityException) {
            android.util.Log.e("MainActivity", "❌ Permiso denegado para acceder a notificaciones", e)
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "❌ Error sincronizando notificaciones", e)
        }
    }
}

@Composable
fun UleamApp(
    openFromNotification: Boolean = false,
    questionnaireType: String? = null
) {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = viewModel()
    val notificationViewModel: NotificationViewModel = viewModel()
    val context = LocalContext.current

    // ✅ CORREGIDO: Agregar collectAsState
    val authState by authViewModel.authState.collectAsState()
    val currentUser by authViewModel.currentUser.collectAsState()

    var permissionGranted by remember { mutableStateOf(context.hasNotificationPermission()) }

    if (!permissionGranted) {
        NotificationPermissionHandler(
            onPermissionGranted = {
                permissionGranted = true
            }
        )
    }

    // ✅ CORREGIDO: Determinar startDestination
    val startDestination = remember(currentUser) {
        if (currentUser != null) {
            if (currentUser?.hasCompletedQuestionnaire == false) {
                Screen.Questionnaire.route
            } else {
                Screen.Home.route
            }
        } else {
            Screen.Login.route
        }
    }

    LaunchedEffect(currentUser) {
        if (currentUser != null) {
            notificationViewModel.checkForNewNotifications()
        }
    }

    LaunchedEffect(openFromNotification, questionnaireType, currentUser) {
        if (openFromNotification && questionnaireType != null && currentUser != null) {
            try {
                val type = QuestionnaireType.valueOf(questionnaireType)
                val route = when (type) {
                    QuestionnaireType.ERGONOMIA -> Screen.ErgonomiaQuestionnaire.route
                    QuestionnaireType.SINTOMAS_MUSCULARES -> Screen.SintomasMuscularesQuestionnaire.route
                    QuestionnaireType.SINTOMAS_VISUALES -> Screen.SintomasVisualesQuestionnaire.route
                    QuestionnaireType.CARGA_TRABAJO -> Screen.CargaTrabajoQuestionnaire.route
                    QuestionnaireType.ESTRES_SALUD_MENTAL -> Screen.EstresSaludMentalQuestionnaire.route
                    QuestionnaireType.HABITOS_SUENO -> Screen.HabitosSuenoQuestionnaire.route
                    QuestionnaireType.ACTIVIDAD_FISICA -> Screen.ActividadFisicaQuestionnaire.route
                    QuestionnaireType.BALANCE_VIDA_TRABAJO -> Screen.BalanceVidaTrabajoQuestionnaire.route
                }

                navController.navigate(Screen.Home.route) {
                    popUpTo(Screen.Login.route) { inclusive = true }
                }
                navController.navigate(route)
            } catch (e: Exception) {
                android.util.Log.e("UleamApp", "❌ Tipo de cuestionario inválido: $questionnaireType", e)
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Login.route) {
            LoginScreen(
                onNavigateToRegister = {
                    navController.navigate(Screen.Register.route) {
                        launchSingleTop = true
                    }
                },
                onLoginSuccess = { needsQuestionnaire ->
                    if (needsQuestionnaire) {
                        navController.navigate(Screen.Questionnaire.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                            launchSingleTop = true
                        }
                    } else {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                }
            )
        }

        composable(Screen.Register.route) {
            RegisterScreen(
                onNavigateToLogin = {
                    navController.popBackStack()
                },
                onRegisterSuccess = {
                    navController.navigate(Screen.Questionnaire.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(Screen.Questionnaire.route) {
            QuestionnaireScreen(
                onComplete = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Questionnaire.route) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(Screen.Home.route) {
            HomeScreen(
                onLogout = {
                    authViewModel.signOut()
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onNavigateToQuestionnaire = { route ->
                    navController.navigate(route) {
                        launchSingleTop = true
                    }
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route) {
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        // Cuestionarios específicos
        composable(Screen.ErgonomiaQuestionnaire.route) {
            ErgonomiaQuestionnaireScreen(
                onComplete = {
                    notificationViewModel.markQuestionnaireCompleted(QuestionnaireType.ERGONOMIA)
                    Toast.makeText(
                        context,
                        "✅ Cuestionario de Ergonomía completado",
                        Toast.LENGTH_LONG
                    ).show()
                    navController.popBackStack()
                },
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.EstresSaludMentalQuestionnaire.route) {
            EstresSaludMentalQuestionnaireScreen(
                onComplete = {
                    notificationViewModel.markQuestionnaireCompleted(QuestionnaireType.ESTRES_SALUD_MENTAL)
                    Toast.makeText(
                        context,
                        "✅ Cuestionario de Estrés y Salud Mental completado",
                        Toast.LENGTH_LONG
                    ).show()
                    navController.popBackStack()
                },
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.SintomasMuscularesQuestionnaire.route) {
            SintomasMuscularesQuestionnaireScreen(
                onComplete = {
                    notificationViewModel.markQuestionnaireCompleted(QuestionnaireType.SINTOMAS_MUSCULARES)
                    Toast.makeText(
                        context,
                        "✅ Cuestionario de Síntomas Músculo-Esqueléticos completado",
                        Toast.LENGTH_LONG
                    ).show()
                    navController.popBackStack()
                },
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.CargaTrabajoQuestionnaire.route) {
            CargaTrabajoQuestionnaireScreen(
                onComplete = {
                    notificationViewModel.markQuestionnaireCompleted(QuestionnaireType.CARGA_TRABAJO)
                    Toast.makeText(
                        context,
                        "✅ Cuestionario de Carga de Trabajo completado",
                        Toast.LENGTH_LONG
                    ).show()
                    navController.popBackStack()
                },
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.SintomasVisualesQuestionnaire.route) {
            SintomasVisualesQuestionnaireScreen(
                onComplete = {
                    notificationViewModel.markQuestionnaireCompleted(QuestionnaireType.SINTOMAS_VISUALES)
                    Toast.makeText(
                        context,
                        "✅ Cuestionario de Síntomas Visuales completado",
                        Toast.LENGTH_LONG
                    ).show()
                    navController.popBackStack()
                },
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.ActividadFisicaQuestionnaire.route) {
            ActividadFisicaQuestionnaireScreen(
                onComplete = {
                    notificationViewModel.markQuestionnaireCompleted(QuestionnaireType.ACTIVIDAD_FISICA)
                    Toast.makeText(
                        context,
                        "✅ Cuestionario de Actividad Física completado",
                        Toast.LENGTH_LONG
                    ).show()
                    navController.popBackStack()
                },
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.HabitosSuenoQuestionnaire.route) {
            HabitosSuenoQuestionnaireScreen(
                onComplete = {
                    notificationViewModel.markQuestionnaireCompleted(QuestionnaireType.HABITOS_SUENO)
                    Toast.makeText(
                        context,
                        "✅ Cuestionario de Hábitos de Sueño completado",
                        Toast.LENGTH_LONG
                    ).show()
                    navController.popBackStack()
                },
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.BalanceVidaTrabajoQuestionnaire.route) {
            BalanceVidaTrabajoQuestionnaireScreen(
                onComplete = {
                    notificationViewModel.markQuestionnaireCompleted(QuestionnaireType.BALANCE_VIDA_TRABAJO)
                    Toast.makeText(
                        context,
                        "✅ Cuestionario de Balance Vida-Trabajo completado",
                        Toast.LENGTH_LONG
                    ).show()
                    navController.popBackStack()
                },
                onBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}