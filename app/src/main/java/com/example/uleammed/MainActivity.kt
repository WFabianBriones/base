package com.example.uleammed

import android.app.Application
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
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
import com.example.uleammed.notifications.QuestionnaireNotificationManager
import com.example.uleammed.notifications.hasNotificationPermission
import com.example.uleammed.notifications.NotificationViewModel
import com.example.uleammed.perfil.SettingsScreen
import com.example.uleammed.questionnaires.*
import com.example.uleammed.scoring.ScoringViewModel
import com.example.uleammed.ui.UleamAppTheme
// ⭐ AGREGAR: Importaciones para análisis de burnout
import com.example.uleammed.burnoutprediction.model.QuestionnaireData
import com.example.uleammed.burnoutprediction.presentation.screen.BurnoutAnalysisScreen
import com.example.uleammed.burnoutprediction.presentation.viewmodel.BurnoutAnalysisViewModel
import com.example.uleammed.burnoutprediction.presentation.viewmodel.BurnoutViewModelFactory
// ✅ CORREGIDO: Agregar imports para coroutines y Firebase
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

    /**
     * ⚠️ CRÍTICO: Manejar deep links cuando la app ya está abierta
     *
     * Este método se llama cuando el usuario toca una notificación
     * y la app ya está en memoria (no se vuelve a llamar onCreate)
     */
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)

        // Actualizar el intent de la actividad
        setIntent(intent)

        val openFromNotification = intent?.getBooleanExtra("open_from_notification", false) ?: false
        val questionnaireType = intent?.getStringExtra("questionnaire_type")

        if (openFromNotification && questionnaireType != null) {
            android.util.Log.d(TAG, """
                📱 Navegación desde notificación (app ya abierta)
                - Tipo de cuestionario: $questionnaireType
                - Intent actualizado correctamente
            """.trimIndent())

            // TODO: Si necesitas navegar programáticamente al cuestionario, hazlo aquí
            // Ejemplo: navigationController.navigate("questionnaire/$questionnaireType")
        }
    }

    /**
     * ✅ CORREGIDO: Sincroniza notificaciones con Firebase
     *
     * Cambios:
     * 1. Llamar a syncWithFirebase() PRIMERO (suspend)
     * 2. Luego llamar a checkAndGenerateNotifications() (NO suspend)
     */
    private fun syncNotificationsOnResume() {
        lifecycleScope.launch {
            try {
                val appNotificationManager = QuestionnaireNotificationManager(this@MainActivity)
                val userId = FirebaseAuth.getInstance().currentUser?.uid

                if (userId != null) {
                    // ✅ Sincronizar con Firebase (elimina notificaciones obsoletas)
                    withContext(Dispatchers.IO) {
                        appNotificationManager.syncWithFirebase(userId)
                    }

                    android.util.Log.d(TAG, "✅ Sincronización completada")
                } else {
                    android.util.Log.w(TAG, "⚠️ Usuario no autenticado")
                }
            } catch (e: Exception) {
                android.util.Log.e(TAG, "❌ Error sincronizando", e)
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
    val application = context.applicationContext as Application // Obtenido para el factory de ScoringViewModel

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

        // ✅ CUESTIONARIO INICIAL: Mantiene el recálculo de scores
        composable(Screen.Questionnaire.route) {
            // ✅ CORRECCIÓN: Inicializar el ViewModel DENTRO del contexto Composable
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
                    // ✅ Usar la instancia ya creada (sin llamar a viewModel() de nuevo)
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
                mainNavController = navController,
                // ⭐ AGREGAR: Callback para análisis de burnout
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

                    navController.navigate(Screen.BurnoutAnalysis.route) {
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

        composable(Screen.BurnoutAnalysis.route) {
            BurnoutAnalysisScreen(
                viewModel = burnoutViewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // --- CUESTIONARIOS ESPECÍFICOS: OPTIMIZADOS (SIN RECÁLCULO DE SCORES) ---

        // ===== CUESTIONARIO 1: Ergonomía =====
        composable(Screen.ErgonomiaQuestionnaire.route) {
            ErgonomiaQuestionnaireScreen(
                onComplete = {
                    notificationViewModel.markQuestionnaireCompleted(QuestionnaireType.ERGONOMIA)

                    // ✅ OPTIMIZADO: Eliminado el recálculo
                    Toast.makeText(
                        context,
                        "✅ Cuestionario de Ergonomía completado. El análisis se actualizará automáticamente.",
                        Toast.LENGTH_SHORT
                    ).show()
                    navController.popBackStack()
                },
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        // ===== CUESTIONARIO 2: Estrés y Salud Mental =====
        composable(Screen.EstresSaludMentalQuestionnaire.route) {
            EstresSaludMentalQuestionnaireScreen(
                onComplete = {
                    notificationViewModel.markQuestionnaireCompleted(QuestionnaireType.ESTRES_SALUD_MENTAL)

                    // ✅ OPTIMIZADO: Eliminado el recálculo
                    Toast.makeText(
                        context,
                        "✅ Cuestionario de Estrés completado.",
                        Toast.LENGTH_SHORT
                    ).show()
                    navController.popBackStack()
                },
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        // ===== CUESTIONARIO 3: Síntomas Musculares =====
        composable(Screen.SintomasMuscularesQuestionnaire.route) {
            SintomasMuscularesQuestionnaireScreen(
                onComplete = {
                    notificationViewModel.markQuestionnaireCompleted(QuestionnaireType.SINTOMAS_MUSCULARES)

                    // ✅ OPTIMIZADO: Eliminado el recálculo
                    Toast.makeText(
                        context,
                        "✅ Cuestionario completado.",
                        Toast.LENGTH_SHORT
                    ).show()
                    navController.popBackStack()
                },
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        // ===== CUESTIONARIO 4: Carga de Trabajo =====
        composable(Screen.CargaTrabajoQuestionnaire.route) {
            CargaTrabajoQuestionnaireScreen(
                onComplete = {
                    notificationViewModel.markQuestionnaireCompleted(QuestionnaireType.CARGA_TRABAJO)

                    // ✅ OPTIMIZADO: Eliminado el recálculo
                    Toast.makeText(
                        context,
                        "✅ Cuestionario completado.",
                        Toast.LENGTH_SHORT
                    ).show()
                    navController.popBackStack()
                },
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        // ===== CUESTIONARIO 5: Síntomas Visuales =====
        composable(Screen.SintomasVisualesQuestionnaire.route) {
            SintomasVisualesQuestionnaireScreen(
                onComplete = {
                    notificationViewModel.markQuestionnaireCompleted(QuestionnaireType.SINTOMAS_VISUALES)

                    // ✅ OPTIMIZADO: Eliminado el recálculo
                    Toast.makeText(
                        context,
                        "✅ Cuestionario completado.",
                        Toast.LENGTH_SHORT
                    ).show()
                    navController.popBackStack()
                },
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        // ===== CUESTIONARIO 6: Actividad Física =====
        composable(Screen.ActividadFisicaQuestionnaire.route) {
            ActividadFisicaQuestionnaireScreen(
                onComplete = {
                    notificationViewModel.markQuestionnaireCompleted(QuestionnaireType.ACTIVIDAD_FISICA)

                    // ✅ OPTIMIZADO: Eliminado el recálculo
                    Toast.makeText(
                        context,
                        "✅ Cuestionario completado.",
                        Toast.LENGTH_SHORT
                    ).show()
                    navController.popBackStack()
                },
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        // ===== CUESTIONARIO 7: Hábitos de Sueño =====
        composable(Screen.HabitosSuenoQuestionnaire.route) {
            HabitosSuenoQuestionnaireScreen(
                onComplete = {
                    notificationViewModel.markQuestionnaireCompleted(QuestionnaireType.HABITOS_SUENO)

                    // ✅ OPTIMIZADO: Eliminado el recálculo
                    Toast.makeText(
                        context,
                        "✅ Cuestionario completado.",
                        Toast.LENGTH_SHORT
                    ).show()
                    navController.popBackStack()
                },
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        // ===== CUESTIONARIO 8: Balance Vida-Trabajo =====
        composable(Screen.BalanceVidaTrabajoQuestionnaire.route) {
            BalanceVidaTrabajoQuestionnaireScreen(
                onComplete = {
                    notificationViewModel.markQuestionnaireCompleted(QuestionnaireType.BALANCE_VIDA_TRABAJO)

                    // ✅ OPTIMIZADO: Eliminado el recálculo
                    Toast.makeText(
                        context,
                        "✅ Cuestionario completado.",
                        Toast.LENGTH_SHORT
                    ).show()
                    navController.popBackStack()
                },
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        // ===== OTRAS PANTALLAS (se mantienen igual) =====

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