package com.example.uleammed

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onLogout: () -> Unit,
    onNavigateToQuestionnaire: (String) -> Unit,
    onNavigateToSettings: () -> Unit,
    authViewModel: AuthViewModel = viewModel(),
    notificationViewModel: NotificationViewModel = viewModel()
) {
    val navController = rememberNavController()
    val currentUser by authViewModel.currentUser.collectAsState()
    val unreadCount by notificationViewModel.unreadCount.collectAsState()

    // Actualizar notificaciones cuando entra a Home
    LaunchedEffect(Unit) {
        notificationViewModel.checkForNewNotifications()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "ULEAM Salud",
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        bottomBar = {
            BottomNavigationBar(
                navController = navController,
                unreadCount = unreadCount
            )
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(paddingValues)
        ) {
            composable(Screen.Home.route) {
                HomeContent(userName = currentUser?.displayName ?: "Usuario")
            }
            composable(Screen.Explore.route) {
                ExploreContent(onNavigateToQuestionnaire = onNavigateToQuestionnaire)
            }
            composable(Screen.Notifications.route) {
                NotificationsContent(onNavigateToQuestionnaire = onNavigateToQuestionnaire)
            }
            composable(Screen.Resources.route) {
                ResourcesContent()
            }
            composable(Screen.Profile.route) {
                ProfileContent(
                    user = currentUser,
                    onLogout = onLogout,
                    onNavigateToSettings = onNavigateToSettings
                )
            }
        }
    }
}

@Composable
fun BottomNavigationBar(
    navController: NavHostController,
    unreadCount: Int
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp
    ) {
        bottomNavItems.forEach { item ->
            NavigationBarItem(
                icon = {
                    BadgedBox(
                        badge = {
                            if (item.route == Screen.Notifications.route && unreadCount > 0) {
                                Badge {
                                    Text(
                                        text = if (unreadCount > 99) "99+" else unreadCount.toString(),
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }
                        }
                    ) {
                        Icon(item.icon, contentDescription = item.title)
                    }
                },
                label = { Text(item.title) },
                selected = currentRoute == item.route,
                onClick = {
                    navController.navigate(item.route) {
                        popUpTo(Screen.Home.route) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    }
}

// Nota: HomeContent, ExploreContent y ResourcesContent permanecen igual
// Solo se actualizan las referencias en composable