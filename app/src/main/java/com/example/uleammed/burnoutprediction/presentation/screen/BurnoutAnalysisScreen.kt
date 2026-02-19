package com.example.uleammed.burnoutprediction.presentation.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.AnimatedContent
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.uleammed.burnoutprediction.model.BurnoutPrediction
import com.example.uleammed.burnoutprediction.model.NivelRiesgoBurnout
import com.example.uleammed.burnoutprediction.presentation.viewmodel.BurnoutAnalysisViewModel
import com.example.uleammed.burnoutprediction.presentation.viewmodel.BurnoutUiState
import com.example.uleammed.tendencias.HealthIndex
import com.example.uleammed.tendencias.TendenciasSection
import com.example.uleammed.tendencias.TendenciasUiState
import com.example.uleammed.tendencias.TendenciasViewModel
import com.example.uleammed.tendencias.TendenciasViewModelFactory
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BurnoutAnalysisScreen(
    viewModel: BurnoutAnalysisViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // ── ViewModel de tendencias (Factory manual, sin Hilt) ────────────────
    val tendenciasViewModel: TendenciasViewModel = viewModel(
        factory = TendenciasViewModelFactory()
    )
    val tendenciasState by tendenciasViewModel.uiState.collectAsStateWithLifecycle()
    // ─────────────────────────────────────────────────────────────────────

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
                is BurnoutUiState.Idle    -> IdleView()
                is BurnoutUiState.Loading -> LoadingView()
                is BurnoutUiState.Error   -> ErrorView(message = state.message)
                is BurnoutUiState.Success -> ResultView(
                    prediction      = state.prediction,
                    tendenciasState = tendenciasState,
                    onToggle        = tendenciasViewModel::toggleIndex,
                    onRetry         = tendenciasViewModel::loadTendencias
                )
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════
//  VISTA DE RESULTADOS — ProbabilityChart reemplazado por TendenciasSection
// ═════════════════════════════════════════════════════════════════════════
@Composable
private fun ResultView(
    prediction: BurnoutPrediction,
    tendenciasState: TendenciasUiState,
    onToggle: (HealthIndex) -> Unit,
    onRetry: () -> Unit
) {
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
            enter   = fadeIn() + slideInVertically()
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {

                // ── 1. Card nivel de riesgo ──────────────────────────────
                RiskLevelCard(prediction)

                // ── 2. Card confianza del modelo ─────────────────────────
                ConfidenceCard(prediction)

                // ── 3. Tendencias (reemplaza ProbabilityChart) ───────────
                TendenciasSection(
                    uiState  = tendenciasState,
                    onToggle = onToggle,
                    onRetry  = onRetry
                )

                // ── 4. Recomendaciones ───────────────────────────────────
                RecommendationsCard(prediction)

                // ── 5. Aviso informativo ─────────────────────────────────
                InfoCard()
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════
//  VISTA DE ESPERA
// ═════════════════════════════════════════════════════════════════════════
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
            text  = "Esperando análisis...",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text      = "El análisis se iniciará automáticamente",
            style     = MaterialTheme.typography.bodyMedium,
            color     = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

// ═════════════════════════════════════════════════════════════════════════
//  VISTA DE CARGA
// ═════════════════════════════════════════════════════════════════════════
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
        val infiniteTransition = rememberInfiniteTransition(label = "pulse")
        val scale by infiniteTransition.animateFloat(
            initialValue = 0.8f,
            targetValue  = 1.2f,
            animationSpec = infiniteRepeatable(
                animation  = tween(1000),
                repeatMode = RepeatMode.Reverse
            ),
            label = "scale"
        )

        Box(
            modifier         = Modifier.size(150.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(120.dp * scale)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
            )
            CircularProgressIndicator(
                modifier    = Modifier.size(80.dp),
                strokeWidth = 6.dp,
                color       = MaterialTheme.colorScheme.primary
            )
            Icon(
                imageVector = Icons.Filled.Psychology,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint     = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        AnimatedVisibility(visible = true, enter = fadeIn() + slideInVertically()) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text       = steps[currentStep],
                    style      = MaterialTheme.typography.titleMedium,
                    color      = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
            colors   = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            )
        ) {
            Row(
                modifier          = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text  = "Nuestro modelo de IA está evaluando tus respuestas para proporcionarte un análisis personalizado",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════
//  CARD DE NIVEL DE RIESGO
// ═════════════════════════════════════════════════════════════════════════
@Composable
private fun RiskLevelCard(prediction: BurnoutPrediction) {
    val (backgroundColor, iconColor, icon) = when (prediction.nivelRiesgo) {
        NivelRiesgoBurnout.BAJO  -> Triple(
            Color(0xFF4CAF50).copy(alpha = 0.15f), Color(0xFF2E7D32), Icons.Filled.CheckCircle
        )
        NivelRiesgoBurnout.MEDIO -> Triple(
            Color(0xFFFFA726).copy(alpha = 0.15f), Color(0xFFE65100), Icons.Filled.Warning
        )
        NivelRiesgoBurnout.ALTO  -> Triple(
            Color(0xFFEF5350).copy(alpha = 0.15f), Color(0xFFC62828), Icons.Filled.Error
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors   = CardDefaults.cardColors(containerColor = backgroundColor),
        shape    = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier            = Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint     = iconColor
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text  = "Nivel de Riesgo",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            Text(
                text       = prediction.nivelRiesgo.displayName,
                style      = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color      = iconColor
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text      = getRiskDescription(prediction.nivelRiesgo),
                style     = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color     = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════
//  CARD DE CONFIANZA
// ═════════════════════════════════════════════════════════════════════════
@Composable
private fun ConfidenceCard(prediction: BurnoutPrediction) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors   = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Analytics,
                    contentDescription = null,
                    tint     = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text       = "Confianza del Modelo de IA",
                    style      = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color      = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Spacer(modifier = Modifier.height(16.dp))

            val animatedProgress by animateFloatAsState(
                targetValue  = prediction.confianza,
                animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
                label        = "confidence"
            )
            LinearProgressIndicator(
                progress     = { animatedProgress },
                modifier     = Modifier.fillMaxWidth().height(12.dp).clip(RoundedCornerShape(6.dp)),
                color        = MaterialTheme.colorScheme.primary,
                trackColor   = MaterialTheme.colorScheme.surfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text       = "${(prediction.confianza * 100).toInt()}% de confianza",
                style      = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color      = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text  = "Basado en el análisis de tus respuestas usando redes neuronales",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
            )
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════
//  CARD DE RECOMENDACIONES
// ═════════════════════════════════════════════════════════════════════════
@Composable
private fun RecommendationsCard(prediction: BurnoutPrediction) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors   = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Lightbulb,
                    contentDescription = null,
                    tint     = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text       = "Recomendaciones",
                    style      = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color      = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            getRecommendations(prediction.nivelRiesgo).forEach { recommendation ->
                RecommendationItem(recommendation)
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun RecommendationItem(text: String) {
    Row(
        modifier          = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = Icons.Filled.CheckCircle,
            contentDescription = null,
            tint     = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text  = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}

// ═════════════════════════════════════════════════════════════════════════
//  CARD INFORMATIVA
// ═════════════════════════════════════════════════════════════════════════
@Composable
private fun InfoCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors   = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        )
    ) {
        Row(
            modifier          = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onTertiaryContainer
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text  = "Este análisis es orientativo. Si experimentas síntomas persistentes, consulta con un profesional de salud ocupacional.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════
//  VISTA DE ERROR
// ═════════════════════════════════════════════════════════════════════════
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
            tint     = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text       = "Error en el Análisis",
            style      = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color      = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(8.dp))
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer
            )
        ) {
            Text(
                text      = message,
                style     = MaterialTheme.typography.bodyMedium,
                color     = MaterialTheme.colorScheme.onErrorContainer,
                textAlign = TextAlign.Center,
                modifier  = Modifier.padding(16.dp)
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text  = "Por favor, intenta nuevamente más tarde",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    }
}

// ═════════════════════════════════════════════════════════════════════════
//  FUNCIONES AUXILIARES
// ═════════════════════════════════════════════════════════════════════════
private fun getRiskDescription(level: NivelRiesgoBurnout): String = when (level) {
    NivelRiesgoBurnout.BAJO  -> "Tu nivel de burnout es bajo. Continúa manteniendo buenos hábitos de trabajo y autocuidado."
    NivelRiesgoBurnout.MEDIO -> "Presentas señales moderadas de burnout. Es momento de tomar medidas preventivas."
    NivelRiesgoBurnout.ALTO  -> "Tu nivel de burnout es alto. Se recomienda buscar apoyo profesional y hacer cambios inmediatos."
}

private fun getRecommendations(level: NivelRiesgoBurnout): List<String> = when (level) {
    NivelRiesgoBurnout.BAJO  -> listOf(
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
    NivelRiesgoBurnout.ALTO  -> listOf(
        "Consulta con un profesional de salud ocupacional",
        "Evalúa la posibilidad de tomar tiempo de descanso",
        "Habla con tu supervisor sobre tu carga laboral",
        "Considera apoyo psicológico profesional",
        "Revisa urgentemente tus hábitos de sueño y alimentación",
        "Implementa cambios inmediatos en tu entorno de trabajo"
    )
}
