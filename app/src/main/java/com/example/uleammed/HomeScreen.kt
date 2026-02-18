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
import com.example.uleammed.auth.AuthViewModel
import com.example.uleammed.auth.AuthRepository
import com.example.uleammed.auth.QuestionnaireStatus
import com.example.uleammed.notifications.NotificationsContent
import com.example.uleammed.notifications.NotificationViewModel
import com.example.uleammed.questionnaires.QuestionnaireType
import com.example.uleammed.scoring.ScoringViewModel
import androidx.compose.ui.platform.LocalContext
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import androidx.compose.ui.draw.alpha
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.util.Log

/**
 * Función principal HomeScreen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onLogout: () -> Unit,
    onNavigateToQuestionnaire: (String) -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToResourceDetail: (String) -> Unit,
    mainNavController: NavHostController,
    onNavigateToBurnoutAnalysis: (Map<String, Float>) -> Unit = {},
    authViewModel: AuthViewModel = viewModel(),
    notificationViewModel: NotificationViewModel = viewModel()
) {
    val navController = rememberNavController()
    val currentUser by authViewModel.currentUser.collectAsState()
    val unreadCount by notificationViewModel.unreadCount.collectAsState()

    val shouldShowDialog by notificationViewModel.shouldShowSaludGeneralDialog.collectAsState()
    val isCheckingDialog by notificationViewModel.isCheckingSaludGeneral.collectAsState()

    LaunchedEffect(currentUser) {
        currentUser?.let { user ->
            Log.d("HomeScreen", "👤 Usuario detectado: ${user.uid}")
            Log.d("HomeScreen", "🔍 Verificando Salud General para: ${user.uid}")
            notificationViewModel.checkShouldShowSaludGeneralDialog(user.uid)
        }
    }

    if (shouldShowDialog) {
        Log.d("HomeScreen", "🎨 Mostrando dialog de Salud General")
        SaludGeneralDialog(
            onStart = {
                Log.d("HomeScreen", "➡️ Navegando a cuestionario de Salud General")
                notificationViewModel.dismissSaludGeneralDialog()
                onNavigateToQuestionnaire(Screen.Questionnaire.route)
            }
        )
    }

    LaunchedEffect(Unit) {
        Log.d("HomeScreen", "🔄 Recargando notificaciones...")
        notificationViewModel.loadNotifications()
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
                HomeContent(
                    userName = currentUser?.displayName ?: "Usuario",
                    onNavigateToBurnoutAnalysis = onNavigateToBurnoutAnalysis
                )
            }
            composable(Screen.Explore.route) {
                ExploreContent(
                    onNavigateToQuestionnaire = onNavigateToQuestionnaire,
                    notificationViewModel = notificationViewModel
                )
            }
            composable(Screen.Notifications.route) {
                NotificationsContent(onNavigateToQuestionnaire = onNavigateToQuestionnaire)
            }
            composable(Screen.Resources.route) {
                com.example.uleammed.resources.ResourcesContentNew(
                    onResourceClick = { resourceId ->
                        mainNavController.navigate(Screen.ArticleViewer.createRoute(resourceId)) {
                            launchSingleTop = true
                        }
                    },
                    onExerciseClick = { exerciseId ->
                        mainNavController.navigate(Screen.ExerciseGuided.createRoute(exerciseId)) {
                            launchSingleTop = true
                        }
                    }
                )
            }
            composable(Screen.Profile.route) {
                com.example.uleammed.perfil.ProfileContent(
                    user = currentUser,
                    onLogout = onLogout,
                    onNavigateToSettings = onNavigateToSettings,
                    onNavigateToEditProfile = {
                        mainNavController.navigate(Screen.EditProfile.route) {
                            launchSingleTop = true
                        }
                    },
                    onNavigateToViewQuestionnaire = {
                        mainNavController.navigate(Screen.ViewQuestionnaire.route) {
                            launchSingleTop = true
                        }
                    },
                    onNavigateToHelp = {
                        mainNavController.navigate(Screen.HelpSupport.route) {
                            launchSingleTop = true
                        }
                    }
                )
            }
        }
    }
}

/**
 * Bottom Navigation Bar
 *
 * 🐛 FIX #2: Cuando el usuario ya está en Inicio y lo vuelve a tocar,
 * se fuerza una re-navegación con popUpTo inclusive=true para que
 * HomeContent se recomponga y recargue los datos.
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
                    // ✅ FIX #2: Si ya estamos en Inicio y se vuelve a tocar,
                    // forzar recarga destruyendo y recreando el composable
                    if (currentRoute == item.route && item.route == Screen.Home.route) {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Home.route) { inclusive = true }
                        }
                    } else {
                        navController.navigate(item.route) {
                            popUpTo(Screen.Home.route) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
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
 *
 * 🐛 FIX #1: Se crea UN SOLO ScoringViewModel aquí y se pasa a HealthDashboard.
 * Antes, HomeContent creaba su instancia con factory, pero HealthDashboard
 * llamaba viewModel() y obtenía una INSTANCIA DIFERENTE → el dashboard
 * siempre veía datos vacíos aunque loadScoreWithSmartRefresh() había cargado
 * los datos en otra instancia.
 */
