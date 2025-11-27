package com.example.uleammed.auth

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Dialog para restablecer contraseña
 * Agregar en LoginScreen.kt
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForgotPasswordDialog(
    onDismiss: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var showSuccessMessage by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val scope = rememberCoroutineScope()

    if (showSuccessMessage) {
        AlertDialog(
            onDismissRequest = {
                showSuccessMessage = false
                onDismiss()
            },
            icon = { Icon(Icons.Filled.CheckCircle, contentDescription = null) },
            title = { Text("Correo Enviado") },
            text = {
                Text("Se ha enviado un enlace de restablecimiento a $email. Revisa tu bandeja de entrada.")
            },
            confirmButton = {
                Button(onClick = {
                    showSuccessMessage = false
                    onDismiss()
                }) {
                    Text("Entendido")
                }
            }
        )
    }

    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        icon = { Icon(Icons.Filled.Lock, contentDescription = null) },
        title = { Text("Restablecer Contraseña") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Ingresa tu correo electrónico y te enviaremos un enlace para restablecer tu contraseña.",
                    style = MaterialTheme.typography.bodyMedium
                )

                OutlinedTextField(
                    value = email,
                    onValueChange = {
                        email = it
                        errorMessage = null
                    },
                    label = { Text("Correo electrónico") },
                    leadingIcon = {
                        Icon(Icons.Filled.Email, contentDescription = null)
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading,
                    isError = errorMessage != null,
                    supportingText = {
                        errorMessage?.let {
                            Text(
                                text = it,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (email.isBlank()) {
                        errorMessage = "Ingresa tu correo electrónico"
                        return@Button
                    }

                    if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                        errorMessage = "Correo electrónico inválido"
                        return@Button
                    }

                    isLoading = true
                    scope.launch {
                        try {
                            FirebaseAuth.getInstance()
                                .sendPasswordResetEmail(email)
                                .await()

                            isLoading = false
                            showSuccessMessage = true
                        } catch (e: Exception) {
                            isLoading = false
                            errorMessage = when {
                                e.message?.contains("user-not-found") == true ->
                                    "No existe una cuenta con este correo"
                                e.message?.contains("invalid-email") == true ->
                                    "Correo electrónico inválido"
                                e.message?.contains("network") == true ->
                                    "Error de conexión. Verifica tu internet"
                                else -> "Error: ${e.message}"
                            }
                        }
                    }
                },
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Enviar")
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isLoading
            ) {
                Text("Cancelar")
            }
        }
    )
}