package com.example.uleammed.burnoutprediction.presentation.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.uleammed.burnoutprediction.presentation.viewmodel.*
import com.example.uleammed.burnoutprediction.model.BurnoutPrediction
import com.example.uleammed.burnoutprediction.model.NivelRiesgoBurnout
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BurnoutAnalysisScreen(
    viewModel: BurnoutAnalysisViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Análisis de Burnout con IA",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (val state = uiState) {
                is BurnoutUiState.Idle -> {
                    IdleView()
                }

                is BurnoutUiState.Loading -> {
                    LoadingView()
                }

                is BurnoutUiState.Success -> {
                    ResultView(prediction = state.prediction)
                }

                is BurnoutUiState.Error -> {
                    ErrorView(message = state.message)
                }
            }
        }
    }
}

// ========== VISTA DE ESPERA ==========
@Composable
private fun IdleView() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Filled.Psychology,
            contentDescription = null,
            modifier = Modifier.size(120.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Esperando análisis...",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "El análisis se iniciará automáticamente",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

// ========== VISTA DE CARGA ==========
@Composable
private fun LoadingView() {
    var currentStep by remember { mutableStateOf(0) }
    val steps = listOf(
        "Procesando respuestas...",
        "Analizando patrones con IA...",
        "Calculando nivel de riesgo...",
        "Generando recomendaciones..."
    )

    LaunchedEffect(Unit) {
        while (currentStep < steps.size - 1) {
            delay(1500)
            currentStep++
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Animación de círculo pulsante
        val infiniteTransition = rememberInfiniteTransition(label = "pulse")
        val scale by infiniteTransition.animateFloat(
            initialValue = 0.8f,
            targetValue = 1.2f,
            animationSpec = infiniteRepeatable(
                animation = tween(1000),
                repeatMode = RepeatMode.Reverse
            ),
            label = "scale"
        )

        Box(
            modifier = Modifier.size(150.dp),
            contentAlignment = Alignment.Center
        ) {
            // Círculo de fondo pulsante
            Box(
                modifier = Modifier
                    .size(120.dp * scale)
                    .clip(CircleShape)
                    .background(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                    )
            )

            // Indicador circular
            CircularProgressIndicator(
                modifier = Modifier.size(80.dp),
                strokeWidth = 6.dp,
                color = MaterialTheme.colorScheme.primary
            )

            // Icono central
            Icon(
                imageVector = Icons.Filled.Psychology,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Texto animado de pasos
        AnimatedVisibility(
            visible = true,
            enter = fadeIn() + slideInVertically()
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = steps[currentStep],
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Indicador de progreso
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    steps.forEachIndexed { index, _ ->
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(
                                    if (index <= currentStep)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.surfaceVariant
                                )
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            )
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Nuestro modelo de IA está evaluando tus respuestas para proporcionarte un análisis personalizado",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
    }
}

// ========== VISTA DE RESULTADOS ==========
@Composable
private fun ResultView(prediction: BurnoutPrediction) {
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(300)
        isVisible = true
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        AnimatedVisibility(
            visible = isVisible,
            enter = fadeIn() + slideInVertically()
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Card de nivel de riesgo principal
                RiskLevelCard(prediction)

                // Card de confianza del modelo
                ConfidenceCard(prediction)

                // Gráfico de probabilidades
                ProbabilityChart(prediction)

                // Recomendaciones
                RecommendationsCard(prediction)

                // Información adicional
                InfoCard()
            }
        }
    }
}

// ========== CARD DE NIVEL DE RIESGO ==========
@Composable
private fun RiskLevelCard(prediction: BurnoutPrediction) {
    val (backgroundColor, iconColor, icon) = when (prediction.nivelRiesgo) {
        NivelRiesgoBurnout.BAJO -> Triple(
            Color(0xFF4CAF50).copy(alpha = 0.15f),
            Color(0xFF2E7D32),
            Icons.Filled.CheckCircle
        )
        NivelRiesgoBurnout.MEDIO -> Triple(
            Color(0xFFFFA726).copy(alpha = 0.15f),
            Color(0xFFE65100),
            Icons.Filled.Warning
        )
        NivelRiesgoBurnout.ALTO -> Triple(
            Color(0xFFEF5350).copy(alpha = 0.15f),
            Color(0xFFC62828),
            Icons.Filled.Error
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = iconColor
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Nivel de Riesgo",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )

            Text(
                text = prediction.nivelRiesgo.displayName,
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = iconColor
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = getRiskDescription(prediction.nivelRiesgo),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

// ========== CARD DE CONFIANZA ==========
@Composable
private fun ConfidenceCard(prediction: BurnoutPrediction) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Analytics,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Confianza del Modelo de IA",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Barra de progreso animada
            val animatedProgress by animateFloatAsState(
                targetValue = prediction.confianza,
                animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
                label = "confidence"
            )

            Column {
                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(12.dp)
                        .clip(RoundedCornerShape(6.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "${(prediction.confianza * 100).toInt()}% de confianza",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Basado en el análisis de tus respuestas usando redes neuronales",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
            )
        }
    }
}

// ========== GRÁFICO CIRCULAR DE PROBABILIDADES ==========
@Composable
private fun ProbabilityChart(prediction: BurnoutPrediction) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.PieChart,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Distribución de Probabilidades",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Gráfico circular
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Gráfico de dona
                CircularChart(
                    bajo = prediction.probabilidadBajo,
                    medio = prediction.probabilidadMedio,
                    alto = prediction.probabilidadAlto
                )

                // Leyenda
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    LegendItem(
                        color = Color(0xFF4CAF50),
                        label = "Bajo",
                        percentage = prediction.probabilidadBajo
                    )
                    LegendItem(
                        color = Color(0xFFFFA726),
                        label = "Medio",
                        percentage = prediction.probabilidadMedio
                    )
                    LegendItem(
                        color = Color(0xFFEF5350),
                        label = "Alto",
                        percentage = prediction.probabilidadAlto
                    )
                }
            }
        }
    }
}

@Composable
private fun CircularChart(bajo: Float, medio: Float, alto: Float) {
    val animatedBajo by animateFloatAsState(
        targetValue = bajo,
        animationSpec = tween(1000),
        label = "bajo"
    )
    val animatedMedio by animateFloatAsState(
        targetValue = medio,
        animationSpec = tween(1000, delayMillis = 200),
        label = "medio"
    )
    val animatedAlto by animateFloatAsState(
        targetValue = alto,
        animationSpec = tween(1000, delayMillis = 400),
        label = "alto"
    )

    Canvas(
        modifier = Modifier.size(140.dp)
    ) {
        val strokeWidth = 30f
        val radius = (size.minDimension - strokeWidth) / 2f
        val center = Offset(size.width / 2f, size.height / 2f)

        var currentAngle = -90f

        // Segmento Bajo (verde)
        val sweepAngleBajo = animatedBajo * 360f
        if (sweepAngleBajo > 0) {
            drawArc(
                color = Color(0xFF4CAF50),
                startAngle = currentAngle,
                sweepAngle = sweepAngleBajo,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2, radius * 2)
            )
            currentAngle += sweepAngleBajo
        }

        // Segmento Medio (naranja)
        val sweepAngleMedio = animatedMedio * 360f
        if (sweepAngleMedio > 0) {
            drawArc(
                color = Color(0xFFFFA726),
                startAngle = currentAngle,
                sweepAngle = sweepAngleMedio,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2, radius * 2)
            )
            currentAngle += sweepAngleMedio
        }

        // Segmento Alto (rojo)
        val sweepAngleAlto = animatedAlto * 360f
        if (sweepAngleAlto > 0) {
            drawArc(
                color = Color(0xFFEF5350),
                startAngle = currentAngle,
                sweepAngle = sweepAngleAlto,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2, radius * 2)
            )
        }
    }
}

