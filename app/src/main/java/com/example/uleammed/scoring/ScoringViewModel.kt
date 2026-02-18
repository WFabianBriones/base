package com.example.uleammed.scoring

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// Asumimos que HealthScore, RiskLevel y ScoringRepository están definidos en el proyecto.

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
        // Intervalo mínimo para forzar un recálculo (5 minutos)
        private const val MIN_RECALC_INTERVAL = 5 * 60 * 1000L
    }

    // Bandera para prevenir cálculos concurrentes.
    private var isCalculating = false

    init {
        // Al iniciar, cargamos el score con el refresco inteligente.
        loadScoreWithSmartRefresh()
    }

    /**
     * Cargar score actual (usa la versión guardada en caché/DB).
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
     * Cargar score con control de recálculo inteligente.
     * Si ha pasado más de [MIN_RECALC_INTERVAL], fuerza el recálculo.
     */
    fun loadScoreWithSmartRefresh() {
        viewModelScope.launch {
            try {
                // No actualizamos _state.value a Loading si ya estamos calculando/cargando
                if (_state.value != ScoringState.Loading) {
                    _state.value = ScoringState.Loading
                }

                // Verificar si necesita recalcular
                val lastCalculation = repository.getLastCalculationTime()
                val now = System.currentTimeMillis()
                val timeSinceLastCalc = now - lastCalculation

                // Convertir a minutos para el log
                val timeSinceLastCalcInMinutes = timeSinceLastCalc / 60000

                if (timeSinceLastCalc > MIN_RECALC_INTERVAL) {
                    android.util.Log.d(TAG, "🔄 Han pasado ${timeSinceLastCalcInMinutes} minutos, recalculando...")
                    recalculateScores()
                } else {
                    android.util.Log.d(TAG, "✅ Usando caché (última actualización hace ${timeSinceLastCalcInMinutes} min)")
                    loadScore()
                }
            } catch (e: Exception) {
                _state.value = ScoringState.Error(e.message ?: "Error desconocido")
                android.util.Log.e(TAG, "❌ Error en loadScoreWithSmartRefresh", e)
            }
        }
    }


    /**
     * Recalcular todos los scores.
     * Modificado para prevenir llamadas concurrentes.
     */
    fun recalculateScores() {
        // Prevenir cálculos concurrentes
        if (isCalculating) {
            android.util.Log.d(TAG, "⏳ Cálculo ya en progreso, ignorando...")
            return
        }

        viewModelScope.launch {
            try {
                isCalculating = true
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
            } finally {
                // Asegurarse de resetear la bandera al finalizar
                isCalculating = false
            }
        }
    }

    /**
     * Obtener timestamp de última actualización desde el repositorio.
     */
    fun getLastCalculationTime(): Long {
        return repository.getLastCalculationTime()
    }

    /**
     * Obtener tendencia de scores para un número de días.
     * Utiliza un callback [onResult] para devolver la lista de HealthScore.
     */
    fun getScoreTrend(days: Int = 30, onResult: (Result<List<HealthScore>>) -> Unit) {
        viewModelScope.launch {
            try {
                // Se necesita un userId, si no existe no se puede buscar la tendencia.
                val userId = _healthScore.value?.userId ?: run {
                    android.util.Log.w(TAG, "Advertencia: userId no disponible para buscar tendencia.")
                    return@launch
                }
                val result = repository.getScoreTrend(userId, days)
                onResult(result)
            } catch (e: Exception) {
                android.util.Log.e(TAG, "❌ Error obteniendo tendencia", e)
                onResult(Result.failure(e))
            }
        }
    }

    /**
     * Verificar si hay datos suficientes para mostrar dashboard completo.
     * Asume que se requiere completar al menos 3 áreas de score (donde el score sea > 0).
     */
    fun hasMinimumDataForDashboard(): Boolean {
        val score = _healthScore.value ?: return false

        // Lista de scores de áreas para verificar si se completaron (score > 0)
        val completedAreas = listOf(
            score.saludGeneralScore,
            score.ergonomiaScore,
            score.sintomasMuscularesScore,
            score.sintomasVisualesScore,
            score.cargaTrabajoScore,
            score.estresSaludMentalScore,
            score.habitosSuenoScore,
            score.actividadFisicaScore,
            score.balanceVidaTrabajoScore
        ).count { it > -1 }

        return completedAreas >= 3
    }

    /**
     * Obtener mensaje de estado general basado en el nivel de riesgo.
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
     * Obtener color según nivel de riesgo.
     * Se asume que RiskLevel tiene una propiedad 'color' de tipo Long.
     */
    fun getRiskColor(risk: RiskLevel): Long {
        return risk.color
    }
}