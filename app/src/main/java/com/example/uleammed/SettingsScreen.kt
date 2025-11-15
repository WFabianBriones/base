package com.example.uleammed

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: NotificationViewModel = viewModel()
) {
    val scheduleConfig by viewModel.scheduleConfig.collectAsState()
    var selectedFrequency by remember {
        mutableStateOf(
            when (scheduleConfig?.periodDays) {
                7 -> QuestionnaireFrequency.WEEKLY
                15 -> QuestionnaireFrequency.BIWEEKLY
                30 -> QuestionnaireFrequency.MONTHLY
                else -> QuestionnaireFrequency.WEEKLY
            }
        )
    }
    var showConfirmDialog by remember { mutableStateOf(false) }
    var pendingFrequency by remember { mutableStateOf<QuestionnaireFrequency?>(null) }

    LaunchedEffect(scheduleConfig) {
        scheduleConfig?.let { config ->
            selectedFrequency = when (config.periodDays) {
                7 -> QuestionnaireFrequency.WEEKLY
                15 -> QuestionnaireFrequency.BIWEEKLY
                30 -> QuestionnaireFrequency.MONTHLY
                else -> QuestionnaireFrequency.WEEKLY
            }
        }
    }

    if (showConfirmDialog && pendingFrequency != null) {
        AlertDialog(
            onDismissRequest = {
                showConfirmDialog = false
                pendingFrequency = null
            },
            icon = { Icon(Icons.Filled.Schedule, contentDescription = null) },
            title = { Text("Cambiar periodicidad") },
            text = {
                Text("¿Deseas cambiar la periodicidad de los cuestionarios a ${pendingFrequency!!.displayName}? Esto regenerará las notificaciones pendientes.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.updatePeriodDays(pendingFrequency!!.days)
                        selectedFrequency = pendingFrequency!!
                        showConfirmDialog = false
                        pendingFrequency = null
                    }
                ) {
                    Text("Confirmar")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showConfirmDialog = false
                        pendingFrequency = null
                    }
                ) {
                    Text("Cancelar")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Configuración") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
        ) {
            // Sección de Notificaciones
            Text(
                text = "Notificaciones de Cuestionarios",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Configura cada cuánto tiempo deseas recibir recordatorios para completar los cuestionarios de salud.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
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
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Periodicidad de Cuestionarios",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Column(modifier = Modifier.selectableGroup()) {
                        QuestionnaireFrequency.values().forEach { frequency ->
                            FrequencyOption(
                                frequency = frequency,
                                selected = selectedFrequency == frequency,
                                onSelect = {
                                    if (selectedFrequency != frequency) {
                                        pendingFrequency = frequency
                                        showConfirmDialog = true
                                    }
                                }
                            )
                            if (frequency != QuestionnaireFrequency.values().last()) {
                                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Información adicional
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Filled.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "¿Por qué es importante?",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Completar los cuestionarios regularmente nos ayuda a monitorear tu salud laboral y detectar problemas tempranamente. Los recordatorios aparecerán en tu sección de Avisos.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Estado actual
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Configuración Actual",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Periodicidad:",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = selectedFrequency.displayName,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Cuestionarios activos:",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "${scheduleConfig?.enabledQuestionnaires?.size ?: 8}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FrequencyOption(
    frequency: QuestionnaireFrequency,
    selected: Boolean,
    onSelect: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                onClick = onSelect,
                role = Role.RadioButton
            )
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = null
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = frequency.displayName,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
            )
            Text(
                text = when (frequency) {
                    QuestionnaireFrequency.WEEKLY -> "Recibirás recordatorios cada semana"
                    QuestionnaireFrequency.BIWEEKLY -> "Recibirás recordatorios cada 15 días"
                    QuestionnaireFrequency.MONTHLY -> "Recibirás recordatorios cada mes"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (selected) {
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}