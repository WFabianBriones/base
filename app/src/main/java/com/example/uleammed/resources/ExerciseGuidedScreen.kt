package com.example.uleammed.resources

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Pantalla de ejercicio guiado con timer, contador y efectos visuales
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseGuidedScreen(
    exerciseId: String,
    onBack: () -> Unit,
    onComplete: () -> Unit,
    viewModel: ResourceViewModel = viewModel()
) {
    var exercise by remember { mutableStateOf<ExerciseResource?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    // Estado del ejercicio
    var currentSet by remember { mutableStateOf(1) }
    var currentRep by remember { mutableStateOf(1) }
    var currentStep by remember { mutableStateOf(0) }
    var isExercising by remember { mutableStateOf(false) }
    var isPaused by remember { mutableStateOf(false) }
    var timeRemaining by remember { mutableStateOf(0) }
    var showCompletionDialog by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    // Cargar ejercicio
    LaunchedEffect(exerciseId) {
        isLoading = true
        val exercises = viewModel.exercises.value
        exercise = exercises.find { it.id == exerciseId }
        if (exercise != null) {
            timeRemaining = exercise!!.duration
        }
        isLoading = false
    }

    // Timer
    LaunchedEffect(isExercising, isPaused, timeRemaining) {
        if (isExercising && !isPaused && timeRemaining > 0) {
            delay(1000L)
            timeRemaining--

            // Completar repetición
            if (timeRemaining == 0 && exercise != null) {
                if (currentRep < exercise!!.repetitions) {
                    // Siguiente repetición
                    currentRep++
                    timeRemaining = exercise!!.duration
                } else if (currentSet < exercise!!.sets) {
                    // Siguiente set
                    currentSet++
                    currentRep = 1
                    timeRemaining = exercise!!.duration
                } else {
                    // Ejercicio completado
                    isExercising = false
                    showCompletionDialog = true
                }
            }
        }
    }

    if (showCompletionDialog) {
        AlertDialog(
            onDismissRequest = { },
            icon = {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            title = { Text("¡Ejercicio Completado!") },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Has completado todos los sets y repeticiones.",
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "✓ ${exercise?.sets} sets",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "✓ ${exercise?.repetitions} repeticiones",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    showCompletionDialog = false
                    onComplete()
                    onBack()
                }) {
                    Text("Finalizar")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    // Reiniciar
                    showCompletionDialog = false
                    currentSet = 1
                    currentRep = 1
                    currentStep = 0
                    timeRemaining = exercise?.duration ?: 0
                }) {
                    Text("Hacer otro")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ejercicio Guiado") },
                navigationIcon = {
                    IconButton(onClick = {
                        if (isExercising) {
                            isPaused = true
                        } else {
                            onBack()
                        }
                    }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (exercise == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.ErrorOutline,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp)
                    )
                    Text("Ejercicio no encontrado")
                    Button(onClick = onBack) {
                        Text("Volver")
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Progreso general
                ExerciseProgress(
                    currentSet = currentSet,
                    totalSets = exercise!!.sets,
                    currentRep = currentRep,
                    totalReps = exercise!!.repetitions
                )

                // Área principal
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    // Nombre del ejercicio
                    Text(
                        text = exercise!!.name,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )

                    CategoryBadge(category = exercise!!.category)

                    // Timer circular
                    CircularTimer(
                        timeRemaining = timeRemaining,
                        totalTime = exercise!!.duration,
                        isRunning = isExercising && !isPaused
                    )

                    // Paso actual
                    if (currentStep < exercise!!.instructions.size) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "Paso ${currentStep + 1}/${exercise!!.instructions.size}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = exercise!!.instructions[currentStep],
                                    style = MaterialTheme.typography.bodyLarge,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    // Beneficios
                    if (!isExercising) {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.CheckCircle,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "Beneficios",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                exercise!!.benefits.forEach { benefit ->
                                    Text(
                                        text = "• $benefit",
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.padding(start = 8.dp, top = 4.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Advertencias
                    if (exercise!!.warnings.isNotEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                            )
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Warning,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                    Text(
                                        text = "Precauciones",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                exercise!!.warnings.forEach { warning ->
                                    Text(
                                        text = "⚠ $warning",
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.padding(start = 8.dp, top = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Controles
                ExerciseControls(
                    isExercising = isExercising,
                    isPaused = isPaused,
                    onStart = {
                        isExercising = true
                        isPaused = false
                    },
                    onPause = {
                        isPaused = true
                    },
                    onResume = {
                        isPaused = false
                    },
                    onStop = {
                        isExercising = false
                        isPaused = false
                        currentSet = 1
                        currentRep = 1
                        currentStep = 0
                        timeRemaining = exercise!!.duration
                    },
                    onNextStep = {
                        if (currentStep < exercise!!.instructions.size - 1) {
                            currentStep++
                        }
                    },
                    onPrevStep = {
                        if (currentStep > 0) {
                            currentStep--
                        }
                    },
                    hasNextStep = currentStep < exercise!!.instructions.size - 1,
                    hasPrevStep = currentStep > 0,
                    modifier = Modifier.padding(24.dp)
                )
            }
        }
    }
}

/**
 * Barra de progreso del ejercicio
 */
@Composable
fun ExerciseProgress(
    currentSet: Int,
    totalSets: Int,
    currentRep: Int,
    totalReps: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // Sets
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Set",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "$currentSet / $totalSets",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            VerticalDivider(modifier = Modifier.height(40.dp))

            // Repeticiones
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Repetición",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "$currentRep / $totalReps",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * Timer circular animado
 */
@Composable
fun CircularTimer(
    timeRemaining: Int,
    totalTime: Int,
    isRunning: Boolean
) {
    val progress = if (totalTime > 0) timeRemaining.toFloat() / totalTime else 0f

    // Animación de pulso cuando está corriendo
    val scale by animateFloatAsState(
        targetValue = if (isRunning) 1.05f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(200.dp)
    ) {
        // Círculo de progreso
        CircularProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxSize()
                .scale(if (isRunning) scale else 1f),
            strokeWidth = 12.dp,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
        )

        // Tiempo restante
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = timeRemaining.toString(),
                fontSize = 56.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "segundos",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Controles del ejercicio
 */
@Composable
fun ExerciseControls(
    isExercising: Boolean,
    isPaused: Boolean,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit,
    onNextStep: () -> Unit,
    onPrevStep: () -> Unit,
    hasNextStep: Boolean,
    hasPrevStep: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Controles principales
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (!isExercising) {
                // Botón Iniciar
                Button(
                    onClick = onStart,
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Iniciar", style = MaterialTheme.typography.titleMedium)
                }
            } else {
                if (isPaused) {
                    // Botón Reanudar
                    Button(
                        onClick = onResume,
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Reanudar", style = MaterialTheme.typography.titleMedium)
                    }
                } else {
                    // Botón Pausar
                    FilledTonalButton(
                        onClick = onPause,
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Pause,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Pausar", style = MaterialTheme.typography.titleMedium)
                    }
                }

                // Botón Detener
                OutlinedButton(
                    onClick = onStop,
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(
                        imageVector = Icons.Filled.Stop,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Detener", style = MaterialTheme.typography.titleMedium)
                }
            }
        }

        // Navegación de pasos
        if (!isExercising || isPaused) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onPrevStep,
                    enabled = hasPrevStep,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Paso Anterior")
                }

                OutlinedButton(
                    onClick = onNextStep,
                    enabled = hasNextStep,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Siguiente Paso")
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(Icons.Filled.ArrowForward, contentDescription = null)
                }
            }
        }
    }
}