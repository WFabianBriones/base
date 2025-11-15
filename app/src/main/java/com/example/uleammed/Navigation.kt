package com.example.uleammed

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

// Rutas de navegación
sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Register : Screen("register")
    object Questionnaire : Screen("questionnaire")
    object Home : Screen("home")
    object Profile : Screen("profile")
    object Explore : Screen("explore")
    object Notifications : Screen("notifications")
    object Resources : Screen("resources")
    object Settings : Screen("settings") // NUEVA RUTA

    // Cuestionarios específicos
    object ErgonomiaQuestionnaire : Screen("ergonomia_questionnaire")
    object EstresSaludMentalQuestionnaire : Screen("estres_questionnaire")
    object SintomasMuscularesQuestionnaire : Screen("sintomas_musculares_questionnaire")
    object CargaTrabajoQuestionnaire : Screen("carga_trabajo_questionnaire")
    object SintomasVisualesQuestionnaire : Screen("sintomas_visuales_questionnaire")
    object HabitosSuenoQuestionnaire : Screen("habitos_sueno_questionnaire")
    object ActividadFisicaQuestionnaire : Screen("actividad_fisica_questionnaire")
    object BalanceVidaTrabajoQuestionnaire : Screen("balance_vida_trabajo_questionnaire")
}

// Items del Bottom Navigation
sealed class BottomNavItem(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    object Home : BottomNavItem(
        route = Screen.Home.route,
        title = "Inicio",
        icon = Icons.Filled.Home
    )
    object Explore : BottomNavItem(
        route = Screen.Explore.route,
        title = "Explorar",
        icon = Icons.Filled.Explore
    )
    object Notifications : BottomNavItem(
        route = Screen.Notifications.route,
        title = "Avisos",
        icon = Icons.Filled.Notifications
    )
    object Resources : BottomNavItem(
        route = Screen.Resources.route,
        title = "Recursos",
        icon = Icons.Filled.LibraryBooks
    )
    object Profile : BottomNavItem(
        route = Screen.Profile.route,
        title = "Perfil",
        icon = Icons.Filled.Person
    )
}

val bottomNavItems = listOf(
    BottomNavItem.Home,
    BottomNavItem.Explore,
    BottomNavItem.Notifications,
    BottomNavItem.Resources,
    BottomNavItem.Profile
)