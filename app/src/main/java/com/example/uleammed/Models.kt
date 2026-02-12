package com.example.uleammed

data class User(
    val uid: String = "",
    val email: String = "",
    val displayName: String = "",
    val photoUrl: String = "",
    val hasCompletedQuestionnaire: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

data class HealthQuestionnaire(
    val userId: String = "",
    // Información básica
    val ageRange: String = "",
    val gender: String = "",
    val weight: Float = 0f,
    val height: Float = 0f,
    val bmi: Float = 0f,
    val bmiCategory: String = "",

    // Hábitos
    val smokingStatus: String = "",
    val alcoholConsumption: String = "",

    // Condiciones médicas
    val preexistingConditions: List<String> = emptyList(),
    val medications: List<String> = emptyList(),
    val recentSurgeries: Boolean = false,
    val surgeryDetails: String = "",
    val familyHistory: List<String> = emptyList(),

    // Estado general
    val energyLevel: String = "",
    val hadCovid: String = "",
    val covidSymptoms: List<String> = emptyList(),
    val generalHealthStatus: String = "",
    val annualCheckups: String = "",

    // Indicadores de salud
    val bloodPressure: String = "",
    val cholesterolLevel: String = "",
    val bloodGlucose: String = "",

    // Alergias y problemas laborales
    val allergies: List<String> = emptyList(),
    val workInterference: List<String> = emptyList(),

    val completedAt: Long = System.currentTimeMillis()
)

// Estados de autenticación
sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class Success(val user: User) : AuthState()
    data class Error(val message: String) : AuthState()
}

// Resultado de validación
data class AuthValidationResult(
    val isValid: Boolean,
    val errorMessage: String? = null
)