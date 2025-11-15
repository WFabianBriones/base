package com.example.uleammed

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
        setContent {
            UleamAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    UleamApp()
                }
            }
        }
    }
}

@Composable
fun UleamApp() {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = viewModel()
    val notificationViewModel: NotificationViewModel = viewModel()
    val context = LocalContext.current

    // Verificar notificaciones cuando la app inicia
    LaunchedEffect(Unit) {
        notificationViewModel.checkForNewNotifications()
    }

    NavHost(
        navController = navController,
        startDestination = Screen.Login.route
    ) {
        // Pantalla de Login
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

        // Pantalla de Registro
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

        // Pantalla de Cuestionario (solo primera vez)
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

        // Pantalla Home (con Bottom Navigation)
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

        // Pantalla de Configuración
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
                    // Marcar como completado
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