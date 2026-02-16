package com.example.uleammed.perfil

import android.util.Log
import android.widget.Toast
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.uleammed.BuildConfig
import com.example.uleammed.notifications.DebugNotificationHelper
import com.example.uleammed.notifications.NotificationPermissionHandler
import com.example.uleammed.notifications.NotificationPermissionStatus
import com.example.uleammed.notifications.NotificationViewModel
import com.example.uleammed.notifications.PreferredTimeConfig
import com.example.uleammed.notifications.QuestionnaireFrequency
import com.example.uleammed.notifications.SaludGeneralFrequency // ✅ NUEVO
import com.example.uleammed.notifications.TestNotificationHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: NotificationViewModel = viewModel()
) {
    val context = LocalContext.current
    val scheduleConfig by viewModel.scheduleConfig.collectAsState()

    var selectedFrequency by remember {
        mutableStateOf(
            QuestionnaireFrequency.fromDays(scheduleConfig?.periodDays ?: 7)
        )
    }

    // ✅ NUEVO: Estado para salud general
    var selectedSaludGeneralFrequency by remember {
        mutableStateOf(
            SaludGeneralFrequency.fromDays(scheduleConfig?.saludGeneralPeriodDays ?: 90)
        )
    }

    var selectedHour by remember {
        mutableIntStateOf(scheduleConfig?.preferredHour ?: 9)
    }

    var selectedMinute by remember {
        mutableIntStateOf(scheduleConfig?.preferredMinute ?: 0)
    }

    var showFrequencyDialog by remember { mutableStateOf(false) }
    var showTimeDialog by remember { mutableStateOf(false) }
    var pendingFrequency by remember { mutableStateOf<QuestionnaireFrequency?>(null) }

    // ✅ NUEVO: Estados para dialog de salud general
    var showSaludGeneralDialog by remember { mutableStateOf(false) }
    var pendingSaludGeneralFrequency by remember { mutableStateOf<SaludGeneralFrequency?>(null) }

    var showPermissionHandler by remember { mutableStateOf(false) }
    var showRemindersInApp by remember {
        mutableStateOf(scheduleConfig?.showRemindersInApp ?: true)
    }

    LaunchedEffect(scheduleConfig) {
        scheduleConfig?.let { config ->
            selectedFrequency = QuestionnaireFrequency.fromDays(config.periodDays)
            selectedHour = config.preferredHour
            selectedMinute = config.preferredMinute
            showRemindersInApp = config.showRemindersInApp

            // ✅ NUEVO: Sincronizar salud general
            selectedSaludGeneralFrequency = SaludGeneralFrequency.fromDays(
                config.saludGeneralPeriodDays
            )
        }
    }

    if (showPermissionHandler) {
        NotificationPermissionHandler(
            onPermissionGranted = {
                showPermissionHandler = false
            }
        )
    }

    if (showFrequencyDialog && pendingFrequency != null) {
        AlertDialog(
            onDismissRequest = {
                showFrequencyDialog = false
                pendingFrequency = null
            },
            icon = { Icon(Icons.Filled.Schedule, contentDescription = null) },
            title = { Text("Cambiar periodicidad") },
            text = {
                Text("¿Deseas cambiar la periodicidad de los cuestionarios regulares a ${pendingFrequency!!.displayName}? Esto regenerará las notificaciones pendientes.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.updatePeriodDays(pendingFrequency!!.days)
                        selectedFrequency = pendingFrequency!!
                        showFrequencyDialog = false
                        pendingFrequency = null
                    }
                ) {
                    Text("Confirmar")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showFrequencyDialog = false
                        pendingFrequency = null
                    }
                ) {
                    Text("Cancelar")
                }
            }
        )
    }

    // ✅ NUEVO: Dialog para salud general
    if (showSaludGeneralDialog && pendingSaludGeneralFrequency != null) {
        AlertDialog(
            onDismissRequest = {
                showSaludGeneralDialog = false
                pendingSaludGeneralFrequency = null
            },
            icon = {
                Icon(
                    Icons.Filled.HealthAndSafety,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            title = { Text("Cambiar periodicidad de Salud General") },
            text = {
                Text(
                    "¿Deseas cambiar la periodicidad del cuestionario de salud general " +
                            "a ${pendingSaludGeneralFrequency!!.displayName}?\n\n" +
                            "Esto regenerará las notificaciones pendientes."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.updateSaludGeneralPeriodDays(pendingSaludGeneralFrequency!!.days)
                        selectedSaludGeneralFrequency = pendingSaludGeneralFrequency!!
                        showSaludGeneralDialog = false
                        pendingSaludGeneralFrequency = null
                    }
                ) {
                    Text("Confirmar")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showSaludGeneralDialog = false
                        pendingSaludGeneralFrequency = null
                    }
                ) {
                    Text("Cancelar")
                }
            }
        )
    }

    if (showTimeDialog) {
        TimePickerDialog(
            initialHour = selectedHour,
            initialMinute = selectedMinute,
            onConfirm = { hour, minute ->
                selectedHour = hour
                selectedMinute = minute
                viewModel.updatePreferredTime(hour, minute)
                showTimeDialog = false
            },
            onDismiss = {
                showTimeDialog = false
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
            Text(
                text = "Permisos",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Gestiona los permisos necesarios para las notificaciones.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            NotificationPermissionStatus(
                onRequestPermission = {
                    showPermissionHandler = true
                }
            )

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Notificaciones de Cuestionarios",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Personaliza cuándo y cómo recibir recordatorios.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(24.dp))

            // ✅ MODIFICADO: Card de cuestionarios regulares
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Assignment,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(
                            text = "Cuestionarios Regulares",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Ergonomía, Síntomas Musculares, Visuales, Carga de Trabajo, Estrés, Sueño, Actividad Física, Balance Vida-Trabajo",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
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
                                        showFrequencyDialog = true
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

            Spacer(modifier = Modifier.height(16.dp))

            // ✅ NUEVO: Card de cuestionario de salud general
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.HealthAndSafety,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(
                            text = "Cuestionario de Salud General",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Reevaluación de tu estado de salud base y condiciones preexistentes",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Column(modifier = Modifier.selectableGroup()) {
                        SaludGeneralFrequency.values().forEach { frequency ->
                            SaludGeneralFrequencyOption(
                                frequency = frequency,
                                selected = selectedSaludGeneralFrequency == frequency,
                                onSelect = {
                                    if (selectedSaludGeneralFrequency != frequency) {
                                        pendingSaludGeneralFrequency = frequency
                                        showSaludGeneralDialog = true
                                    }
                                }
                            )
                            if (frequency != SaludGeneralFrequency.values().last()) {
                                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                onClick = { showTimeDialog = true }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AccessTime,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Column {
                            Text(
                                text = "Hora preferida",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = PreferredTimeConfig(selectedHour, selectedMinute).formatReadable(),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.Filled.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Notifications,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Column {
                            Text(
                                text = "Recordatorios previos",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Mostrar en Avisos 1 día antes",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Switch(
                        checked = showRemindersInApp,
                        onCheckedChange = { isEnabled ->
                            showRemindersInApp = isEnabled
                            viewModel.updateRemindersInApp(isEnabled)
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

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
                            text = "Completar los cuestionarios regularmente nos ayuda a monitorear tu salud laboral y detectar problemas tempranamente.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
                onClick = {
                    DebugNotificationHelper.diagnoseNotifications(context)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.BugReport, null)
                Spacer(Modifier.width(8.dp))
                Text("Diagnosticar Notificaciones")
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (BuildConfig.DEBUG) {
                Text(
                    text = "🧪 Herramientas de Prueba",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Solo visible en modo DEBUG. Prueba las notificaciones push.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                TestNotificationHelper.showImmediateTestNotification(context)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Filled.Notifications, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Notificación Inmediata")
                        }

                        OutlinedButton(
                            onClick = {
                                TestNotificationHelper.scheduleTestNotification10Seconds(context)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Filled.AccessTime, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Notificación en 10 seg")
                        }

                        OutlinedButton(
                            onClick = {
                                TestNotificationHelper.scheduleTestNotification1Minute(context)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Filled.Schedule, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Notificación en 1 min")
                        }

                        var showStatus by remember { mutableStateOf(false) }

                        TextButton(
                            onClick = {
                                val status = TestNotificationHelper.checkNotificationStatus(context)
                                Log.d("TestNotification", status)
                                Toast.makeText(context, "Estado en Logcat", Toast.LENGTH_SHORT).show()
                                showStatus = true
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Filled.Info, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Ver Estado del Sistema")
                        }

                        if (showStatus) {
                            Text(
                                text = TestNotificationHelper.checkNotificationStatus(context),
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }

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

                    ConfigItem("Periodicidad regular", selectedFrequency.displayName)
                    ConfigItem("Periodicidad salud general", selectedSaludGeneralFrequency.displayName)
                    ConfigItem("Hora preferida", PreferredTimeConfig(selectedHour, selectedMinute).formatReadable())
                    ConfigItem("Recordatorios previos", if (showRemindersInApp) "Habilitados" else "Deshabilitados")
                    ConfigItem("Cuestionarios activos", "${scheduleConfig?.enabledQuestionnaires?.size ?: 9}")
                }
            }
        }
    }
}

@Composable
private fun ConfigItem(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "$label:",
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
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
                text = frequency.description,
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

// ✅ NUEVO: Componente para opciones de salud general
@Composable
fun SaludGeneralFrequencyOption(
    frequency: SaludGeneralFrequency,
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
                text = frequency.description,
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerDialog(
    initialHour: Int,
    initialMinute: Int,
    onConfirm: (Int, Int) -> Unit,
    onDismiss: () -> Unit
) {
    val timePickerState = rememberTimePickerState(
        initialHour = initialHour,
        initialMinute = initialMinute,
        is24Hour = false
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.AccessTime,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text("Seleccionar hora")
            }
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                TimePicker(
                    state = timePickerState,
                    modifier = Modifier.padding(16.dp)
                )
                Text(
                    text = "Los cuestionarios se programarán para esta hora",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(timePickerState.hour, timePickerState.minute)
                }
            ) {
                Text("Confirmar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}