package com.example.uleammed.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

/**
 * Dashboard principal para administradores
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    viewModel: AdminDashboardViewModel = viewModel(),
    onNavigateToUserManagement: () -> Unit = {},
    onNavigateToCreateAdmin: () -> Unit = {},
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val userRole by viewModel.userRole.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Panel de Administración") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Volver")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.loadDashboardData() }) {
                        Icon(Icons.Default.Refresh, "Actualizar")
                    }
                }
            )
        }
    ) { padding ->
        when (val state = uiState) {
            is AdminDashboardState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            is AdminDashboardState.Success -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Estadísticas generales
                    item {
                        StatisticsOverviewCard(statistics = state.statistics)
                    }

                    // Distribución de riesgo
                    item {
                        RiskDistributionCard(riskDistribution = state.riskDistribution)
                    }

                    // Estadísticas de cuestionarios
                    item {
                        QuestionnaireStatsCard(stats = state.questionnaireStats)
                    }

                    // Acciones administrativas (solo para super usuario)
                    if (userRole == UserRole.SUPERUSER) {
                        item {
                            AdminActionsCard(
                                onNavigateToUserManagement = onNavigateToUserManagement,
                                onNavigateToCreateAdmin = onNavigateToCreateAdmin
                            )
                        }
                    }
                }
            }

            is AdminDashboardState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.ErrorOutline,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = state.message,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Button(onClick = { viewModel.loadDashboardData() }) {
                            Text("Reintentar")
                        }
                    }
                }
            }

            else -> {}
        }
    }
}

/**
 * Card con estadísticas generales
 */
@Composable
private fun StatisticsOverviewCard(statistics: AppStatistics) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Estadísticas Generales",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(
                    icon = Icons.Default.People,
                    label = "Usuarios",
                    value = statistics.totalUsers.toString(),
                    color = MaterialTheme.colorScheme.primary
                )

                StatItem(
                    icon = Icons.Default.PersonAdd,
                    label = "Activos",
                    value = statistics.activeUsers.toString(),
                    color = MaterialTheme.colorScheme.secondary
                )

                StatItem(
                    icon = Icons.Default.Assignment,
                    label = "Cuestionarios",
                    value = statistics.totalQuestionnaires.toString(),
                    color = MaterialTheme.colorScheme.tertiary
                )
            }

            Divider()

            // Tasa de activación
            val activationRate = if (statistics.totalUsers > 0) {
                (statistics.activeUsers.toFloat() / statistics.totalUsers * 100).toInt()
            } else 0

            LinearProgressIndicator(
                progress = { activationRate / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
            )
            Text(
                text = "Tasa de activación: $activationRate%",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Componente de estadística individual
 */
@Composable
private fun StatItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(32.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Card con distribución de niveles de riesgo
 */
@Composable
private fun RiskDistributionCard(riskDistribution: Map<String, Int>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Distribución de Riesgo",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            val total = riskDistribution.values.sum()

            riskDistribution.forEach { (risk, count) ->
                RiskBar(
                    riskLevel = risk,
                    count = count,
                    total = total
                )
            }
        }
    }
}

/**
 * Barra de distribución de riesgo
 */
@Composable
private fun RiskBar(
    riskLevel: String,
    count: Int,
    total: Int
) {
    val percentage = if (total > 0) count.toFloat() / total else 0f
    val color = when (riskLevel) {
        "LOW" -> Color(0xFF4CAF50)
        "MODERATE" -> Color(0xFFFFC107)
        "HIGH" -> Color(0xFFFF9800)
        "CRITICAL" -> Color(0xFFF44336)
        else -> Color.Gray
    }
    val label = when (riskLevel) {
        "LOW" -> "Bajo"
        "MODERATE" -> "Moderado"
        "HIGH" -> "Alto"
        "CRITICAL" -> "Crítico"
        else -> riskLevel
    }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "$count usuarios (${(percentage * 100).toInt()}%)",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        LinearProgressIndicator(
            progress = { percentage },
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
                .clip(RoundedCornerShape(6.dp)),
            color = color,
        )
    }
}

/**
 * Card con estadísticas de cuestionarios
 */
@Composable
private fun QuestionnaireStatsCard(stats: QuestionnaireStatistics) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Cuestionarios Completados",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            val questionnaireNames = mapOf(
                "salud_general" to "Salud General",
                "ergonomia" to "Ergonomía",
                "sintomas_musculares" to "Síntomas Musculares",
                "sintomas_visuales" to "Síntomas Visuales",
                "carga_trabajo" to "Carga de Trabajo",
                "estres_salud_mental" to "Estrés y Salud Mental",
                "habitos_sueno" to "Hábitos de Sueño",
                "actividad_fisica" to "Actividad Física",
                "balance_vida_trabajo" to "Balance Vida-Trabajo"
            )

            stats.completionByType.forEach { (type, count) ->
                val displayName = questionnaireNames[type] ?: type
                val rate = stats.getCompletionRate(type)

                QuestionnaireStatRow(
                    name = displayName,
                    count = count,
                    totalUsers = stats.totalUsers,
                    completionRate = rate
                )
            }
        }
    }
}

/**
 * Fila de estadística de cuestionario
 */
@Composable
private fun QuestionnaireStatRow(
    name: String,
    count: Int,
    totalUsers: Int,
    completionRate: Float
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = name,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "$count/$totalUsers (${(completionRate * 100).toInt()}%)",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        LinearProgressIndicator(
            progress = { completionRate },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
        )
    }
}

/**
 * Card con acciones administrativas
 */
@Composable
private fun AdminActionsCard(
    onNavigateToUserManagement: () -> Unit,
    onNavigateToCreateAdmin: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Acciones de Super Usuario",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Button(
                onClick = onNavigateToCreateAdmin,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.PersonAdd, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Crear Nuevo Administrador")
            }

            OutlinedButton(
                onClick = onNavigateToUserManagement,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.ManageAccounts, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Gestionar Usuarios")
            }
        }
    }
}
