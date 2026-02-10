package com.example.uleammed.burnoutprediction.presentation.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.uleammed.burnoutprediction.presentation.viewmodel.BurnoutAnalysisViewModel
import com.example.uleammed.burnoutprediction.presentation.viewmodel.BurnoutUiState
import com.example.uleammed.burnoutprediction.model.*
import com.example.uleammed.scoring.CriticalLevel

/**
 * ✅ PANTALLA PRINCIPAL DE ANÁLISIS DE BURNOUT
 * Compatible con MainActivity - Recibe ViewModel y maneja estados
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BurnoutAnalysisScreen(
    viewModel: BurnoutAnalysisViewModel,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Análisis de Riesgo de Burnout") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val state = uiState) {
                is BurnoutUiState.Idle -> IdleView()
                is BurnoutUiState.Loading -> LoadingView()
                is BurnoutUiState.Success -> SimpleResultView(state.prediction)
                is BurnoutUiState.EnhancedSuccess -> EnhancedResultView(state.prediction)
                is BurnoutUiState.Error -> ErrorView(state.message)
            }
        }
    }
}

// ==================== VISTAS DE ESTADO ====================

@Composable
private fun IdleView() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                Icons.Filled.Psychology,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text("Inicia un análisis desde el dashboard")
        }
    }
}

@Composable
private fun LoadingView() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator()
            Text("Analizando datos con IA...")
        }
    }
}

@Composable
private fun ErrorView(message: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    Icons.Filled.Error,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.error
                )
                Text(
                    text = "Error",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.error
                )
                Text(text = message, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun SimpleResultView(prediction: BurnoutPrediction) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { Spacer(modifier = Modifier.height(8.dp)) }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color(prediction.nivelRiesgo.color).copy(alpha = 0.1f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Nivel de Riesgo", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = prediction.nivelRiesgo.displayName,
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(prediction.nivelRiesgo.color)
                    )
                    Text("Confianza: ${(prediction.confianza * 100).toInt()}%")
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "Probabilidades",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    SimpleProbability("Bajo", prediction.probabilidadBajo)
                    SimpleProbability("Medio", prediction.probabilidadMedio)
                    SimpleProbability("Alto", prediction.probabilidadAlto)
                }
            }
        }

        item { Spacer(modifier = Modifier.height(32.dp)) }
    }
}

@Composable
private fun SimpleProbability(label: String, probability: Float) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label)
            Text("${(probability * 100).toInt()}%", fontWeight = FontWeight.Bold)
        }
        LinearProgressIndicator(progress = probability, modifier = Modifier.fillMaxWidth())
    }
}

// ==================== VISTA MEJORADA ====================

@Composable
private fun EnhancedResultView(prediction: EnhancedBurnoutPrediction) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { Spacer(modifier = Modifier.height(8.dp)) }

        if (prediction.requiresUrgentAttention) {
            item {
                UrgentAttentionCard(
                    patterns = prediction.criticalPatterns.filter {
                        it.severity == CriticalLevel.INTERVENCION_URGENTE
                    }
                )
            }
        }

        item {
            RiskLevelCard(
                nivelRiesgo = prediction.nivelRiesgo,
                confianza = prediction.confianza,
                hasCriticalPatterns = prediction.hasCriticalPatterns
            )
        }

        item {
            ProbabilitiesCard(
                probabilidadBajo = prediction.probabilidadBajo,
                probabilidadMedio = prediction.probabilidadMedio,
                probabilidadAlto = prediction.probabilidadAlto
            )
        }

        if (prediction.criticalPatterns.isNotEmpty()) {
            item { CriticalPatternsCard(patterns = prediction.criticalPatterns) }
        }

        if (prediction.factoresRiesgo.isNotEmpty()) {
            item { RiskFactorsCard(factors = prediction.factoresRiesgo) }
        }

        item {
            RecommendationsCard(
                recommendations = prediction.recomendaciones,
                hasCriticalPatterns = prediction.hasCriticalPatterns
            )
        }

        item { Spacer(modifier = Modifier.height(32.dp)) }
    }
}

// ==================== COMPONENTES UI ====================

@Composable
private fun UrgentAttentionCard(patterns: List<com.example.uleammed.scoring.CriticalPattern>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Filled.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(32.dp)
                )
                Text(
                    text = "⚠️ ATENCIÓN URGENTE REQUERIDA",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
            }

            patterns.forEach { pattern ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.1f),
                    shape = MaterialTheme.shapes.small
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = pattern.area,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = pattern.description)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Filled.LocalHospital,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = pattern.recommendation,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RiskLevelCard(
    nivelRiesgo: BurnoutRiskLevel,
    confianza: Float,
    hasCriticalPatterns: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(nivelRiesgo.color).copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Nivel de Riesgo",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = nivelRiesgo.displayName.uppercase(),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = Color(nivelRiesgo.color)
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    if (hasCriticalPatterns) Icons.Filled.CheckCircle else Icons.Filled.Info,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Text("Confianza: ${(confianza * 100).toInt()}%")
                if (hasCriticalPatterns) {
                    Text(
                        text = " (+ evidencia)",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun ProbabilitiesCard(
    probabilidadBajo: Float,
    probabilidadMedio: Float,
    probabilidadAlto: Float
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Filled.Psychology, contentDescription = null)
                Text(
                    "Análisis de IA",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            ProbabilityBar("Riesgo Bajo", probabilidadBajo, BurnoutRiskLevel.BAJO.color)
            ProbabilityBar("Riesgo Medio", probabilidadMedio, BurnoutRiskLevel.MEDIO.color)
            ProbabilityBar("Riesgo Alto", probabilidadAlto, BurnoutRiskLevel.ALTO.color)
        }
    }
}

@Composable
private fun ProbabilityBar(label: String, probability: Float, color: Long) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label)
            Text("${(probability * 100).toInt()}%", fontWeight = FontWeight.Bold)
        }
        LinearProgressIndicator(
            progress = probability,
            modifier = Modifier.fillMaxWidth().height(8.dp),
            color = Color(color),
            trackColor = Color(color).copy(alpha = 0.2f)
        )
    }
}

@Composable
private fun CriticalPatternsCard(patterns: List<com.example.uleammed.scoring.CriticalPattern>) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Filled.Assessment, contentDescription = null)
                Text(
                    "Patrones Críticos Detectados",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            patterns.forEach { pattern -> PatternItem(pattern) }
        }
    }
}

@Composable
private fun PatternItem(pattern: com.example.uleammed.scoring.CriticalPattern) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = when (pattern.severity) {
            CriticalLevel.INTERVENCION_URGENTE -> MaterialTheme.colorScheme.errorContainer
            CriticalLevel.ATENCION_REQUERIDA -> MaterialTheme.colorScheme.tertiaryContainer
            CriticalLevel.ALERTA_TEMPRANA -> MaterialTheme.colorScheme.secondaryContainer
        },
        shape = MaterialTheme.shapes.small
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                when (pattern.severity) {
                    CriticalLevel.INTERVENCION_URGENTE -> Icons.Filled.ReportProblem
                    CriticalLevel.ATENCION_REQUERIDA -> Icons.Filled.Warning
                    CriticalLevel.ALERTA_TEMPRANA -> Icons.Filled.Info
                },
                contentDescription = null
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    pattern.area,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(pattern.description, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun RiskFactorsCard(factors: List<RiskFactor>) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Filled.TrendingUp, contentDescription = null)
                Text(
                    "Factores de Riesgo",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            factors.forEach { factor -> RiskFactorItem(factor) }
        }
    }
}

@Composable
private fun RiskFactorItem(factor: RiskFactor) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(factor.area, fontWeight = FontWeight.Bold)
            Text(
                "Score: ${factor.score}/100",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        SeverityChip(severity = factor.severity)
    }
}

@Composable
private fun SeverityChip(severity: RiskSeverity) {
    val (color, label) = when (severity) {
        RiskSeverity.MUY_ALTO -> Color.Red to "MUY ALTO"
        RiskSeverity.ALTO -> Color(0xFFFF9800) to "ALTO"
        RiskSeverity.MODERADO -> Color(0xFFFFC107) to "MODERADO"
        RiskSeverity.BAJO -> Color.Green to "BAJO"
    }
    Surface(color = color.copy(alpha = 0.2f), shape = MaterialTheme.shapes.small) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@Composable
private fun RecommendationsCard(
    recommendations: List<String>,
    hasCriticalPatterns: Boolean
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Filled.Lightbulb, contentDescription = null)
                Text(
                    "Recomendaciones",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            if (hasCriticalPatterns) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = MaterialTheme.shapes.small
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Filled.Info,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            "Priorizadas según patrones",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            recommendations.forEachIndexed { index, rec ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        "${index + 1}.",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(rec, modifier = Modifier.weight(1f))
                }
                if (index < recommendations.size - 1) {
                    Divider(modifier = Modifier.padding(vertical = 4.dp))
                }
            }
        }
    }
}