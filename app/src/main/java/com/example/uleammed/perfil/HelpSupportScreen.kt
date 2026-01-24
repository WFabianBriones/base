package com.example.uleammed.perfil

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Pantalla de Ayuda y Soporte
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpSupportScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current

    val faqItems = remember {
        listOf(
            FAQHelpItem(
                "¿Cómo completo los cuestionarios?",
                "Ve a la pestaña 'Explorar' en la barra inferior. Allí encontrarás todos los cuestionarios disponibles. Toca uno para comenzar.",
                Icons.Filled.Assignment
            ),
            FAQHelpItem(
                "¿Con qué frecuencia debo completar los cuestionarios?",
                "Recomendamos completarlos cada 7 días. Puedes configurar la frecuencia en Configuración.",
                Icons.Filled.Schedule
            ),
            FAQHelpItem(
                "¿Mis datos son privados?",
                "Sí. Tu información está encriptada y protegida. Solo tú tienes acceso a tus datos de salud.",
                Icons.Filled.Security
            ),
            FAQHelpItem(
                "¿Cómo desactivo las notificaciones?",
                "Ve a tu Perfil > Configuración y ajusta las preferencias de notificaciones.",
                Icons.Filled.Notifications
            ),
            FAQHelpItem(
                "¿Puedo editar mis respuestas?",
                "Cada cuestionario se guarda como una respuesta individual. Puedes completar uno nuevo en cualquier momento.",
                Icons.Filled.Edit
            ),
            FAQHelpItem(
                "¿La app funciona sin internet?",
                "La mayoría de funciones requieren conexión. Los recursos descargados sí están disponibles offline.",
                Icons.Filled.CloudOff
            )
        )
    }

    var expandedFaqId by remember { mutableStateOf<Int?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ayuda y Soporte") },
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(20.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.HelpOutline,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Column {
                            Text(
                                text = "¿Necesitas ayuda?",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Estamos aquí para ayudarte",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }

            // Contacto Rápido
            item {
                Text(
                    text = "Contacto",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            item {
                ContactOption(
                    icon = Icons.Filled.Email,
                    title = "Email",
                    description = "soporte@uleam.edu.ec",
                    onClick = {
                        val intent = Intent(Intent.ACTION_SENDTO).apply {
                            data = Uri.parse("mailto:soporte@uleam.edu.ec")
                            putExtra(Intent.EXTRA_SUBJECT, "Soporte ULEAM Salud")
                        }
                        context.startActivity(intent)
                    }
                )
            }

            item {
                ContactOption(
                    icon = Icons.Filled.Phone,
                    title = "Teléfono",
                    description = "+593 5-262-3740",
                    onClick = {
                        val intent = Intent(Intent.ACTION_DIAL).apply {
                            data = Uri.parse("tel:+59352623740")
                        }
                        context.startActivity(intent)
                    }
                )
            }

            item {
                ContactOption(
                    icon = Icons.Filled.Language,
                    title = "Sitio Web ULEAM",
                    description = "www.uleam.edu.ec",
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW).apply {
                            data = Uri.parse("https://www.uleam.edu.ec")
                        }
                        context.startActivity(intent)
                    }
                )
            }

            // Preguntas Frecuentes
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Preguntas Frecuentes",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            items(faqItems.size) { index ->
                val item = faqItems[index]
                FAQCard(
                    faq = item,
                    isExpanded = expandedFaqId == index,
                    onToggle = {
                        expandedFaqId = if (expandedFaqId == index) null else index
                    }
                )
            }

            // Recursos Adicionales
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Recursos Adicionales",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            item {
                ResourceCard(
                    icon = Icons.Filled.LibraryBooks,
                    title = "Guía de Usuario",
                    description = "Manual completo de uso de la aplicación",
                    onClick = {
                        // TODO: Abrir guía en PDF o web
                    }
                )
            }

            item {
                ResourceCard(
                    icon = Icons.Filled.VideoLibrary,
                    title = "Tutoriales en Video",
                    description = "Aprende a usar todas las funciones",
                    onClick = {
                        // TODO: Abrir playlist de tutoriales
                    }
                )
            }

            item {
                ResourceCard(
                    icon = Icons.Filled.BugReport,
                    title = "Reportar un Problema",
                    description = "Ayúdanos a mejorar la app",
                    onClick = {
                        val intent = Intent(Intent.ACTION_SENDTO).apply {
                            data = Uri.parse("mailto:soporte@uleam.edu.ec")
                            putExtra(Intent.EXTRA_SUBJECT, "Reporte de Bug - ULEAM Salud")
                            putExtra(Intent.EXTRA_TEXT, "Describe el problema:\n\n")
                        }
                        context.startActivity(intent)
                    }
                )
            }

            // Info de versión
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "ULEAM Salud",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Versión 1.0.0",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "© 2025 Universidad Laica Eloy Alfaro de Manabí",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactOption(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.padding(12.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FAQCard(
    faq: FAQHelpItem,
    isExpanded: Boolean,
    onToggle: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onToggle
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = faq.icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = faq.question,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                Icon(
                    imageVector = if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = if (isExpanded) "Contraer" else "Expandir"
                )
            }

            if (isExpanded) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = faq.answer,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResourceCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit
) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

data class FAQHelpItem(
    val question: String,
    val answer: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)