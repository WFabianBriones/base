package com.example.uleammed.notifications

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat

/**
 * Verifica si el permiso de notificaciones está otorgado
 */
fun Context.hasNotificationPermission(): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    } else {
        // Android < 13 no necesita permiso
        true
    }
}

/**
 * Composable para solicitar permiso de notificaciones
 */
@Composable
fun RequestNotificationPermission(
    onPermissionGranted: () -> Unit = {},
    onPermissionDenied: () -> Unit = {}
) {
    val context = LocalContext.current

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val launcher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            if (isGranted) {
                onPermissionGranted()
            } else {
                onPermissionDenied()
            }
        }

        LaunchedEffect(Unit) {
            if (!context.hasNotificationPermission()) {
                launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                onPermissionGranted()
            }
        }
    } else {
        // Android < 13 no necesita permiso
        LaunchedEffect(Unit) {
            onPermissionGranted()
        }
    }
}

/**
 * Dialog para explicar por qué necesitamos el permiso
 */
@Composable
fun NotificationPermissionDialog(
    onDismiss: () -> Unit,
    onRequestPermission: () -> Unit,
    onOpenSettings: () -> Unit,
    isPermanentlyDenied: Boolean = false
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Filled.Notifications,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(
                text = if (isPermanentlyDenied) {
                    "Permiso de Notificaciones Requerido"
                } else {
                    "Mantente al Día con tus Cuestionarios"
                },
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = if (isPermanentlyDenied) {
                        "Has denegado el permiso de notificaciones. Para recibir recordatorios de tus cuestionarios, debes habilitar las notificaciones manualmente en la configuración."
                    } else {
                        "Necesitamos tu permiso para enviarte recordatorios sobre tus cuestionarios de salud. Así no olvidarás completarlos a tiempo."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )

                if (!isPermanentlyDenied) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            PermissionBenefitItem(
                                icon = Icons.Filled.Alarm,
                                text = "Recordatorios oportunos"
                            )
                            PermissionBenefitItem(
                                icon = Icons.Filled.TrendingUp,
                                text = "Mejora tu seguimiento de salud"
                            )
                            PermissionBenefitItem(
                                icon = Icons.Filled.Security,
                                text = "100% privado y seguro"
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = if (isPermanentlyDenied) onOpenSettings else onRequestPermission
            ) {
                Text(
                    if (isPermanentlyDenied) "Abrir Configuración" else "Permitir Notificaciones"
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Ahora No")
            }
        }
    )
}

@Composable
private fun PermissionBenefitItem(
    icon: ImageVector,
    text: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium
        )
    }
}

/**
 * Composable completo con manejo de estados
 */
@Composable
fun NotificationPermissionHandler(
    onPermissionGranted: () -> Unit = {}
) {
    val context = LocalContext.current
    var showDialog by remember { mutableStateOf(false) }
    var isPermanentlyDenied by remember { mutableStateOf(false) }
    var permissionRequested by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        permissionRequested = true
        if (isGranted) {
            showDialog = false
            onPermissionGranted()
        } else {
            // Verificar si el usuario seleccionó "No volver a preguntar"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val activity = context as? Activity
                isPermanentlyDenied = activity?.shouldShowRequestPermissionRationale(
                    Manifest.permission.POST_NOTIFICATIONS
                ) == false && permissionRequested
                showDialog = true
            }
        }
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!context.hasNotificationPermission()) {
                showDialog = true
            } else {
                onPermissionGranted()
            }
        } else {
            onPermissionGranted()
        }
    }

    if (showDialog) {
        NotificationPermissionDialog(
            onDismiss = { showDialog = false },
            onRequestPermission = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            },
            onOpenSettings = {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                }
                context.startActivity(intent)
                showDialog = false
            },
            isPermanentlyDenied = isPermanentlyDenied
        )
    }
}

/**
 * Widget para mostrar estado del permiso en configuración
 */
@Composable
fun NotificationPermissionStatus(
    onRequestPermission: () -> Unit
) {
    val context = LocalContext.current
    val hasPermission = context.hasNotificationPermission()

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (hasPermission) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            } else {
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
            }
        )
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
                    imageVector = if (hasPermission) Icons.Filled.CheckCircle else Icons.Filled.Warning,
                    contentDescription = null,
                    tint = if (hasPermission) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.error
                    }
                )
                Column {
                    Text(
                        text = "Permiso de Notificaciones",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (hasPermission) "Habilitado" else "Deshabilitado",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (hasPermission) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.error
                        }
                    )
                }
            }

            if (!hasPermission) {
                Button(
                    onClick = onRequestPermission,
                    modifier = Modifier.height(36.dp)
                ) {
                    Text("Habilitar", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}