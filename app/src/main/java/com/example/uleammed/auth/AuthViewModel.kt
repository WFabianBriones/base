package com.example.uleammed.auth

import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.uleammed.auth.AuthRepository
import com.example.uleammed.AuthState
import com.example.uleammed.User
import com.example.uleammed.AuthValidationResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AuthViewModel : ViewModel() {
    private val repository = AuthRepository()

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    init {
        checkCurrentUser()
    }

    fun checkCurrentUser() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val firebaseUser = repository.currentUser
                if (firebaseUser != null) {
                    val result = repository.getUserData(firebaseUser.uid)
                    result.onSuccess { user ->
                        withContext(Dispatchers.Main) {
                            _currentUser.value = user
                            _authState.value = AuthState.Success(user)
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _authState.value = AuthState.Error("Error al verificar usuario: ${e.message}")
                }
            }
        }
    }

    fun registerWithEmail(email: String, password: String, confirmPassword: String, displayName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            // Validación rápida en background
            val validation = validateRegistration(email, password, confirmPassword, displayName)
            if (!validation.isValid) {
                withContext(Dispatchers.Main) {
                    _authState.value =
                        AuthState.Error(validation.errorMessage ?: "Error de validación")
                }
                return@launch
            }

            withContext(Dispatchers.Main) {
                _authState.value = AuthState.Loading
            }

            val result = repository.registerWithEmail(email, password, displayName)

            withContext(Dispatchers.Main) {
                result.onSuccess { user ->
                    _currentUser.value = user
                    _authState.value = AuthState.Success(user)
                }.onFailure { exception ->
                    _authState.value = AuthState.Error(
                        when {
                            exception.message?.contains("email") == true -> "El correo ya está en uso"
                            exception.message?.contains("weak-password") == true -> "La contraseña es muy débil"
                            exception.message?.contains("network") == true -> "Error de conexión. Verifica tu internet"
                            else -> "Error al registrarse: ${exception.message}"
                        }
                    )
                }
            }
        }
    }

    fun loginWithEmail(email: String, password: String) {
        viewModelScope.launch(Dispatchers.IO) {
            // Validación rápida
            if (email.isBlank() || password.isBlank()) {
                withContext(Dispatchers.Main) {
                    _authState.value = AuthState.Error("Por favor completa todos los campos")
                }
                return@launch
            }

            withContext(Dispatchers.Main) {
                _authState.value = AuthState.Loading
            }

            val result = repository.loginWithEmail(email, password)

            withContext(Dispatchers.Main) {
                result.onSuccess { user ->
                    _currentUser.value = user
                    _authState.value = AuthState.Success(user)
                }.onFailure { exception ->
                    _authState.value = AuthState.Error(
                        when {
                            exception.message?.contains("user-not-found") == true -> "Usuario no encontrado"
                            exception.message?.contains("wrong-password") == true -> "Contraseña incorrecta"
                            exception.message?.contains("invalid-credential") == true -> "Credenciales inválidas"
                            exception.message?.contains("network") == true -> "Error de conexión. Verifica tu internet"
                            exception.message?.contains("too-many-requests") == true -> "Demasiados intentos. Intenta más tarde"
                            else -> "Error al iniciar sesión: ${exception.message}"
                        }
                    )
                }
            }
        }
    }

    fun signInWithGoogle(idToken: String) {
        viewModelScope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) {
                _authState.value = AuthState.Loading
            }

            val result = repository.signInWithGoogle(idToken)

            withContext(Dispatchers.Main) {
                result.onSuccess { user ->
                    _currentUser.value = user
                    _authState.value = AuthState.Success(user)
                }.onFailure { exception ->
                    _authState.value = AuthState.Error(
                        when {
                            exception.message?.contains("network") == true -> "Error de conexión. Verifica tu internet"
                            exception.message?.contains("cancelled") == true -> "Inicio de sesión cancelado"
                            else -> "Error al iniciar sesión con Google: ${exception.message}"
                        }
                    )
                }
            }
        }
    }

    fun signOut() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.signOut()
                withContext(Dispatchers.Main) {
                    _currentUser.value = null
                    _authState.value = AuthState.Idle
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _authState.value = AuthState.Error("Error al cerrar sesión: ${e.message}")
                }
            }
        }
    }

    fun resetAuthState() {
        _authState.value = AuthState.Idle
    }

    private fun validateRegistration(
        email: String,
        password: String,
        confirmPassword: String,
        displayName: String
    ): AuthValidationResult {
        return when {
            displayName.isBlank() -> AuthValidationResult(false, "Por favor ingresa tu nombre")
            displayName.length < 2 -> AuthValidationResult(
                false,
                "El nombre debe tener al menos 2 caracteres"
            )
            email.isBlank() -> AuthValidationResult(false, "Por favor ingresa tu correo")
            !Patterns.EMAIL_ADDRESS.matcher(email).matches() ->
                AuthValidationResult(false, "Correo electrónico inválido")
            !email.endsWith("@uleam.edu.ec") && !email.endsWith("@gmail.com") ->
                AuthValidationResult(false, "Usa un correo @uleam.edu.ec o @gmail.com")
            password.isBlank() -> AuthValidationResult(false, "Por favor ingresa tu contraseña")
            password.length < 6 -> AuthValidationResult(
                false,
                "La contraseña debe tener al menos 6 caracteres"
            )
            password.length > 50 -> AuthValidationResult(false, "La contraseña es demasiado larga")
            password != confirmPassword -> AuthValidationResult(false, "Las contraseñas no coinciden")
            else -> AuthValidationResult(true)
        }
    }

    // Función para limpiar recursos
    override fun onCleared() {
        super.onCleared()
        // Limpiar si es necesario
    }
}