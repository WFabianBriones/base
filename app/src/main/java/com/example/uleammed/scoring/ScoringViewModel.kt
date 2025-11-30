package com.example.uleammed.scoring

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class ScoringState {
    object Idle : ScoringState()
    object Loading : ScoringState()
    data class Success(val score: HealthScore) : ScoringState()
    data class Error(val message: String) : ScoringState()
}

class ScoringViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ScoringRepository(application)

    private val _state = MutableStateFlow<ScoringState>(ScoringState.Idle)
    val state: StateFlow<ScoringState> = _state.asStateFlow()

    private val _healthScore = MutableStateFlow<HealthScore?>(null)
    val healthScore: StateFlow<HealthScore?> = _healthScore.asStateFlow()

    companion object {
        private const val TAG = "ScoringViewModel"
    }

    init {
        loadScore()
    }

    /**
     * Cargar score actual
     */
    fun loadScore() {
        viewModelScope.launch {
            try {
                _state.value = ScoringState.Loading

                val result = repository.getCurrentScore()

                result.onSuccess { score ->
                    _healthScore.value = score
                    _state.value = ScoringState.Success(score)

                    android.util.Log.d(TAG, """
                        ✅ Score cargado
                        - Overall: ${score.overallScore}
                        - Riesgo: ${score.overallRisk.displayName}
                    """.trimIndent())
                }.onFailure { exception ->
                    _state.value = ScoringState.Error(exception.message ?: "Error desconocido")
                    android.util.Log.e(TAG, "❌ Error cargando score", exception)
                }

            } catch (e: Exception) {
                _state.value = ScoringState.Error(e.message ?: "Error desconocido")
                android.util.Log.e(TAG, "❌ Error en loadScore", e)
            }
        }
    }

    /**
     * Recalcular todos los scores
     */
    fun recalculateScores() {
        viewModelScope.launch {
            try {
                _state.value = ScoringState.Loading

                val result = repository.calculateAllScores()

                result.onSuccess { score ->
                    _healthScore.value = score
                    _state.value = ScoringState.Success(score)

                    android.util.Log.d(TAG, "✅ Scores recalculados exitosamente")
                }.onFailure { exception ->
                    _state.value = ScoringState.Error(exception.message ?: "Error al recalcular")
                    android.util.Log.e(TAG, "❌ Error recalculando scores", exception)
                }

            } catch (e: Exception) {
                _state.value = ScoringState.Error(e.message ?: "Error desconocido")
                android.util.Log.e(TAG, "❌ Error en recalculateScores", e)
            }
        }
    }

    /**
     * Obtener mensaje de estado general
     */
    fun getOverallStatusMessage(score: HealthScore): String {
        return when (score.overallRisk) {
            RiskLevel.BAJO -> "Tu salud laboral está en buen estado. ¡Sigue así!"
            RiskLevel.MODERADO -> "Hay áreas que necesitan atención. Revisa las recomendaciones."
            RiskLevel.ALTO -> "Se detectaron varios factores de riesgo. Toma acción pronto."
            RiskLevel.MUY_ALTO -> "⚠️ Situación crítica detectada. Busca apoyo profesional."
        }
    }

    /**
     * Obtener color según nivel de riesgo
     */
    fun getRiskColor(risk: RiskLevel): Long {
        return risk.color
    }
}