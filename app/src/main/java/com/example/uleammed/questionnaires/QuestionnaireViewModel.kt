package com.example.uleammed.questionnaires

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.uleammed.AuthRepository
import com.example.uleammed.HealthQuestionnaire
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.pow

sealed class QuestionnaireState {
    object Idle : QuestionnaireState()
    object Loading : QuestionnaireState()
    object Success : QuestionnaireState()
    data class Error(val message: String) : QuestionnaireState()
}

class QuestionnaireViewModel : ViewModel() {
    private val repository = AuthRepository()

    private val _state = MutableStateFlow<QuestionnaireState>(QuestionnaireState.Idle)
    val state: StateFlow<QuestionnaireState> = _state.asStateFlow()

    // Información básica
    var ageRange by mutableStateOf("")
    var gender by mutableStateOf("")
    var weight by mutableStateOf("")
    var height by mutableStateOf("")
    var bmi by mutableStateOf("")
    var bmiCategory by mutableStateOf("")

    // Hábitos
    var smokingStatus by mutableStateOf("")
    var alcoholConsumption by mutableStateOf("")

    // Condiciones médicas
    var preexistingConditions by mutableStateOf<List<String>>(emptyList())
    var medications by mutableStateOf<List<String>>(emptyList())
    var recentSurgeries by mutableStateOf(false)
    var surgeryDetails by mutableStateOf("")
    var familyHistory by mutableStateOf<List<String>>(emptyList())

    // Estado general
    var energyLevel by mutableStateOf("")
    var hadCovid by mutableStateOf("")
    var covidSymptoms by mutableStateOf<List<String>>(emptyList())
    var generalHealthStatus by mutableStateOf("")
    var annualCheckups by mutableStateOf("")

    // Indicadores de salud
    var bloodPressure by mutableStateOf("")
    var cholesterolLevel by mutableStateOf("")
    var bloodGlucose by mutableStateOf("")

    // Alergias y problemas laborales
    var allergies by mutableStateOf<List<String>>(emptyList())
    var workInterference by mutableStateOf<List<String>>(emptyList())

    fun calculateBMI() {
        val weightVal = weight.toFloatOrNull()
        val heightVal = height.toFloatOrNull()

        if (weightVal != null && heightVal != null && heightVal > 0) {
            val heightInMeters = heightVal / 100
            val bmiValue = weightVal / (heightInMeters.pow(2))
            bmi = String.format("%.1f", bmiValue)

            bmiCategory = when {
                bmiValue < 18.5 -> "Bajo peso (<18.5)"
                bmiValue < 25.0 -> "Normal (18.5-24.9)"
                bmiValue < 30.0 -> "Sobrepeso (25-29.9)"
                else -> "Obesidad (30+)"
            }
        } else {
            bmi = ""
            bmiCategory = ""
        }
    }

    fun isFormValid(): Boolean {
        return ageRange.isNotEmpty() &&
                gender.isNotEmpty() &&
                weight.isNotEmpty() &&
                height.isNotEmpty() &&
                bmi.isNotEmpty() &&
                smokingStatus.isNotEmpty() &&
                alcoholConsumption.isNotEmpty() &&
                energyLevel.isNotEmpty() &&
                hadCovid.isNotEmpty() &&
                generalHealthStatus.isNotEmpty() &&
                annualCheckups.isNotEmpty() &&
                bloodPressure.isNotEmpty() &&
                cholesterolLevel.isNotEmpty() &&
                bloodGlucose.isNotEmpty()
    }

    fun submitQuestionnaire() {
        viewModelScope.launch {
            if (!isFormValid()) {
                _state.value = QuestionnaireState.Error("Por favor completa todas las preguntas obligatorias")
                return@launch
            }

            val userId = FirebaseAuth.getInstance().currentUser?.uid
            if (userId == null) {
                _state.value = QuestionnaireState.Error("Usuario no autenticado")
                return@launch
            }

            _state.value = QuestionnaireState.Loading

            val questionnaire = HealthQuestionnaire(
                userId = userId,
                ageRange = ageRange,
                gender = gender,
                weight = weight.toFloatOrNull() ?: 0f,
                height = height.toFloatOrNull() ?: 0f,
                bmi = bmi.toFloatOrNull() ?: 0f,
                bmiCategory = bmiCategory,
                smokingStatus = smokingStatus,
                alcoholConsumption = alcoholConsumption,
                preexistingConditions = preexistingConditions,
                medications = medications,
                recentSurgeries = recentSurgeries,
                surgeryDetails = surgeryDetails,
                familyHistory = familyHistory,
                energyLevel = energyLevel,
                hadCovid = hadCovid,
                covidSymptoms = covidSymptoms,
                generalHealthStatus = generalHealthStatus,
                annualCheckups = annualCheckups,
                bloodPressure = bloodPressure,
                cholesterolLevel = cholesterolLevel,
                bloodGlucose = bloodGlucose,
                allergies = allergies,
                workInterference = workInterference
            )

            val result = repository.saveQuestionnaire(questionnaire)
            result.onSuccess {
                _state.value = QuestionnaireState.Success
            }.onFailure { exception ->
                _state.value = QuestionnaireState.Error("Error al guardar: ${exception.message}")
            }
        }
    }

    fun resetState() {
        _state.value = QuestionnaireState.Idle
    }
}