package com.example.uleammed.burnoutprediction.presentation.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.uleammed.burnoutprediction.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class BurnoutAnalysisViewModel(context: Context) : ViewModel() {

    private val model = BurnoutPredictionModel(context)

    private val _uiState = MutableStateFlow<BurnoutUiState>(BurnoutUiState.Idle)
    val uiState: StateFlow<BurnoutUiState> = _uiState.asStateFlow()

    fun analyzeBurnout(data: QuestionnaireData) {
        viewModelScope.launch {
            try {
                _uiState.value = BurnoutUiState.Loading

                val prediction = model.predict(data)

                _uiState.value = BurnoutUiState.Success(prediction)
            } catch (e: Exception) {
                _uiState.value = BurnoutUiState.Error(
                    e.message ?: "Error desconocido"
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