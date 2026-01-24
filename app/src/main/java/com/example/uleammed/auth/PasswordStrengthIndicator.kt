package com.example.uleammed.auth

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Indicador de fortaleza de contraseña
 * Opcional para RegisterScreen.kt
 */
@Composable
fun PasswordStrengthIndicator(
    password: String,
    modifier: Modifier = Modifier
) {
    val strength = calculatePasswordStrength(password)

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            repeat(4) { index ->
                LinearProgressIndicator(
                    progress = { if (index < strength.level) 1f else 0f },
                    modifier = Modifier
                        .weight(1f)
                        .height(4.dp),
                    color = strength.color,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
            }
        }

        Text(
            text = strength.label,
            style = MaterialTheme.typography.labelSmall,
            color = strength.color,
            fontWeight = FontWeight.Medium
        )
    }
}

private data class PasswordStrength(
    val level: Int,
    val label: String,
    val color: Color
)

private fun calculatePasswordStrength(password: String): PasswordStrength {
    if (password.isEmpty()) {
        return PasswordStrength(0, "", Color.Gray)
    }

    var score = 0

    // Longitud
    if (password.length >= 6) score++
    if (password.length >= 8) score++

    // Tiene números
    if (password.any { it.isDigit() }) score++

    // Tiene mayúsculas
    if (password.any { it.isUpperCase() }) score++

    // Tiene caracteres especiales
    if (password.any { !it.isLetterOrDigit() }) score++

    return when {
        score <= 1 -> PasswordStrength(1, "Muy débil", Color(0xFFEF5350))
        score == 2 -> PasswordStrength(2, "Débil", Color(0xFFFF9800))
        score == 3 -> PasswordStrength(3, "Buena", Color(0xFFFDD835))
        else -> PasswordStrength(4, "Fuerte", Color(0xFF66BB6A))
    }
}

// ✅ USO EN RegisterScreen.kt:
// Después del OutlinedTextField de password, agregar:
/*
if (password.isNotEmpty()) {
    PasswordStrengthIndicator(password = password)
}
*/