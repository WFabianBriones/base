package com.example.uleammed.perfil

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.uleammed.auth.AuthViewModel
import com.example.uleammed.auth.AuthRepository
import kotlinx.coroutines.launch
import coil.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    onBack: () -> Unit,
    onSaveSuccess: () -> Unit,
    viewModel: AuthViewModel = viewModel()
) {
    val currentUser by viewModel.currentUser.collectAsState()

    // ── Estado: Perfil ────────────────────────────────────────────────────────
    var displayName       by remember { mutableStateOf(currentUser?.displayName ?: "") }
    var email             by remember { mutableStateOf(currentUser?.email ?: "") }
    var selectedImageUri  by remember { mutableStateOf<Uri?>(null) }

    // ── Estado: Contraseña ────────────────────────────────────────────────────
    var currentPassword   by remember { mutableStateOf("") }
    var newPassword       by remember { mutableStateOf("") }
    var confirmPassword   by remember { mutableStateOf("") }
    var showCurrentPwd    by remember { mutableStateOf(false) }
    var showNewPwd        by remember { mutableStateOf(false) }
    var showConfirmPwd    by remember { mutableStateOf(false) }
    var passwordExpanded  by remember { mutableStateOf(false) }

    // ── Estado: General ───────────────────────────────────────────────────────
    var isLoading         by remember { mutableStateOf(false) }
    var successMessage    by remember { mutableStateOf("") }
    var showSuccessDialog by remember { mutableStateOf(false) }
    var showErrorDialog   by remember { mutableStateOf(false) }
    var errorMessage      by remember { mutableStateOf("") }

    val scope      = rememberCoroutineScope()
    val repository = remember { AuthRepository() }

    // Sincronizar campos al cargar el usuario
    LaunchedEffect(currentUser) {
        currentUser?.let {
            displayName = it.displayName
            email       = it.email
        }
    }

    // ── Selector de imagen ────────────────────────────────────────────────────
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedImageUri = uri
    }

    // ── Validación de contraseña ──────────────────────────────────────────────
    val passwordStrength = passwordStrength(newPassword)
    val passwordsMatch   = newPassword == confirmPassword
    val passwordFormValid = currentPassword.isNotBlank()
            && newPassword.length >= 8
            && passwordsMatch

    // ── Diálogos ──────────────────────────────────────────────────────────────
    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = {
                showSuccessDialog = false
                if (successMessage.contains("perfil", ignoreCase = true)
                    || successMessage.contains("foto", ignoreCase = true)) {
                    onSaveSuccess()
                }
            },
            icon    = { Icon(Icons.Filled.CheckCircle, contentDescription = null) },
            title   = { Text("¡Listo!") },
            text    = { Text(successMessage) },
            confirmButton = {
                Button(onClick = {
                    showSuccessDialog = false
                    if (successMessage.contains("perfil", ignoreCase = true)
                        || successMessage.contains("foto", ignoreCase = true)) {
                        onSaveSuccess()
                    }
                }) { Text("Continuar") }
            }
        )
    }

    if (showErrorDialog) {
        AlertDialog(
            onDismissRequest = { showErrorDialog = false },
            icon    = { Icon(Icons.Filled.Error, contentDescription = null) },
            title   = { Text("Error") },
            text    = { Text(errorMessage) },
            confirmButton = {
                Button(onClick = { showErrorDialog = false }) { Text("Entendido") }
            }
        )
    }

    // ── Scaffold ──────────────────────────────────────────────────────────────
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Editar Perfil") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor             = MaterialTheme.colorScheme.primary,
                    titleContentColor          = MaterialTheme.colorScheme.onPrimary,
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
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // ════════════════════════════════════════════════════════════════
            // FOTO DE PERFIL
            // ════════════════════════════════════════════════════════════════
            Text(
                text       = "Foto de Perfil",
                style      = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color      = MaterialTheme.colorScheme.primary
            )

            Box(
                modifier          = Modifier.align(Alignment.CenterHorizontally),
                contentAlignment  = Alignment.BottomEnd
            ) {
                // Avatar: foto seleccionada > foto actual en Firestore > ícono por defecto
                val photoSource: Any? = selectedImageUri
                    ?: currentUser?.photoUrl?.takeIf { it.isNotBlank() }

                if (photoSource != null) {
                    AsyncImage(
                        model             = photoSource,
                        contentDescription = "Foto de perfil",
                        contentScale      = ContentScale.Crop,
                        modifier          = Modifier
                            .size(110.dp)
                            .clip(CircleShape)
                            .border(3.dp, MaterialTheme.colorScheme.primary, CircleShape)
                            .clickable { imagePickerLauncher.launch("image/*") }
                    )
                } else {
                    Surface(
                        shape    = CircleShape,
                        color    = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier
                            .size(110.dp)
                            .border(3.dp, MaterialTheme.colorScheme.primary, CircleShape)
                            .clickable { imagePickerLauncher.launch("image/*") }
                    ) {
                        Icon(
                            imageVector        = Icons.Filled.AccountCircle,
                            contentDescription = "Foto de perfil",
                            modifier           = Modifier.padding(16.dp),
                            tint               = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // Badge de edición
                Surface(
                    shape  = CircleShape,
                    color  = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .size(34.dp)
                        .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape)
                        .clickable { imagePickerLauncher.launch("image/*") }
                ) {
                    Icon(
                        imageVector        = Icons.Filled.CameraAlt,
                        contentDescription = "Cambiar foto",
                        modifier           = Modifier.padding(7.dp),
                        tint               = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }

            // Indicador de nueva imagen seleccionada
            if (selectedImageUri != null) {
                Row(
                    modifier              = Modifier.align(Alignment.CenterHorizontally),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint     = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text  = "Nueva imagen lista para subir",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            TextButton(
                onClick  = { imagePickerLauncher.launch("image/*") },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Icon(Icons.Filled.PhotoLibrary, contentDescription = null,
                    modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Seleccionar desde galería")
            }

            HorizontalDivider()

            // ════════════════════════════════════════════════════════════════
            // INFORMACIÓN PERSONAL
            // ════════════════════════════════════════════════════════════════
            Text(
                text       = "Información Personal",
                style      = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color      = MaterialTheme.colorScheme.primary
            )

            OutlinedTextField(
                value         = displayName,
                onValueChange = { displayName = it },
                label         = { Text("Nombre completo") },
                leadingIcon   = { Icon(Icons.Filled.Person, contentDescription = null) },
                singleLine    = true,
                modifier      = Modifier.fillMaxWidth(),
                enabled       = !isLoading,
                supportingText = {
                    Text("Este nombre se mostrará en tu perfil",
                        style = MaterialTheme.typography.bodySmall)
                }
            )

            OutlinedTextField(
                value         = email,
                onValueChange = {},
                label         = { Text("Correo electrónico") },
                leadingIcon   = { Icon(Icons.Filled.Email, contentDescription = null) },
                singleLine    = true,
                modifier      = Modifier.fillMaxWidth(),
                enabled       = false,
                colors        = OutlinedTextFieldDefaults.colors(
                    disabledTextColor        = MaterialTheme.colorScheme.onSurface,
                    disabledBorderColor      = MaterialTheme.colorScheme.outline,
                    disabledLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    disabledLabelColor       = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                supportingText = {
                    Text("No se puede modificar por seguridad",
                        style = MaterialTheme.typography.bodySmall)
                }
            )

            // Botón guardar perfil + foto
            val profileChanged = displayName != currentUser?.displayName || selectedImageUri != null
            Button(
                onClick = {
                    if (displayName.isBlank()) {
                        errorMessage = "El nombre no puede estar vacío"
                        showErrorDialog = true
                        return@Button
                    }
                    if (displayName.length < 2) {
                        errorMessage = "El nombre debe tener al menos 2 caracteres"
                        showErrorDialog = true
                        return@Button
                    }
                    isLoading = true
                    scope.launch {
                        try {
                            val userId = currentUser?.uid
                                ?: throw Exception("Usuario no autenticado")

                            // 1. Subir foto si hay una nueva seleccionada
                            var photoUrl = currentUser?.photoUrl ?: ""
                            if (selectedImageUri != null) {
                                val uploadResult = repository.uploadProfilePhoto(
                                    userId = userId,
                                    imageUri = selectedImageUri!!
                                )
                                uploadResult.onSuccess { url ->
                                    photoUrl = url
                                }.onFailure { e ->
                                    isLoading    = false
                                    errorMessage = "Error al subir la imagen: ${e.message}"
                                    showErrorDialog = true
                                    return@launch
                                }
                            }

                            // 2. Actualizar nombre y/o foto en Firestore
                            val updates = mutableMapOf<String, Any>(
                                "displayName" to displayName
                            )
                            if (photoUrl.isNotBlank()) updates["photoUrl"] = photoUrl

                            val result = repository.updateUserProfile(userId, updates)
                            result.onSuccess {
                                // ✅ Refrescar el usuario en memoria para que
                                // ProfileScreen muestre la foto inmediatamente
                                viewModel.refreshCurrentUser()
                                selectedImageUri  = null
                                isLoading         = false
                                successMessage    = "Perfil actualizado correctamente."
                                showSuccessDialog = true
                            }.onFailure { e ->
                                isLoading    = false
                                errorMessage = e.message ?: "Error desconocido"
                                showErrorDialog = true
                            }
                        } catch (e: Exception) {
                            isLoading    = false
                            errorMessage = e.message ?: "Error al guardar cambios"
                            showErrorDialog = true
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                enabled  = !isLoading && profileChanged
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color    = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Icon(Icons.Filled.Save, contentDescription = null,
                        modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Guardar Perfil")
                }
            }

            HorizontalDivider()

            // ════════════════════════════════════════════════════════════════
            // CAMBIO DE CONTRASEÑA
            // ════════════════════════════════════════════════════════════════
            Row(
                modifier              = Modifier
                    .fillMaxWidth()
                    .clickable { passwordExpanded = !passwordExpanded }
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Filled.Lock,
                        contentDescription = null,
                        tint     = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text       = "Cambiar Contraseña",
                        style      = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color      = MaterialTheme.colorScheme.primary
                    )
                }
                Icon(
                    imageVector        = if (passwordExpanded) Icons.Filled.ExpandLess
                    else Icons.Filled.ExpandMore,
                    contentDescription = null,
                    tint               = MaterialTheme.colorScheme.primary
                )
            }

            if (passwordExpanded) {
                // Info card
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
                    )
                ) {
                    Row(
                        modifier              = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Filled.Info, contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(18.dp))
                        Text(
                            text  = "Necesitas ingresar tu contraseña actual para confirmar el cambio.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Contraseña actual
                OutlinedTextField(
                    value             = currentPassword,
                    onValueChange     = { currentPassword = it },
                    label             = { Text("Contraseña actual") },
                    leadingIcon       = { Icon(Icons.Filled.Lock, contentDescription = null) },
                    trailingIcon      = {
                        IconButton(onClick = { showCurrentPwd = !showCurrentPwd }) {
                            Icon(
                                if (showCurrentPwd) Icons.Filled.VisibilityOff
                                else Icons.Filled.Visibility,
                                contentDescription = "Ver contraseña"
                            )
                        }
                    },
                    visualTransformation = if (showCurrentPwd) VisualTransformation.None
                    else PasswordVisualTransformation(),
                    keyboardOptions      = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine           = true,
                    modifier             = Modifier.fillMaxWidth(),
                    enabled              = !isLoading
                )

                // Nueva contraseña
                OutlinedTextField(
                    value             = newPassword,
                    onValueChange     = { newPassword = it },
                    label             = { Text("Nueva contraseña") },
                    leadingIcon       = { Icon(Icons.Filled.LockOpen, contentDescription = null) },
                    trailingIcon      = {
                        IconButton(onClick = { showNewPwd = !showNewPwd }) {
                            Icon(
                                if (showNewPwd) Icons.Filled.VisibilityOff
                                else Icons.Filled.Visibility,
                                contentDescription = "Ver contraseña"
                            )
                        }
                    },
                    visualTransformation = if (showNewPwd) VisualTransformation.None
                    else PasswordVisualTransformation(),
                    keyboardOptions      = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine           = true,
                    modifier             = Modifier.fillMaxWidth(),
                    enabled              = !isLoading,
                    isError              = newPassword.isNotEmpty() && newPassword.length < 8,
                    supportingText       = {
                        if (newPassword.isNotEmpty()) {
                            Text(
                                text  = passwordStrength.label,
                                color = passwordStrength.color
                            )
                        }
                    }
                )

                // Barra de fortaleza de contraseña
                if (newPassword.isNotEmpty()) {
                    PasswordStrengthBar(strength = passwordStrength)
                }

                // Confirmar nueva contraseña
                OutlinedTextField(
                    value             = confirmPassword,
                    onValueChange     = { confirmPassword = it },
                    label             = { Text("Confirmar nueva contraseña") },
                    leadingIcon       = { Icon(Icons.Filled.LockOpen, contentDescription = null) },
                    trailingIcon      = {
                        IconButton(onClick = { showConfirmPwd = !showConfirmPwd }) {
                            Icon(
                                if (showConfirmPwd) Icons.Filled.VisibilityOff
                                else Icons.Filled.Visibility,
                                contentDescription = "Ver contraseña"
                            )
                        }
                    },
                    visualTransformation = if (showConfirmPwd) VisualTransformation.None
                    else PasswordVisualTransformation(),
                    keyboardOptions      = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine           = true,
                    modifier             = Modifier.fillMaxWidth(),
                    enabled              = !isLoading,
                    isError              = confirmPassword.isNotEmpty() && !passwordsMatch,
                    supportingText       = {
                        if (confirmPassword.isNotEmpty()) {
                            if (passwordsMatch) {
                                Text("Las contraseñas coinciden ✓",
                                    color = MaterialTheme.colorScheme.primary)
                            } else {
                                Text("Las contraseñas no coinciden",
                                    color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                )

                // Botón cambiar contraseña
                Button(
                    onClick = {
                        isLoading = true
                        scope.launch {
                            try {
                                val result = repository.updatePassword(
                                    currentPassword = currentPassword,
                                    newPassword     = newPassword
                                )
                                result.onSuccess {
                                    isLoading       = false
                                    currentPassword = ""
                                    newPassword     = ""
                                    confirmPassword = ""
                                    passwordExpanded = false
                                    successMessage  = "Contraseña actualizada correctamente."
                                    showSuccessDialog = true
                                }.onFailure { e ->
                                    isLoading    = false
                                    errorMessage = when {
                                        e.message?.contains("wrong-password") == true ||
                                                e.message?.contains("invalid-credential") == true ->
                                            "La contraseña actual es incorrecta."
                                        e.message?.contains("weak-password") == true ->
                                            "La nueva contraseña es demasiado débil."
                                        e.message?.contains("requires-recent-login") == true ->
                                            "Por seguridad, cierra sesión y vuelve a iniciarla antes de cambiar la contraseña."
                                        else -> e.message ?: "Error al cambiar la contraseña."
                                    }
                                    showErrorDialog = true
                                }
                            } catch (e: Exception) {
                                isLoading    = false
                                errorMessage = e.message ?: "Error inesperado"
                                showErrorDialog = true
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    enabled  = !isLoading && passwordFormValid,
                    colors   = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    )
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color    = MaterialTheme.colorScheme.onSecondary
                        )
                    } else {
                        Icon(Icons.Filled.LockReset, contentDescription = null,
                            modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Cambiar Contraseña")
                    }
                }
            }

            // ════════════════════════════════════════════════════════════════
            // BOTÓN CANCELAR
            // ════════════════════════════════════════════════════════════════
            OutlinedButton(
                onClick  = onBack,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                enabled  = !isLoading
            ) {
                Icon(Icons.Filled.Cancel, contentDescription = null,
                    modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Cancelar")
            }

            // ════════════════════════════════════════════════════════════════
            // CARD DE SEGURIDAD
            // ════════════════════════════════════════════════════════════════
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors   = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier            = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Security, contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary)
                        Text(
                            text       = "Privacidad y Seguridad",
                            style      = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text("• Tu información está protegida y encriptada",
                        style = MaterialTheme.typography.bodySmall)
                    Text("• Solo tú puedes ver y editar tu perfil",
                        style = MaterialTheme.typography.bodySmall)
                    Text("• No compartimos tu información con terceros",
                        style = MaterialTheme.typography.bodySmall)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Fortaleza de contraseña
// ─────────────────────────────────────────────────────────────────────────────

enum class PasswordStrengthLevel(
    val label: String,
    val color: androidx.compose.ui.graphics.Color,
    val fraction: Float
) {
    WEAK(   "Débil",    androidx.compose.ui.graphics.Color(0xFFE53935), 0.25f),
    FAIR(   "Regular",  androidx.compose.ui.graphics.Color(0xFFFF8F00), 0.50f),
    GOOD(   "Buena",    androidx.compose.ui.graphics.Color(0xFF7CB342), 0.75f),
    STRONG( "Fuerte",   androidx.compose.ui.graphics.Color(0xFF2E7D32), 1.00f)
}

fun passwordStrength(password: String): PasswordStrengthLevel {
    if (password.length < 8) return PasswordStrengthLevel.WEAK
    var score = 0
    if (password.length >= 12)               score++
    if (password.any { it.isUpperCase() })   score++
    if (password.any { it.isDigit() })       score++
    if (password.any { !it.isLetterOrDigit() }) score++
    return when (score) {
        0    -> PasswordStrengthLevel.WEAK
        1    -> PasswordStrengthLevel.FAIR
        2, 3 -> PasswordStrengthLevel.GOOD
        else -> PasswordStrengthLevel.STRONG
    }
}

@Composable
fun PasswordStrengthBar(strength: PasswordStrengthLevel) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        LinearProgressIndicator(
            progress         = { strength.fraction },
            modifier         = Modifier.fillMaxWidth().height(6.dp)
                .clip(androidx.compose.foundation.shape.RoundedCornerShape(3.dp)),
            color            = strength.color,
            trackColor       = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}