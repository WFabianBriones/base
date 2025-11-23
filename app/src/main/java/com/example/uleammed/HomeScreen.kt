package com.example.uleammed

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.uleammed.notifications.NotificationViewModel
import com.example.uleammed.notifications.NotificationsContent
import com.example.uleammed.questionnaires.QuestionnaireInfo
import com.example.uleammed.questionnaires.QuestionnaireType

/**
 * ✅ Función principal HomeScreen (ÚNICA)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onLogout: () -> Unit,
    onNavigateToQuestionnaire: (String) -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToResourceDetail: (String) -> Unit,  // ✅ Parámetro necesario
    authViewModel: AuthViewModel = viewModel(),
    notificationViewModel: NotificationViewModel = viewModel()
) {
    val navController = rememberNavController()
    val currentUser by authViewModel.currentUser.collectAsState()
    val unreadCount by notificationViewModel.unreadCount.collectAsState()

    LaunchedEffect(Unit) {
        android.util.Log.d("HomeScreen", "🔄 Recargando notificaciones...")
        notificationViewModel.loadNotifications()
        notificationViewModel.checkForNewNotifications()
    }

    LaunchedEffect(currentUser) {
        if (currentUser != null) {
            android.util.Log.d("HomeScreen", "👤 Usuario detectado, verificando notificaciones...")
            notificationViewModel.checkForNewNotifications()
        }
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
                com.example.uleammed.resources.ResourcesContentNew(
                    onResourceClick = { resourceId ->
                        // Navegar a visor de artículos
                        navController.navigate(Screen.ArticleViewer.createRoute(resourceId)) {
                            launchSingleTop = true
                        }
                    },
                    onExerciseClick = { exerciseId ->
                        // ✅ NUEVO: Navegar a ejercicio guiado
                        navController.navigate(Screen.ExerciseGuided.createRoute(exerciseId)) {
                            launchSingleTop = true
                        }
                    }
                )
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

/**
 * Bottom Navigation Bar
 */
