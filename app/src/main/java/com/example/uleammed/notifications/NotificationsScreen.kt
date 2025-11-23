package com.example.uleammed.notifications

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.uleammed.QuestionnaireType
import com.example.uleammed.Screen
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun NotificationsContent(
    onNavigateToQuestionnaire: (String) -> Unit,
    viewModel: NotificationViewModel = viewModel()
) {
    val notifications by viewModel.notifications.collectAsState()
    val unreadCount by viewModel.unreadCount.collectAsState()

    var showClearDialog by remember { mutableStateOf(false) }
    var clearDialogType by remember { mutableStateOf(ClearType.READ) }

    LaunchedEffect(Unit) {
        viewModel.checkForNewNotifications()
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            icon = { Icon(Icons.Filled.Delete, contentDescription = null) },
            title = {
                Text(
                    when (clearDialogType) {
                        ClearType.READ -> "Limpiar notificaciones leídas"
                        ClearType.ALL -> "Limpiar todas las notificaciones"
                        ClearType.OLD -> "Limpiar notificaciones antiguas"
                    }
                )
            },
            text = {
                Text(
                    when (clearDialogType) {
                        ClearType.READ -> "¿Deseas eliminar todas las notificaciones que ya has leído?"
                        ClearType.ALL -> "¿Deseas eliminar TODAS las notificaciones? Esta acción no se puede deshacer."
                        ClearType.OLD -> "¿Deseas eliminar las notificaciones completadas de hace más de 30 días?"
                    }
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        when (clearDialogType) {
                            ClearType.READ -> viewModel.clearReadNotifications()
                            ClearType.ALL -> viewModel.clearAllNotifications()
                            ClearType.OLD -> viewModel.cleanupOldNotifications()
                        }
                        showClearDialog = false
                    }
                ) {
                    Text("Limpiar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Avisos",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                if (unreadCount > 0) {
                    Text(
                        text = "$unreadCount pendiente${if (unreadCount > 1) "s" else ""}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row {
                IconButton(onClick = { viewModel.checkForNewNotifications() }) {
                    Icon(
                        imageVector = Icons.Filled.Refresh,
                        contentDescription = "Actualizar"
                    )
                }
                // Menú de opciones de limpieza
                var showMenu by remember { mutableStateOf(false) }
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(
                            imageVector = Icons.Filled.MoreVert,
                            contentDescription = "Más opciones"
                        )
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Limpiar leídas") },
                            onClick = {
                                clearDialogType = ClearType.READ
                                showClearDialog = true
                                showMenu = false
                            },
                            leadingIcon = {
                                Icon(Icons.Filled.DoneAll, contentDescription = null)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Limpiar antiguas") },
                            onClick = {
                                clearDialogType = ClearType.OLD
                                showClearDialog = true
                                showMenu = false
                            },
                            leadingIcon = {
                                Icon(Icons.Filled.DeleteSweep, contentDescription = null)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Limpiar todas") },
                            onClick = {
                                clearDialogType = ClearType.ALL
                                showClearDialog = true
                                showMenu = false
                            },
                            leadingIcon = {
                                Icon(Icons.Filled.DeleteForever, contentDescription = null)
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (notifications.isEmpty()) {
            // Estado vacío
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
                        imageVector = Icons.Filled.Notifications,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
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
                        text = "Los recordatorios de cuestionarios aparecerán aquí cuando completes un cuestionario por primera vez",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            // Lista de notificaciones
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(notifications, key = { it.id }) { notification ->
                    NotificationCard(
                        notification = notification,
                        onMarkAsRead = { viewModel.markAsRead(notification.id) },
                        onDelete = { viewModel.deleteNotification(notification.id) },
                        onOpenQuestionnaire = {
                            viewModel.markAsRead(notification.id)
                            val route = getRouteForQuestionnaireType(notification.questionnaireType)
                            onNavigateToQuestionnaire(route)
                        }
                    )
                }
            }
        }
    }
}

// Enum para tipos de limpieza
private enum class ClearType {
    READ,    // Solo leídas
    ALL,     // Todas
    OLD      // Antiguas (30+ días)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationCard(
    notification: QuestionnaireNotification,
    onMarkAsRead: () -> Unit,
    onDelete: () -> Unit,
    onOpenQuestionnaire: () -> Unit
) {
    // ✅ CAMBIO CRÍTICO: Usar dueDate en lugar de createdAt
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            icon = { Icon(Icons.Filled.Delete, contentDescription = null) },
            title = { Text("Eliminar notificación") },
            text = { Text("¿Deseas eliminar esta notificación?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteDialog = false
                    }
                ) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (!notification.isRead) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        onClick = onOpenQuestionnaire
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = getIconForQuestionnaireType(notification.questionnaireType),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (!notification.isRead) {
                                Surface(
                                    modifier = Modifier.size(8.dp),
                                    shape = MaterialTheme.shapes.small,
                                    color = MaterialTheme.colorScheme.primary
                                ) {}
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            Text(
                                text = notification.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = if (!notification.isRead) FontWeight.Bold else FontWeight.Normal,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                IconButton(
                    onClick = { showDeleteDialog = true },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Eliminar",
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = notification.message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.AccessTime,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (notification.dueDate <= System.currentTimeMillis()) {
                            "✅ Disponible ahora"
                        } else {
                            "Fecha: ${dateFormat.format(Date(notification.dueDate))}"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (notification.dueDate <= System.currentTimeMillis()) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        fontWeight = if (notification.dueDate <= System.currentTimeMillis()) {
                            FontWeight.Bold
                        } else {
                            FontWeight.Normal
                        }
                    )
                }
            }
        }
    }
}

private fun getIconForQuestionnaireType(type: QuestionnaireType): ImageVector {
    return when (type) {
        QuestionnaireType.ERGONOMIA -> Icons.Filled.Computer
        QuestionnaireType.SINTOMAS_MUSCULARES -> Icons.Filled.MonitorHeart
        QuestionnaireType.SINTOMAS_VISUALES -> Icons.Filled.RemoveRedEye
        QuestionnaireType.CARGA_TRABAJO -> Icons.Filled.Work
        QuestionnaireType.ESTRES_SALUD_MENTAL -> Icons.Filled.Psychology
        QuestionnaireType.HABITOS_SUENO -> Icons.Filled.NightlightRound
        QuestionnaireType.ACTIVIDAD_FISICA -> Icons.Filled.SportsGymnastics
        QuestionnaireType.BALANCE_VIDA_TRABAJO -> Icons.Filled.Scale
    }
}

private fun getRouteForQuestionnaireType(type: QuestionnaireType): String {
    return when (type) {
        QuestionnaireType.ERGONOMIA -> Screen.ErgonomiaQuestionnaire.route
        QuestionnaireType.SINTOMAS_MUSCULARES -> Screen.SintomasMuscularesQuestionnaire.route
        QuestionnaireType.SINTOMAS_VISUALES -> Screen.SintomasVisualesQuestionnaire.route
        QuestionnaireType.CARGA_TRABAJO -> Screen.CargaTrabajoQuestionnaire.route
        QuestionnaireType.ESTRES_SALUD_MENTAL -> Screen.EstresSaludMentalQuestionnaire.route
        QuestionnaireType.HABITOS_SUENO -> Screen.HabitosSuenoQuestionnaire.route
        QuestionnaireType.ACTIVIDAD_FISICA -> Screen.ActividadFisicaQuestionnaire.route
        QuestionnaireType.BALANCE_VIDA_TRABAJO -> Screen.BalanceVidaTrabajoQuestionnaire.route
    }
}