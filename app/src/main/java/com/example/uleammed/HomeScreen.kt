package com.example.uleammed

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.tooling.preview.Preview

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onLogout: () -> Unit,
    onNavigateToQuestionnaire: (String) -> Unit,
    authViewModel: AuthViewModel = viewModel()
) {
    val navController = rememberNavController()
    val currentUser by authViewModel.currentUser.collectAsState()

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
            BottomNavigationBar(navController = navController)
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
                NotificationsContent()
            }
            composable(Screen.Resources.route) {
                ResourcesContent()
            }
            composable(Screen.Profile.route) {
                ProfileContent(
                    user = currentUser,
                    onLogout = onLogout
                )
            }
        }
    }
}

@Composable
fun BottomNavigationBar(navController: NavHostController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp
    ) {
        bottomNavItems.forEach { item ->
            NavigationBarItem(
                icon = { Icon(item.icon, contentDescription = item.title) },
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

@Composable
fun HomeContent(userName: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.Start
    ) {
        // Bienvenida
        Text(
            text = "¡Bienvenido!",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Text(
            text = userName,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(top = 4.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Tarjetas de información
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Estado de Salud",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Tu cuestionario de salud ha sido completado. Revisa tu perfil para más detalles.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Recomendaciones",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "• Mantén una rutina de ejercicio regular\n• Hidrátate adecuadamente\n• Descansa lo suficiente",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Próximas Actividades",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "No hay actividades programadas",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
        }
    }
}

@Composable
fun ExploreContent(onNavigateToQuestionnaire: (String) -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Cuestionarios de Salud",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Text(
            text = "Completa estos cuestionarios para un diagnóstico más detallado",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
        )

        // Lista de cuestionarios
        val questionnaires = listOf(
            QuestionnaireInfo(
                type = QuestionnaireType.ERGONOMIA,
                title = "Ergonomía y Ambiente de Trabajo",
                description = "Evalúa tu espacio de trabajo, mobiliario y ambiente laboral",
                icon = Icons.Filled.Chair,
                estimatedTime = "8-10 min",
                totalQuestions = 22
            ),
            QuestionnaireInfo(
                type = QuestionnaireType.SINTOMAS_MUSCULARES,
                title = "Síntomas Músculo-Esqueléticos",
                description = "Identifica dolores y molestias relacionadas con tu postura",
                icon = Icons.Filled.Healing,
                estimatedTime = "6-8 min",
                totalQuestions = 18
            ),
            QuestionnaireInfo(
                type = QuestionnaireType.SINTOMAS_VISUALES,
                title = "Síntomas Visuales",
                description = "Evalúa el síndrome visual informático y fatiga ocular",
                icon = Icons.Filled.Visibility,
                estimatedTime = "4-5 min",
                totalQuestions = 14
            ),
            QuestionnaireInfo(
                type = QuestionnaireType.CARGA_TRABAJO,
                title = "Carga de Trabajo",
                description = "Analiza la demanda laboral y factores psicosociales",
                icon = Icons.Filled.WorkHistory,
                estimatedTime = "5-7 min",
                totalQuestions = 15
            ),
            QuestionnaireInfo(
                type = QuestionnaireType.ESTRES_SALUD_MENTAL,
                title = "Estrés y Salud Mental",
                description = "Identifica niveles de estrés, burnout y bienestar emocional",
                icon = Icons.Filled.Psychology,
                estimatedTime = "7-9 min",
                totalQuestions = 19
            ),
            QuestionnaireInfo(
                type = QuestionnaireType.HABITOS_SUENO,
                title = "Hábitos de Sueño",
                description = "Evalúa la calidad y cantidad de tu descanso nocturno",
                icon = Icons.Filled.Bedtime,
                estimatedTime = "3-4 min",
                totalQuestions = 9
            ),
            QuestionnaireInfo(
                type = QuestionnaireType.ACTIVIDAD_FISICA,
                title = "Actividad Física y Nutrición",
                description = "Analiza tus hábitos de ejercicio y alimentación",
                icon = Icons.Filled.FitnessCenter,
                estimatedTime = "4-5 min",
                totalQuestions = 10
            ),
            QuestionnaireInfo(
                type = QuestionnaireType.BALANCE_VIDA_TRABAJO,
                title = "Balance Vida-Trabajo",
                description = "Evalúa el equilibrio entre tu vida personal y profesional",
                icon = Icons.Filled.Balance,
                estimatedTime = "3-4 min",
                totalQuestions = 8
            )
        )

        questionnaires.forEach { info ->
            QuestionnaireCard(
                info = info,
                onClick = {
                    // Navegar según el tipo de cuestionario
                    val route = when (info.type) {
                        QuestionnaireType.ERGONOMIA -> Screen.ErgonomiaQuestionnaire.route
                        QuestionnaireType.SINTOMAS_MUSCULARES -> Screen.SintomasMuscularesQuestionnaire.route
                        QuestionnaireType.ESTRES_SALUD_MENTAL -> Screen.EstresSaludMentalQuestionnaire.route
                        QuestionnaireType.CARGA_TRABAJO -> Screen.CargaTrabajoQuestionnaire.route
                        QuestionnaireType.SINTOMAS_VISUALES -> Screen.SintomasVisualesQuestionnaire.route
                        QuestionnaireType.ACTIVIDAD_FISICA -> Screen.ActividadFisicaQuestionnaire.route
                        QuestionnaireType.HABITOS_SUENO -> Screen.HabitosSuenoQuestionnaire.route
                        QuestionnaireType.BALANCE_VIDA_TRABAJO -> Screen.BalanceVidaTrabajoQuestionnaire.route
                        else -> {
                            // Para los cuestionarios que aún no están implementados
                            android.widget.Toast.makeText(
                                context,
                                "Próximamente: ${info.title}",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                            null
                        }
                    }

                    route?.let { onNavigateToQuestionnaire(it) }
                }
            )
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuestionnaireCard(
    info: QuestionnaireInfo,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icono
            Surface(
                modifier = Modifier.size(56.dp),
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        imageVector = info.icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Información
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = info.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = info.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Timer,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = info.estimatedTime,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Icon(
                        imageVector = Icons.Filled.Quiz,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${info.totalQuestions} preguntas",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Flecha
            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun NotificationsContent() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Text(
            text = "Notificaciones",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
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
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Filled.Notifications,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "No tienes notificaciones",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Las notificaciones importantes aparecerán aquí",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun ResourcesContent() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Text(
            text = "Recursos",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "📚 Biblioteca de Salud",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Accede a artículos y guías sobre salud laboral",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "💪 Ejercicios Recomendados",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Rutinas adaptadas para personal académico",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "🧘 Salud Mental",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Técnicas de relajación y manejo del estrés",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
        }
    }
}