package com.example.uleammed.scoring

// ─────────────────────────────────────────────────────────────────────────────
// NUEVAS IMPORTACIONES — añadir al bloque de imports de HealthDashboard.kt
// ─────────────────────────────────────────────────────────────────────────────
// import android.content.Context
// import androidx.compose.material.icons.filled.PictureAsPdf
// import androidx.compose.runtime.rememberCoroutineScope
// import androidx.compose.ui.platform.LocalContext
// import kotlinx.coroutines.Dispatchers
// import kotlinx.coroutines.launch
// import kotlinx.coroutines.withContext

import android.content.Context
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// ─────────────────────────────────────────────────────────────────────────────
// 1. Botón de exportar PDF
//    Colócalo en DashboardContent(), ANTES del botón "Recalcular Análisis"
//
//    Ejemplo de uso dentro del LazyColumn de DashboardContent():
//
//        item {
//            ExportPdfButton(healthScore = healthScore)
//        }
//
//        item {
//            Button(onClick = onRecalculate, ...) { ... }  // ya existía
//        }
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Botón que genera y abre el PDF del reporte de salud laboral.
 *
 * La generación corre en IO para no bloquear el hilo principal.
 * Muestra un indicador de carga mientras trabaja.
 */
@Composable
fun ExportPdfButton(healthScore: HealthScore) {
    val context    = LocalContext.current
    val scope      = rememberCoroutineScope()
    var isLoading  by remember { mutableStateOf(false) }
    var errorMsg   by remember { mutableStateOf<String?>(null) }

    // Pequeño efecto de pulso al completar la exportación
    val scale by animateFloatAsState(
        targetValue = if (isLoading) 0.97f else 1f,
        label = "pdf_scale"
    )

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {

        Button(
            onClick = {
                if (isLoading) return@Button
                errorMsg  = null
                isLoading = true
                scope.launch {
                    try {
                        withContext(Dispatchers.IO) {
                            PdfExportUtils.exportAndOpen(context, healthScore)
                        }
                    } catch (e: Exception) {
                        errorMsg = "No se pudo generar el PDF: ${e.localizedMessage}"
                    } finally {
                        isLoading = false
                    }
                }
            },
            enabled = !isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .scale(scale),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF1565C0),          // azul oscuro diferenciado
                contentColor   = Color.White,
                disabledContainerColor = Color(0xFF1565C0).copy(alpha = 0.6f),
                disabledContentColor   = Color.White.copy(alpha = 0.7f)
            )
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    color    = Color.White,
                    strokeWidth = 2.5.dp
                )
                Spacer(Modifier.width(10.dp))
                Text("Generando PDF…", fontWeight = FontWeight.SemiBold)
            } else {
                Icon(
                    imageVector = Icons.Filled.PictureAsPdf,
                    contentDescription = "Exportar PDF"
                )
                Spacer(Modifier.width(10.dp))
                Text("Exportar Reporte PDF", fontWeight = FontWeight.SemiBold)
            }
        }

        // Mensaje de error opcional
        errorMsg?.let { msg ->
            Text(
                text  = msg,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
    }
}