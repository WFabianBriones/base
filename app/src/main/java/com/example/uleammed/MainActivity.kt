package com.example.uleammed

import android.app.Application
import android.content.Intent
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
import androidx.compose.runtime.rememberCoroutineScope
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
import com.example.uleammed.notifications.QuestionnaireNotificationManager
import com.example.uleammed.notifications.hasNotificationPermission
import com.example.uleammed.notifications.NotificationViewModel
import com.example.uleammed.perfil.SettingsScreen
import com.example.uleammed.questionnaires.*
import com.example.uleammed.scoring.ScoringViewModel
import com.example.uleammed.ui.UleamAppTheme
import com.example.uleammed.burnoutprediction.model.QuestionnaireData
import com.example.uleammed.burnoutprediction.presentation.screen.BurnoutAnalysisScreen
import com.example.uleammed.burnoutprediction.presentation.viewmodel.BurnoutAnalysisViewModel
import com.example.uleammed.burnoutprediction.presentation.viewmodel.BurnoutViewModelFactory
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import com.google.firebase.auth.FirebaseAuth

class MainActivity : ComponentActivity() {

    companion object {
        private const val TAG = "MainActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        LocalNotificationScheduler.createNotificationChannel(this)
        LocalNotificationScheduler.schedulePeriodicCheck(this)
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

    override fun onResume() {
        super.onResume()
        syncNotificationsOnResume()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        val openFromNotification = intent?.getBooleanExtra("open_from_notification", false) ?: false
        val questionnaireType = intent?.getStringExtra("questionnaire_type")
        if (openFromNotification && questionnaireType != null) {
            android.util.Log.d(TAG, "📱 Navegación desde notificación (app ya abierta) - tipo: $questionnaireType")
        }
    }

    private fun syncNotificationsOnResume() {
        lifecycleScope.launch {
            try {
                val appNotificationManager = QuestionnaireNotificationManager(this@MainActivity)
                val userId = FirebaseAuth.getInstance().currentUser?.uid
                if (userId != null) {
                    android.util.Log.d(TAG, "📱 Sincronizando notificaciones...")
                    withContext(Dispatchers.IO) {
                        appNotificationManager.syncWithFirebase(userId)
                    }
                    appNotificationManager.checkAndGenerateNotifications(userId)
                    android.util.Log.d(TAG, "✅ Sincronización completada")
                } else {
                    android.util.Log.w(TAG, "⚠️ Usuario no autenticado")
                }
            } catch (e: Exception) {
                android.util.Log.e(TAG, "❌ Error sincronizando notificaciones", e)
            }
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
    val application = context.applicationContext as Application

    // ✅ scope para lanzar coroutines en callbacks de Compose
    val scope = rememberCoroutineScope()

    val currentUser by authViewModel.currentUser.collectAsState()

    var permissionGranted by remember { mutableStateOf(context.hasNotificationPermission()) }

    if (!permissionGranted) {
        NotificationPermissionHandler(
            onPermissionGranted = { permissionGranted = true }
        )
    }

    // startDestination basado en el estado actual al abrir la app
    val startDestination = remember(currentUser) {
        if (currentUser != null) {
            if (currentUser?.hasCompletedQuestionnaire == false) Screen.Questionnaire.route
            else Screen.Home.route
        } else {
            Screen.Login.route
        }
    }

    // ✅ Solo para notificaciones y sincronización, NO para navegación principal
    LaunchedEffect(currentUser) {
        if (currentUser != null) {
            notificationViewModel.checkForNewNotifications()
        }
    }

    // Navegación desde notificación push
    LaunchedEffect(openFromNotification, questionnaireType, currentUser) {
        if (openFromNotification && questionnaireType != null && currentUser != null) {
            try {
                val type = QuestionnaireType.valueOf(questionnaireType)
                val route = when (type) {
                    QuestionnaireType.SALUD_GENERAL -> Screen.Questionnaire.route
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

    val burnoutViewModel: BurnoutAnalysisViewModel = viewModel(
        factory = BurnoutViewModelFactory(application)
    )

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Login.route) {
            LoginScreen(
                onNavigateToRegister = {
                    navController.navigate(Screen.Register.route) { launchSingleTop = true }
                },
                // ✅ FIX: Usamos scope.launch para el admin check y luego navegamos
                onLoginSuccess = { needsQuestionnaire ->
                    scope.launch {
                        android.util.Log.d("UleamApp", "🔑 Login exitoso, verificando rol...")

                        val destination = try {
                            com.example.uleammed.admin.AdminNavigationHelper
                                .getPostLoginDestination()
                        } catch (e: Exception) {
                            android.util.Log.e("UleamApp", "❌ Error verificando rol", e)
                            "home"
                        }

                        android.util.Log.d("UleamApp", "🔀 Destino: $destination")

                        when {
                            destination == "admin_dashboard" -> {
                                navController.navigate(Screen.AdminDashboard.route) {
                                    popUpTo(Screen.Login.route) { inclusive = true }
                                    launchSingleTop = true
                                }
                            }
                            needsQuestionnaire -> {
                                navController.navigate(Screen.Questionnaire.route) {
                                    popUpTo(Screen.Login.route) { inclusive = true }
                                    launchSingleTop = true
                                }
                            }
                            else -> {
                                navController.navigate(Screen.Home.route) {
                                    popUpTo(Screen.Login.route) { inclusive = true }
                                    launchSingleTop = true
                                }
                            }
                        }
                    }
                }
            )
        }

        composable(Screen.Register.route) {
            RegisterScreen(
                onNavigateToLogin = { navController.popBackStack() },
                onRegisterSuccess = {
                    navController.navigate(Screen.Questionnaire.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(Screen.Questionnaire.route) {
            val scoringViewModel: ScoringViewModel = viewModel(
                factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                        @Suppress("UNCHECKED_CAST")
                        return ScoringViewModel(application) as T
                    }
                }
            )
            QuestionnaireScreen(
                onComplete = {
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
                    navController.navigate(route) { launchSingleTop = true }
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route) { launchSingleTop = true }
                },
                onNavigateToResourceDetail = { resourceId ->
                    navController.navigate(Screen.ResourceDetail.createRoute(resourceId)) {
                        launchSingleTop = true
                    }
                },
                mainNavController = navController,
                onNavigateToBurnoutAnalysis = { indices ->
                    android.util.Log.d("MainActivity", "🎯 Iniciando análisis de burnout con IA")
                    val data = QuestionnaireData(
                        estresIndex = indices["estres"] ?: 0f,
                        ergonomiaIndex = indices["ergonomia"] ?: 0f,
                        cargaTrabajoIndex = indices["carga_trabajo"] ?: 0f,
                        calidadSuenoIndex = indices["calidad_sueno"] ?: 0f,
                        actividadFisicaIndex = indices["actividad_fisica"] ?: 0f,
                        sintomasMuscularesIndex = indices["sintomas_musculares"] ?: 0f,
                        sintomasVisualesIndex = indices["sintomas_visuales"] ?: 0f,
                        saludGeneralIndex = indices["salud_general"] ?: 0f
                    )
                    burnoutViewModel.analyzeBurnout(data)
                    navController.navigate(Screen.BurnoutAnalysis.route) { launchSingleTop = true }
                }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }

        composable(Screen.BurnoutAnalysis.route) {
            BurnoutAnalysisScreen(
                viewModel = burnoutViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // ===== CUESTIONARIOS =====

        composable(Screen.ErgonomiaQuestionnaire.route) {
            ErgonomiaQuestionnaireScreen(
                onComplete = {
                    notificationViewModel.markQuestionnaireCompleted(QuestionnaireType.ERGONOMIA)
                    Toast.makeText(context, "✅ Cuestionario de Ergonomía completado.", Toast.LENGTH_SHORT).show()
                    navController.popBackStack()
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.EstresSaludMentalQuestionnaire.route) {
            EstresSaludMentalQuestionnaireScreen(
                onComplete = {
                    notificationViewModel.markQuestionnaireCompleted(QuestionnaireType.ESTRES_SALUD_MENTAL)
                    Toast.makeText(context, "✅ Cuestionario de Estrés completado.", Toast.LENGTH_SHORT).show()
                    navController.popBackStack()
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.SintomasMuscularesQuestionnaire.route) {
            SintomasMuscularesQuestionnaireScreen(
                onComplete = {
                    notificationViewModel.markQuestionnaireCompleted(QuestionnaireType.SINTOMAS_MUSCULARES)
                    Toast.makeText(context, "✅ Cuestionario completado.", Toast.LENGTH_SHORT).show()
                    navController.popBackStack()
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.CargaTrabajoQuestionnaire.route) {
            CargaTrabajoQuestionnaireScreen(
                onComplete = {
                    notificationViewModel.markQuestionnaireCompleted(QuestionnaireType.CARGA_TRABAJO)
                    Toast.makeText(context, "✅ Cuestionario completado.", Toast.LENGTH_SHORT).show()
                    navController.popBackStack()
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.SintomasVisualesQuestionnaire.route) {
            SintomasVisualesQuestionnaireScreen(
                onComplete = {
                    notificationViewModel.markQuestionnaireCompleted(QuestionnaireType.SINTOMAS_VISUALES)
                    Toast.makeText(context, "✅ Cuestionario completado.", Toast.LENGTH_SHORT).show()
                    navController.popBackStack()
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.ActividadFisicaQuestionnaire.route) {
            ActividadFisicaQuestionnaireScreen(
                onComplete = {
                    notificationViewModel.markQuestionnaireCompleted(QuestionnaireType.ACTIVIDAD_FISICA)
                    Toast.makeText(context, "✅ Cuestionario completado.", Toast.LENGTH_SHORT).show()
                    navController.popBackStack()
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.HabitosSuenoQuestionnaire.route) {
            HabitosSuenoQuestionnaireScreen(
                onComplete = {
                    notificationViewModel.markQuestionnaireCompleted(QuestionnaireType.HABITOS_SUENO)
                    Toast.makeText(context, "✅ Cuestionario completado.", Toast.LENGTH_SHORT).show()
                    navController.popBackStack()
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.BalanceVidaTrabajoQuestionnaire.route) {
            BalanceVidaTrabajoQuestionnaireScreen(
                onComplete = {
                    notificationViewModel.markQuestionnaireCompleted(QuestionnaireType.BALANCE_VIDA_TRABAJO)
                    Toast.makeText(context, "✅ Cuestionario completado.", Toast.LENGTH_SHORT).show()
                    navController.popBackStack()
                },
                onBack = { navController.popBackStack() }
            )
        }

        // ===== OTRAS PANTALLAS =====

        composable(
            route = Screen.ResourceDetail.route,
            arguments = listOf(navArgument("resourceId") { type = NavType.StringType })
        ) { backStackEntry ->
            val resourceId = backStackEntry.arguments?.getString("resourceId") ?: ""
            com.example.uleammed.resources.ResourceDetailScreen(
                resourceId = resourceId,
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.ArticleViewer.route,
            arguments = listOf(navArgument("resourceId") { type = NavType.StringType })
        ) { backStackEntry ->
            val resourceId = backStackEntry.arguments?.getString("resourceId") ?: ""
            com.example.uleammed.resources.ArticleViewerScreen(
                resourceId = resourceId,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.EditProfile.route) {
            com.example.uleammed.perfil.EditProfileScreen(
                onBack = { navController.popBackStack() },
                onSaveSuccess = {
                    authViewModel.checkCurrentUser()
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.ViewQuestionnaire.route) {
            com.example.uleammed.perfil.ViewQuestionnaireScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.HelpSupport.route) {
            com.example.uleammed.perfil.HelpSupportScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.ExerciseGuided.route,
            arguments = listOf(navArgument("exerciseId") { type = NavType.StringType })
        ) { backStackEntry ->
            val exerciseId = backStackEntry.arguments?.getString("exerciseId") ?: ""
            com.example.uleammed.resources.ExerciseGuidedScreen(
                exerciseId = exerciseId,
                onBack = { navController.popBackStack() },
                onComplete = {
                    Toast.makeText(context, "✅ Ejercicio completado", Toast.LENGTH_SHORT).show()
                }
            )
        }

        // ===== ADMIN =====

        composable(Screen.AdminDashboard.route) {
            com.example.uleammed.admin.AdminDashboardScreen(
                onNavigateToUserManagement = {
                    navController.navigate(Screen.AdminUserManagement.route) {
                        launchSingleTop = true
                    }
                },
                onNavigateToCreateAdmin = {
                    navController.navigate(Screen.AdminCreateAdmin.route) {
                        launchSingleTop = true
                    }
                },
                onBack = {
                    authViewModel.signOut()
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.AdminCreateAdmin.route) {
            com.example.uleammed.admin.CreateAdminScreen(
                onBack = { navController.popBackStack() },
                onAdminCreated = { navController.popBackStack() }
            )
        }

        composable(Screen.AdminUserManagement.route) {
            com.example.uleammed.admin.UserManagementScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}