@Composable
fun HomeContent(
    userName: String,
    onNavigateToBurnoutAnalysis: (Map<String, Float>) -> Unit
) {
    val context = LocalContext.current

    // ✅ FIX #1: Crear el ViewModel UNA SOLA VEZ aquí con el factory correcto
    val scoringViewModel: ScoringViewModel = viewModel(
        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return ScoringViewModel(context.applicationContext as android.app.Application) as T
            }
        }
    )

    LaunchedEffect(Unit) {
        android.util.Log.d("HomeScreen", "🔄 Cargando scores con smart refresh...")
        scoringViewModel.loadScoreWithSmartRefresh()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            )
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.FavoriteBorder,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Column {
                    Text(
                        text = "Hola, $userName",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Así está tu salud laboral",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // ✅ FIX #1: Pasar el mismo scoringViewModel al HealthDashboard
        // para que ambos compartan la misma instancia y los mismos datos
        com.example.uleammed.scoring.HealthDashboard(
            viewModel = scoringViewModel,
            onNavigateToBurnoutAnalysis = onNavigateToBurnoutAnalysis
        )
    }
}

/**
 * Contenido de la pestaña Explorar con sistema de expiración integrado
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExploreContent(
    onNavigateToQuestionnaire: (String) -> Unit,
    notificationViewModel: NotificationViewModel = viewModel()
) {
    val repository = remember { AuthRepository() }
    val userId = FirebaseAuth.getInstance().currentUser?.uid

    val scheduleConfig = notificationViewModel.scheduleConfig.collectAsState()
    val periodDays = scheduleConfig.value?.periodDays ?: 7

    var completedQuestionnaires by remember { mutableStateOf<Set<String>>(emptySet()) }
    var isLoading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(periodDays, userId) {
        scope.launch {
            if (userId != null) {
                isLoading = true
                val result = repository.getCompletedQuestionnaires(userId, periodDays)
                result.onSuccess { completed ->
                    completedQuestionnaires = completed
                    isLoading = false
                    android.util.Log.d("ExploreContent",
                        "🔄 Cuestionarios recargados con período de $periodDays días")
                }.onFailure {
                    isLoading = false
                    android.util.Log.e("ExploreContent", "❌ Error recargando cuestionarios", it)
                }
            } else {
                isLoading = false
            }
        }
    }

    val questionnaireList = remember {
        listOf(
            QuestionnaireInfo(
                type = QuestionnaireType.ERGONOMIA,
                title = "Ergonomía",
                description = "Evalúa tu estación de trabajo",
                icon = Icons.Filled.Chair,
                estimatedTime = "8-10 min",
                totalQuestions = 22,
                firestoreId = "ergonomia"
            ),
            QuestionnaireInfo(
                type = QuestionnaireType.SINTOMAS_MUSCULARES,
                title = "Síntomas Musculares",
                description = "Identifica molestias físicas",
                icon = Icons.Filled.Accessibility,
                estimatedTime = "6-8 min",
                totalQuestions = 17,
                firestoreId = "sintomas_musculares"
            ),
            QuestionnaireInfo(
                type = QuestionnaireType.SINTOMAS_VISUALES,
                title = "Síntomas Visuales",
                description = "Detecta fatiga ocular",
                icon = Icons.Filled.RemoveRedEye,
                estimatedTime = "4-5 min",
                totalQuestions = 14,
                firestoreId = "sintomas_visuales"
            ),
            QuestionnaireInfo(
                type = QuestionnaireType.CARGA_TRABAJO,
                title = "Carga de Trabajo",
                description = "Analiza demanda laboral",
                icon = Icons.Filled.Work,
                estimatedTime = "5-7 min",
                totalQuestions = 15,
                firestoreId = "carga_trabajo"
            ),
            QuestionnaireInfo(
                type = QuestionnaireType.ESTRES_SALUD_MENTAL,
                title = "Estrés y Salud Mental",
                description = "Identifica niveles de estrés",
                icon = Icons.Filled.Psychology,
                estimatedTime = "7-9 min",
                totalQuestions = 19,
                firestoreId = "estres_salud_mental"
            ),
            QuestionnaireInfo(
                type = QuestionnaireType.HABITOS_SUENO,
                title = "Hábitos de Sueño",
                description = "Evalúa calidad de descanso",
                icon = Icons.Filled.NightlightRound,
                estimatedTime = "3-4 min",
                totalQuestions = 9,
                firestoreId = "habitos_sueno"
            ),
            QuestionnaireInfo(
                type = QuestionnaireType.ACTIVIDAD_FISICA,
                title = "Actividad Física",
                description = "Analiza hábitos de ejercicio",
                icon = Icons.Filled.SportsGymnastics,
                estimatedTime = "4-5 min",
                totalQuestions = 10,
                firestoreId = "actividad_fisica"
            ),
            QuestionnaireInfo(
                type = QuestionnaireType.BALANCE_VIDA_TRABAJO,
                title = "Balance Vida-Trabajo",
                description = "Evalúa equilibrio personal",
                icon = Icons.Filled.Scale,
                estimatedTime = "3-4 min",
                totalQuestions = 8,
                firestoreId = "balance_vida_trabajo"
            )
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Cuestionarios Disponibles",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = when (periodDays) {
                        7 -> "Frecuencia: Semanal"
                        15 -> "Frecuencia: Quincenal"
                        30 -> "Frecuencia: Mensual"
                        else -> "Frecuencia: cada $periodDays días"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(questionnaireList) { questionnaire ->
                    QuestionnaireCardDynamic(
                        questionnaire = questionnaire,
                        isCompleted = completedQuestionnaires.contains(questionnaire.firestoreId),
                        userId = userId ?: "",
                        repository = repository,
                        periodDays = periodDays,
                        notificationViewModel = notificationViewModel,
                        onClick = {
                            if (!completedQuestionnaires.contains(questionnaire.firestoreId)) {
                                val route = when (questionnaire.type) {
                                    QuestionnaireType.ERGONOMIA -> Screen.ErgonomiaQuestionnaire.route
                                    QuestionnaireType.SINTOMAS_MUSCULARES -> Screen.SintomasMuscularesQuestionnaire.route
                                    QuestionnaireType.SINTOMAS_VISUALES -> Screen.SintomasVisualesQuestionnaire.route
                                    QuestionnaireType.CARGA_TRABAJO -> Screen.CargaTrabajoQuestionnaire.route
                                    QuestionnaireType.ESTRES_SALUD_MENTAL -> Screen.EstresSaludMentalQuestionnaire.route
                                    QuestionnaireType.HABITOS_SUENO -> Screen.HabitosSuenoQuestionnaire.route
                                    QuestionnaireType.ACTIVIDAD_FISICA -> Screen.ActividadFisicaQuestionnaire.route
                                    QuestionnaireType.BALANCE_VIDA_TRABAJO -> Screen.BalanceVidaTrabajoQuestionnaire.route
                                    else -> return@QuestionnaireCardDynamic
                                }
                                onNavigateToQuestionnaire(route)
                            } else {
                                scope.launch {
                                    if (userId != null) {
                                        val result = repository.getCompletedQuestionnaires(userId, periodDays)
                                        result.onSuccess { completed ->
                                            completedQuestionnaires = completed
                                        }
                                    }
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

/**
 * Card de cuestionario con umbrales dinámicos
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun QuestionnaireCardDynamic(
    questionnaire: QuestionnaireInfo,
    isCompleted: Boolean,
    userId: String,
    repository: AuthRepository,
    periodDays: Int,
    onClick: () -> Unit,
    notificationViewModel: NotificationViewModel
) {
    val context = LocalContext.current
    var status by remember { mutableStateOf<QuestionnaireStatus?>(null) }
    val scope = rememberCoroutineScope()
    var showDeleteDialog by remember { mutableStateOf(false) }
    var isDeleting by remember { mutableStateOf(false) }

    LaunchedEffect(isCompleted, periodDays) {
        if (isCompleted && userId.isNotEmpty()) {
            scope.launch {
                val result = repository.getQuestionnaireStatus(
                    userId,
                    questionnaire.firestoreId,
                    periodDays
                )
                result.onSuccess { s ->
                    status = s
                    android.util.Log.d("QuestionnaireCard",
                        "📊 Status actualizado para ${questionnaire.title}: $s (período: $periodDays días)")
                }
            }
        } else {
            status = null
        }
    }

    val criticalThreshold = (periodDays * 0.3).toInt().coerceAtLeast(1)
    val warningThreshold = (periodDays * 0.5).toInt().coerceAtLeast(2)
    val isLocked = isCompleted && status is QuestionnaireStatus.Completed

    val cardColor = when {
        !isCompleted -> MaterialTheme.colorScheme.surface
        status is QuestionnaireStatus.Completed -> {
            val daysRemaining = (status as QuestionnaireStatus.Completed).daysRemaining
            when {
                daysRemaining <= criticalThreshold -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                daysRemaining <= warningThreshold -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f)
                else -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
            }
        }
        else -> MaterialTheme.colorScheme.surface
    }

    val iconColor = when {
        !isCompleted -> MaterialTheme.colorScheme.primaryContainer
        status is QuestionnaireStatus.Completed -> {
            val daysRemaining = (status as QuestionnaireStatus.Completed).daysRemaining
            when {
                daysRemaining <= criticalThreshold -> MaterialTheme.colorScheme.error
                daysRemaining <= warningThreshold -> MaterialTheme.colorScheme.tertiary
                else -> MaterialTheme.colorScheme.secondary
            }
        }
        else -> MaterialTheme.colorScheme.primaryContainer
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            icon = {
                Icon(
                    Icons.Filled.DeleteForever,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = { Text("¿Eliminar cuestionario?") },
            text = {
                Column {
                    Text("¿Deseas eliminar el cuestionario \"${questionnaire.title}\"?")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Esto te permitirá completarlo nuevamente.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            isDeleting = true
                            val result = repository.deleteQuestionnaire(userId, questionnaire.firestoreId)
                            result.onSuccess {
                                try {
                                    Log.d("QuestionnaireCard", "🔄 Regenerando notificaciones después de eliminar...")
                                    withContext(Dispatchers.IO) {
                                        val notificationManager = com.example.uleammed.notifications.QuestionnaireNotificationManager(context)
                                        notificationManager.syncWithFirebase(userId)
                                    }
                                    notificationViewModel.checkAndGenerateNotifications()
                                    notificationViewModel.loadNotifications()
                                    Log.d("QuestionnaireCard", "✅ Notificaciones regeneradas exitosamente")
                                } catch (e: Exception) {
                                    Log.e("QuestionnaireCard", "❌ Error regenerando notificaciones", e)
                                }
                                Toast.makeText(
                                    context,
                                    "Cuestionario eliminado. Nuevas notificaciones programadas.",
                                    Toast.LENGTH_LONG
                                ).show()
                                showDeleteDialog = false
                                onClick()
                            }.onFailure { error ->
                                Toast.makeText(
                                    context,
                                    "Error al eliminar: ${error.message}",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                            isDeleting = false
                        }
                    },
                    enabled = !isDeleting
                ) {
                    if (isDeleting) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        Text("Eliminar", color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }, enabled = !isDeleting) {
                    Text("Cancelar")
                }
            }
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (isLocked) 0.6f else 1f)
            .combinedClickable(
                onClick = {
                    if (isLocked && status is QuestionnaireStatus.Completed) {
                        val daysRemaining = (status as QuestionnaireStatus.Completed).daysRemaining
                        val periodText = when (periodDays) {
                            7 -> "semanal"
                            15 -> "quincenal"
                            30 -> "mensual"
                            else -> "de $periodDays días"
                        }
                        val mensaje = when {
                            daysRemaining <= 0 -> "Este cuestionario estará disponible mañana. Recibirás una notificación."
                            daysRemaining == 1 -> "Este cuestionario ($periodText) estará disponible en 1 día. Recibirás una notificación."
                            else -> "Este cuestionario ($periodText) estará disponible en $daysRemaining días. Recibirás una notificación."
                        }
                        Toast.makeText(context, mensaje, Toast.LENGTH_LONG).show()
                    } else {
                        onClick()
                    }
                },
                onLongClick = {
                    if (isCompleted) {
                        showDeleteDialog = true
                    }
                }
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isLocked) 0.dp else 2.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = iconColor,
                modifier = Modifier.size(56.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = questionnaire.icon,
                        contentDescription = null,
                        modifier = Modifier.size(28.dp),
                        tint = when {
                            !isCompleted -> MaterialTheme.colorScheme.onPrimaryContainer
                            status is QuestionnaireStatus.Completed -> {
                                val daysRemaining = (status as QuestionnaireStatus.Completed).daysRemaining
                                when {
                                    daysRemaining <= criticalThreshold -> MaterialTheme.colorScheme.onError
                                    daysRemaining <= warningThreshold -> MaterialTheme.colorScheme.onTertiary
                                    else -> MaterialTheme.colorScheme.onSecondary
                                }
                            }
                            else -> MaterialTheme.colorScheme.onPrimaryContainer
                        }
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(text = questionnaire.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(text = questionnaire.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Filled.AccessTime, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(text = questionnaire.estimatedTime, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(imageVector = Icons.Filled.Assignment, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(text = "${questionnaire.totalQuestions} preguntas", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                if (isCompleted) {
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f),
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(imageVector = Icons.Filled.TouchApp, contentDescription = null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.onTertiaryContainer)
                            Text(text = "Mantén presionado para eliminar", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onTertiaryContainer, fontWeight = FontWeight.Normal)
                        }
                    }
                }

                if (isLocked && status is QuestionnaireStatus.Completed) {
                    val daysRemaining = (status as QuestionnaireStatus.Completed).daysRemaining
                    val periodText = when (periodDays) {
                        7 -> "Semanal"; 15 -> "Quincenal"; 30 -> "Mensual"
                        else -> "Cada $periodDays días"
                    }
                    Surface(shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.padding(top = 4.dp)) {
                        Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Filled.Lock, contentDescription = null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = when {
                                    daysRemaining <= 0 -> "Disponible mañana ($periodText)"
                                    daysRemaining == 1 -> "Disponible en 1 día ($periodText)"
                                    else -> "Disponible en $daysRemaining días ($periodText)"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                if (isCompleted && status is QuestionnaireStatus.Completed) {
                    val daysRemaining = (status as QuestionnaireStatus.Completed).daysRemaining
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = when {
                            daysRemaining <= criticalThreshold -> MaterialTheme.colorScheme.errorContainer
                            daysRemaining <= warningThreshold -> MaterialTheme.colorScheme.tertiaryContainer
                            else -> MaterialTheme.colorScheme.secondaryContainer
                        }
                    ) {
                        Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (daysRemaining <= criticalThreshold) Icons.Filled.Warning else Icons.Filled.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = when {
                                    daysRemaining <= criticalThreshold -> MaterialTheme.colorScheme.onError
                                    daysRemaining <= warningThreshold -> MaterialTheme.colorScheme.onTertiary
                                    else -> MaterialTheme.colorScheme.onSecondary
                                }
                            )
                            Text(
                                text = when {
                                    daysRemaining <= 0 -> "Vence hoy"
                                    daysRemaining == 1 -> "1 día restante"
                                    daysRemaining <= criticalThreshold -> "$daysRemaining días restantes"
                                    else -> "Completado"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = when {
                                    daysRemaining <= criticalThreshold -> MaterialTheme.colorScheme.onErrorContainer
                                    daysRemaining <= warningThreshold -> MaterialTheme.colorScheme.onTertiaryContainer
                                    else -> MaterialTheme.colorScheme.onSecondaryContainer
                                }
                            )
                        }
                    }
                }
            }

            if (!isLocked) {
                Icon(imageVector = Icons.Filled.ChevronRight, contentDescription = "Ir al cuestionario", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

/**
 * Data class para información de cuestionarios
 */
data class QuestionnaireInfo(
    val type: QuestionnaireType,
    val title: String,
    val description: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val estimatedTime: String,
    val totalQuestions: Int,
    val firestoreId: String
)

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

/**
 * Dialog obligatorio de Salud General
 */
@Composable
fun SaludGeneralDialog(
    onStart: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { /* No se puede cerrar */ },
        icon = {
            Icon(
                Icons.Filled.HealthAndSafety,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp)
            )
        },
        title = {
            Text("Reevaluación de Salud General", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        },
        text = {
            Column {
                Text("Es momento de actualizar tu evaluación de salud base.")
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Este cuestionario nos ayuda a monitorear cambios en tu estado de salud general y condiciones preexistentes.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.AccessTime, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Tiempo estimado: 5-7 minutos", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
            }
        },
        confirmButton = {
            Button(onClick = onStart, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.ArrowForward, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Comenzar Ahora")
            }
        },
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
    )
}