@Composable
fun BottomNavigationBar(
    navController: NavHostController,
    unreadCount: Int
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    LaunchedEffect(unreadCount) {
        android.util.Log.d("BottomNav", "📊 Badge actualizado: $unreadCount notificaciones pendientes")
    }

    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp
    ) {
        bottomNavItems.forEach { item ->
            NavigationBarItem(
                icon = {
                    if (item.route == Screen.Notifications.route && unreadCount > 0) {
                        BadgedBox(
                            badge = {
                                Badge(
                                    containerColor = MaterialTheme.colorScheme.error,
                                    contentColor = MaterialTheme.colorScheme.onError
                                ) {
                                    Text(
                                        text = if (unreadCount > 99) "99+" else unreadCount.toString(),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        ) {
                            Icon(item.icon, contentDescription = item.title)
                        }
                    } else {
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
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}

/**
 * Contenido de la pestaña Home
 */
@Composable
fun HomeContent(userName: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Text(
            text = "Hola, $userName",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Text(
            text = "Bienvenido a tu panel de salud",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Filled.Dashboard,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Dashboard en construcción",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Aquí verás estadísticas, gráficos y resúmenes de tu salud laboral",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Contenido de la pestaña Explorar
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExploreContent(onNavigateToQuestionnaire: (String) -> Unit) {
    val questionnaireList = remember {
        listOf(
            QuestionnaireInfo(
                type = QuestionnaireType.ERGONOMIA,
                title = "Ergonomía y Ambiente",
                description = "Evalúa tu espacio de trabajo",
                icon = Icons.Filled.Computer,
                estimatedTime = "8-10 min",
                totalQuestions = 22
            ),
            QuestionnaireInfo(
                type = QuestionnaireType.SINTOMAS_MUSCULARES,
                title = "Síntomas Músculo-Esqueléticos",
                description = "Identifica dolores y molestias",
                icon = Icons.Filled.MonitorHeart,
                estimatedTime = "6-8 min",
                totalQuestions = 18
            ),
            QuestionnaireInfo(
                type = QuestionnaireType.SINTOMAS_VISUALES,
                title = "Síntomas Visuales",
                description = "Evalúa fatiga ocular",
                icon = Icons.Filled.RemoveRedEye,
                estimatedTime = "4-5 min",
                totalQuestions = 14
            ),
            QuestionnaireInfo(
                type = QuestionnaireType.CARGA_TRABAJO,
                title = "Carga de Trabajo",
                description = "Analiza demanda laboral",
                icon = Icons.Filled.Work,
                estimatedTime = "5-7 min",
                totalQuestions = 15
            ),
            QuestionnaireInfo(
                type = QuestionnaireType.ESTRES_SALUD_MENTAL,
                title = "Estrés y Salud Mental",
                description = "Identifica niveles de estrés",
                icon = Icons.Filled.Psychology,
                estimatedTime = "7-9 min",
                totalQuestions = 19
            ),
            QuestionnaireInfo(
                type = QuestionnaireType.HABITOS_SUENO,
                title = "Hábitos de Sueño",
                description = "Evalúa calidad de descanso",
                icon = Icons.Filled.NightlightRound,
                estimatedTime = "3-4 min",
                totalQuestions = 9
            ),
            QuestionnaireInfo(
                type = QuestionnaireType.ACTIVIDAD_FISICA,
                title = "Actividad Física",
                description = "Analiza hábitos de ejercicio",
                icon = Icons.Filled.SportsGymnastics,
                estimatedTime = "4-5 min",
                totalQuestions = 10
            ),
            QuestionnaireInfo(
                type = QuestionnaireType.BALANCE_VIDA_TRABAJO,
                title = "Balance Vida-Trabajo",
                description = "Evalúa equilibrio personal",
                icon = Icons.Filled.Scale,
                estimatedTime = "3-4 min",
                totalQuestions = 8
            )
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Text(
            text = "Cuestionarios Disponibles",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(questionnaireList) { questionnaire ->
                QuestionnaireCard(
                    questionnaire = questionnaire,
                    onClick = {
                        val route = when (questionnaire.type) {
                            QuestionnaireType.ERGONOMIA -> Screen.ErgonomiaQuestionnaire.route
                            QuestionnaireType.SINTOMAS_MUSCULARES -> Screen.SintomasMuscularesQuestionnaire.route
                            QuestionnaireType.SINTOMAS_VISUALES -> Screen.SintomasVisualesQuestionnaire.route
                            QuestionnaireType.CARGA_TRABAJO -> Screen.CargaTrabajoQuestionnaire.route
                            QuestionnaireType.ESTRES_SALUD_MENTAL -> Screen.EstresSaludMentalQuestionnaire.route
                            QuestionnaireType.HABITOS_SUENO -> Screen.HabitosSuenoQuestionnaire.route
                            QuestionnaireType.ACTIVIDAD_FISICA -> Screen.ActividadFisicaQuestionnaire.route
                            QuestionnaireType.BALANCE_VIDA_TRABAJO -> Screen.BalanceVidaTrabajoQuestionnaire.route
                        }
                        onNavigateToQuestionnaire(route)
                    }
                )
            }
        }
    }
}

/**
 * ✅ NUEVO: Card de cuestionario individual
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuestionnaireCard(
    questionnaire: QuestionnaireInfo,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(56.dp)
            ) {
                Icon(
                    imageVector = questionnaire.icon,
                    contentDescription = null,
                    modifier = Modifier.padding(12.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = questionnaire.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = questionnaire.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AccessTime,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = questionnaire.estimatedTime,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Assignment,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${questionnaire.totalQuestions} preguntas",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Contenido de la pestaña Recursos
 */
@Composable
fun ResourcesContent(
    onNavigateToResourceDetail: (String) -> Unit
) {
    com.example.uleammed.resources.ResourcesContentNew(
        onResourceClick = onNavigateToResourceDetail
    )
}