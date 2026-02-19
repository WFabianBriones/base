package com.example.uleammed.burnoutprediction.presentation.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.uleammed.burnoutprediction.model.*
import com.example.uleammed.tendencias.HealthSnapshot
import com.example.uleammed.tendencias.TendenciasRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class BurnoutAnalysisViewModel(context: Context) : ViewModel() {

    private val model                = BurnoutPredictionModel(context)
    private val tendenciasRepository = TendenciasRepository()   // ← NUEVO

    private val _uiState = MutableStateFlow<BurnoutUiState>(BurnoutUiState.Idle)
    val uiState: StateFlow<BurnoutUiState> = _uiState.asStateFlow()

    fun analyzeBurnout(data: QuestionnaireData) {
        viewModelScope.launch {
            try {
                _uiState.value = BurnoutUiState.Loading

                val prediction = model.predict(data)

                // Guardar snapshot para tendencias (silencioso, no bloquea UI)
                saveHealthSnapshot(data, prediction)

                _uiState.value = BurnoutUiState.Success(prediction)
            } catch (e: Exception) {
                _uiState.value = BurnoutUiState.Error(e.message ?: "Error desconocido")
            }
        }
    }

    private fun saveHealthSnapshot(data: QuestionnaireData, prediction: BurnoutPrediction) {
        viewModelScope.launch {
            val snapshot = HealthSnapshot(
                timestamp                 = System.currentTimeMillis(),
                estresIndex               = data.estresIndex,
                ergonomiaIndex            = data.ergonomiaIndex,
                cargaTrabajoIndex         = data.cargaTrabajoIndex,
                calidadSuenoIndex         = data.calidadSuenoIndex,
                actividadFisicaIndex      = data.actividadFisicaIndex,
                sintomasMuscularesIndex   = data.sintomasMuscularesIndex,
                sintomasVisualesIndex     = data.sintomasVisualesIndex,
                saludGeneralIndex         = data.saludGeneralIndex,
                nivelRiesgo               = prediction.nivelRiesgo.name,
                confianza                 = prediction.confianza
            )
            tendenciasRepository.saveSnapshot(snapshot)
                .onFailure { e ->
                    android.util.Log.w(
                        "BurnoutViewModel",
                        "⚠️ No se pudo guardar snapshot: ${e.message}"
                    )
                }
        }
    }

    override fun onCleared() {
        super.onCleared()
        model.close()
    }
}

sealed class BurnoutUiState {
    object Idle : BurnoutUiState()
    object Loading : BurnoutUiState()
    data class Success(val prediction: BurnoutPrediction) : BurnoutUiState()
    data class Error(val message: String) : BurnoutUiState()
}
