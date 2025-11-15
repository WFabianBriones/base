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
    val context = LocalContext.current

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
                }
            )
        }

        // Cuestionarios específicos
        composable(Screen.ErgonomiaQuestionnaire.route) {
            ErgonomiaQuestionnaireScreen(
                onComplete = {
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