package com.example.uleammed.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/**
 * Pantalla para gestionar usuarios (solo super usuario)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserManagementScreen(
    repository: AdminRepository = AdminRepository(),
    onBack: () -> Unit
) {
    var users by remember { mutableStateOf<List<UserWithRole>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var selectedUser by remember { mutableStateOf<UserWithRole?>(null) }
    var showDialog by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    // Cargar usuarios
    LaunchedEffect(Unit) {
        isLoading = true
        val result = repository.getAllUsers()
        result.onSuccess {
            users = it
            isLoading = false
        }.onFailure {
            errorMessage = it.message
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Gestión de Usuarios") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Volver")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            scope.launch {
                                isLoading = true
                                val result = repository.getAllUsers()
                                result.onSuccess {
                                    users = it
                                    isLoading = false
                                }.onFailure {
                                    errorMessage = it.message
                                    isLoading = false
                                }
                            }
                        }
                    ) {
                        Icon(Icons.Default.Refresh, "Actualizar")
                    }
                }
            )
        }
    ) { padding ->
        when {
            isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            errorMessage != null -> {
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
                        Text(errorMessage ?: "Error desconocido")
                    }
                }
            }

            users.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No hay usuarios registrados")
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Estadísticas rápidas
                    item {
                        UserStatsCard(users = users)
                    }

                    // Lista de usuarios
                    items(users) { user ->
                        UserCard(
                            user = user,
                            onEditRole = {
                                selectedUser = user
                                showDialog = true
                            }
                        )
                    }
                }
            }
        }
    }

    // Diálogo para cambiar rol
    if (showDialog && selectedUser != null) {
        ChangeRoleDialog(
            user = selectedUser!!,
            repository = repository,
            onDismiss = { showDialog = false },
            onSuccess = {
                showDialog = false
                // Recargar usuarios
                scope.launch {
                    val result = repository.getAllUsers()
                    result.onSuccess { users = it }
                }
            }
        )
    }
}

/**
 * Card con estadísticas de usuarios
 */
@Composable
private fun UserStatsCard(users: List<UserWithRole>) {
    val totalUsers = users.size
    val admins = users.count { it.isAdmin() }
    val superUsers = users.count { it.isSuperUser() }
    val regularUsers = users.count { it.getUserRole() == UserRole.USER }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = totalUsers.toString(),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Total",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = superUsers.toString(),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Super Usuarios",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = admins.toString(),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Admins",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = regularUsers.toString(),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Usuarios",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

/**
 * Card de usuario individual
 */
@Composable
private fun UserCard(
    user: UserWithRole,
    onEditRole: () -> Unit
) {
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
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Avatar
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = user.displayName.firstOrNull()?.uppercase() ?: "?",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                Column {
                    Text(
                        text = user.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = user.email,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Rol
                    Spacer(Modifier.height(4.dp))
                    RoleBadge(role = user.getUserRole())
                }
            }

            // Botón editar rol
            IconButton(onClick = onEditRole) {
                Icon(Icons.Default.Edit, "Editar rol")
            }
        }
    }
}

/**
 * Badge para mostrar el rol
 */
@Composable
private fun RoleBadge(role: UserRole) {
    val (color, icon) = when (role) {
        UserRole.SUPERUSER -> Pair(
            MaterialTheme.colorScheme.error,
            Icons.Default.SupervisorAccount
        )
        UserRole.ADMIN -> Pair(
            MaterialTheme.colorScheme.primary,
            Icons.Default.AdminPanelSettings
        )
        UserRole.USER -> Pair(
            MaterialTheme.colorScheme.secondary,
            Icons.Default.Person
        )
    }

    Surface(
        shape = MaterialTheme.shapes.small,
        color = color.copy(alpha = 0.1f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = color
            )
            Text(
                text = role.displayName,
                style = MaterialTheme.typography.labelSmall,
                color = color
            )
        }
    }
}

/**
 * Diálogo para cambiar el rol de un usuario
 */
@Composable
private fun ChangeRoleDialog(
    user: UserWithRole,
    repository: AdminRepository,
    onDismiss: () -> Unit,
    onSuccess: () -> Unit
) {
    var selectedRole by remember { mutableStateOf(user.getUserRole()) }
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Cambiar Rol de Usuario") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Usuario: ${user.displayName}")
                Text("Email: ${user.email}")

                Divider(modifier = Modifier.padding(vertical = 8.dp))

                Text(
                    "Selecciona el nuevo rol:",
                    style = MaterialTheme.typography.labelLarge
                )

                UserRole.values().forEach { role ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedRole == role,
                            onClick = { selectedRole = role }
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(role.displayName)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    scope.launch {
                        isLoading = true
                        val result = repository.changeUserRole(user.uid, selectedRole)
                        result.onSuccess {
                            onSuccess()
                        }
                        isLoading = false
                    }
                },
                enabled = !isLoading && selectedRole != user.getUserRole()
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Guardar")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isLoading) {
                Text("Cancelar")
            }
        }
    )
}
