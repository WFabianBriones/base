package com.example.uleammed

import android.app.Application
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
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.example.uleammed.auth.AuthViewModel
import com.example.uleammed.auth.LoginScreen
import com.example.uleammed.auth.RegisterScreen
import com.example.uleammed.notifications.LocalNotificationScheduler
import com.example.uleammed.notifications.NotificationPermissionHandler
import com.example.uleammed.notifications.NotificationViewModel
import com.example.uleammed.notifications.QuestionnaireNotificationManager
import com.example.uleammed.notifications.hasNotificationPermission
import com.example.uleammed.perfil.SettingsScreen
import com.example.uleammed.questionnaires.*
import com.example.uleammed.scoring.ScoringViewModel
import com.example.uleammed.ui.UleamAppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Inicializar canal de notificaciones
        LocalNotificationScheduler.createNotificationChannel(this)

        // Iniciar verificación periódica automática
        LocalNotificationScheduler.schedulePeriodicCheck(this)

        // Sincronizar notificaciones al abrir la app
        syncNotificationsOnResume()

        setContent {
            UleamAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    UleamApp(
                        openFromNotification = intent.getBooleanExtra(
                            "open_from_notification",
                            false
                        ),
                        questionnaireType = intent.getStringExtra("questionnaire_type")
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        syncNotificationsOnResume()
    }

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

                inAppNotifications.forEach { notification ->
                    val notificationId = 1000 + notification.questionnaireType.ordinal

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
                    // ✅ NUEVO: Calcular scores después del cuestionario inicial
                    val scoringViewModel: ScoringViewModel = viewModel(
                        factory = androidx.lifecycle.viewmodel.compose.viewModel<ScoringViewModel>().javaClass.let {
                            object : androidx.lifecycle.ViewModelProvider.Factory {
                                override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                                    @Suppress("UNCHECKED_CAST")
                                    return ScoringViewModel(context.applicationContext as Application) as T
                                }
                            }
                        }
                    )
                    scoringViewModel.recalculateScores()

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
                },
                onNavigateToResourceDetail = { resourceId ->
                    navController.navigate(Screen.ResourceDetail.createRoute(resourceId)) {
                        launchSingleTop = true
                    }
                },
                mainNavController = navController
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        // ===== CUESTIONARIOS ESPECÍFICOS =====

        composable(Screen.ErgonomiaQuestionnaire.route) {
            val scoringViewModel: ScoringViewModel = viewModel(
                factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                        @Suppress("UNCHECKED_CAST")
                        return ScoringViewModel(context.applicationContext as Application) as T
                    }
                }
            )

            ErgonomiaQuestionnaireScreen(
                onComplete = {
                    notificationViewModel.markQuestionnaireCompleted(QuestionnaireType.ERGONOMIA)

                    // ✅ RECALCULAR SCORES AUTOMÁTICAMENTE
                    scoringViewModel.recalculateScores()

                    Toast.makeText(
                        context,
                        "✅ Cuestionario de Ergonomía completado. Actualizando tu análisis...",
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
            val scoringViewModel: ScoringViewModel = viewModel(
                factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                        @Suppress("UNCHECKED_CAST")
                        return ScoringViewModel(context.applicationContext as Application) as T
                    }
                }
            )

            EstresSaludMentalQuestionnaireScreen(
                onComplete = {
                    notificationViewModel.markQuestionnaireCompleted(QuestionnaireType.ESTRES_SALUD_MENTAL)
                    scoringViewModel.recalculateScores()

                    Toast.makeText(
                        context,
                        "✅ Cuestionario de Estrés y Salud Mental completado. Actualizando análisis...",
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
            val scoringViewModel: ScoringViewModel = viewModel(
                factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                        @Suppress("UNCHECKED_CAST")
                        return ScoringViewModel(context.applicationContext as Application) as T
                    }
                }
            )

            SintomasMuscularesQuestionnaireScreen(
                onComplete = {
                    notificationViewModel.markQuestionnaireCompleted(QuestionnaireType.SINTOMAS_MUSCULARES)
                    scoringViewModel.recalculateScores()

                    Toast.makeText(
                        context,
                        "✅ Cuestionario de Síntomas Músculo-Esqueléticos completado. Actualizando...",
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
            val scoringViewModel: ScoringViewModel = viewModel(
                factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                        @Suppress("UNCHECKED_CAST")
                        return ScoringViewModel(context.applicationContext as Application) as T
                    }
                }
            )

            CargaTrabajoQuestionnaireScreen(
                onComplete = {
                    notificationViewModel.markQuestionnaireCompleted(QuestionnaireType.CARGA_TRABAJO)
                    scoringViewModel.recalculateScores()

                    Toast.makeText(
                        context,
                        "✅ Cuestionario de Carga de Trabajo completado. Actualizando análisis...",
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
            val scoringViewModel: ScoringViewModel = viewModel(
                factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                        @Suppress("UNCHECKED_CAST")
                        return ScoringViewModel(context.applicationContext as Application) as T
                    }
                }
            )

            SintomasVisualesQuestionnaireScreen(
                onComplete = {
                    notificationViewModel.markQuestionnaireCompleted(QuestionnaireType.SINTOMAS_VISUALES)
                    scoringViewModel.recalculateScores()

                    Toast.makeText(
                        context,
                        "✅ Cuestionario de Síntomas Visuales completado. Actualizando análisis...",
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
            val scoringViewModel: ScoringViewModel = viewModel(
                factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                        @Suppress("UNCHECKED_CAST")
                        return ScoringViewModel(context.applicationContext as Application) as T
                    }
                }
            )

            ActividadFisicaQuestionnaireScreen(
                onComplete = {
                    notificationViewModel.markQuestionnaireCompleted(QuestionnaireType.ACTIVIDAD_FISICA)
                    scoringViewModel.recalculateScores()

                    Toast.makeText(
                        context,
                        "✅ Cuestionario de Actividad Física completado. Actualizando análisis...",
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
            val scoringViewModel: ScoringViewModel = viewModel(
                factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                        @Suppress("UNCHECKED_CAST")
                        return ScoringViewModel(context.applicationContext as Application) as T
                    }
                }
            )

            HabitosSuenoQuestionnaireScreen(
                onComplete = {
                    notificationViewModel.markQuestionnaireCompleted(QuestionnaireType.HABITOS_SUENO)
                    scoringViewModel.recalculateScores()

                    Toast.makeText(
                        context,
                        "✅ Cuestionario de Hábitos de Sueño completado. Actualizando análisis...",
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
            val scoringViewModel: ScoringViewModel = viewModel(
                factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                        @Suppress("UNCHECKED_CAST")
                        return ScoringViewModel(context.applicationContext as Application) as T
                    }
                }
            )

            BalanceVidaTrabajoQuestionnaireScreen(
                onComplete = {
                    notificationViewModel.markQuestionnaireCompleted(QuestionnaireType.BALANCE_VIDA_TRABAJO)
                    scoringViewModel.recalculateScores()

                    Toast.makeText(
                        context,
                        "✅ Cuestionario de Balance Vida-Trabajo completado. Actualizando análisis...",
                        Toast.LENGTH_LONG
                    ).show()
                    navController.popBackStack()
                },
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        // ===== OTRAS PANTALLAS =====

        composable(
            route = Screen.ResourceDetail.route,
            arguments = listOf(
                navArgument("resourceId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val resourceId = backStackEntry.arguments?.getString("resourceId") ?: ""

            com.example.uleammed.resources.ResourceDetailScreen(
                resourceId = resourceId,
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = Screen.ArticleViewer.route,
            arguments = listOf(
                navArgument("resourceId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val resourceId = backStackEntry.arguments?.getString("resourceId") ?: ""

            com.example.uleammed.resources.ArticleViewerScreen(
                resourceId = resourceId,
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.EditProfile.route) {
            com.example.uleammed.perfil.EditProfileScreen(
                onBack = {
                    navController.popBackStack()
                },
                onSaveSuccess = {
                    authViewModel.checkCurrentUser()
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.ViewQuestionnaire.route) {
            com.example.uleammed.perfil.ViewQuestionnaireScreen(
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.HelpSupport.route) {
            com.example.uleammed.perfil.HelpSupportScreen(
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = Screen.ExerciseGuided.route,
            arguments = listOf(
                navArgument("exerciseId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val exerciseId = backStackEntry.arguments?.getString("exerciseId") ?: ""

            com.example.uleammed.resources.ExerciseGuidedScreen(
                exerciseId = exerciseId,
                onBack = {
                    navController.popBackStack()
                },
                onComplete = {
                    Toast.makeText(
                        context,
                        "✅ Ejercicio completado",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            )
        }
    }
}