@Composable
private fun LegendItem(color: Color, label: String, percentage: Float) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(16.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = "$label:",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = "${(percentage * 100).toInt()}%",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

// ========== CARD DE RECOMENDACIONES ==========
@Composable
private fun RecommendationsCard(prediction: BurnoutPrediction) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Lightbulb,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Recomendaciones",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            val recommendations = getRecommendations(prediction.nivelRiesgo)
            recommendations.forEach { recommendation ->
                RecommendationItem(recommendation)
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun RecommendationItem(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = Icons.Filled.CheckCircle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}

// ========== CARD DE INFORMACIÓN ==========
@Composable
private fun InfoCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onTertiaryContainer
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Este análisis es orientativo. Si experimentas síntomas persistentes, consulta con un profesional de salud ocupacional.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )
        }
    }
}

// ========== VISTA DE ERROR ==========
@Composable
private fun ErrorView(message: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Filled.ErrorOutline,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.error
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Error en el Análisis",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.error
        )

        Spacer(modifier = Modifier.height(8.dp))

        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer
            )
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(16.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Por favor, intenta nuevamente más tarde",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    }
}

// ========== FUNCIONES AUXILIARES ==========
private fun getRiskDescription(level: NivelRiesgoBurnout): String {
    return when (level) {
        NivelRiesgoBurnout.BAJO -> "Tu nivel de burnout es bajo. Continúa manteniendo buenos hábitos de trabajo y autocuidado."
        NivelRiesgoBurnout.MEDIO -> "Presentas señales moderadas de burnout. Es momento de tomar medidas preventivas."
        NivelRiesgoBurnout.ALTO -> "Tu nivel de burnout es alto. Se recomienda buscar apoyo profesional y hacer cambios inmediatos."
    }
}

private fun getRecommendations(level: NivelRiesgoBurnout): List<String> {
    return when (level) {
        NivelRiesgoBurnout.BAJO -> listOf(
            "Mantén una rutina de descanso adecuada",
            "Continúa con actividad física regular",
            "Realiza pausas activas durante tu jornada laboral",
            "Practica técnicas de relajación"
        )
        NivelRiesgoBurnout.MEDIO -> listOf(
            "Establece límites claros entre trabajo y vida personal",
            "Organiza mejor tu carga de trabajo y prioridades",
            "Considera técnicas de manejo del estrés",
            "Busca apoyo de colegas o supervisores",
            "Evalúa tu ergonomía laboral"
        )
        NivelRiesgoBurnout.ALTO -> listOf(
            "Consulta con un profesional de salud ocupacional",
            "Evalúa la posibilidad de tomar tiempo de descanso",
            "Habla con tu supervisor sobre tu carga laboral",
            "Considera apoyo psicológico profesional",
            "Revisa urgentemente tus hábitos de sueño y alimentación",
            "Implementa cambios inmediatos en tu entorno de trabajo"
        )
    }
}