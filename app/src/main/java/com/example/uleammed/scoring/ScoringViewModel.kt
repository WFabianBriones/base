package com.example.uleammed.scoring

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * ✅ VIEWMODEL CORREGIDO - Gestión de estados y recálculo inteligente
 */

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

    // ✅ Mutex para prevenir cálculos concurrentes
    private val calculationMutex = Mutex()

    companion object {
        private const val TAG = "ScoringViewModel"
        // Intervalo mínimo para forzar un recálculo (5 minutos)
        private const val MIN_RECALC_INTERVAL = 5 * 60 * 1000L
    }

    init {
        // Al iniciar, cargamos el score con el refresco inteligente
        loadScoreWithSmartRefresh()
    }

    /**
     * ✅ NUEVA FUNCIÓN: Cargar score con control de recálculo inteligente
     * Si ha pasado más de MIN_RECALC_INTERVAL, fuerza el recálculo
     */
    fun loadScoreWithSmartRefresh() {
        viewModelScope.launch {
            try {
                _state.value = ScoringState.Loading

                val lastCalcTime = repository.getLastCalculationTime()
                val currentTime = System.currentTimeMillis()
                val timeSinceLastCalc = currentTime - lastCalcTime

                android.util.Log.d(TAG, """
                    ⏰ Tiempo desde último cálculo: ${timeSinceLastCalc / 1000}s
                    - Última vez: $lastCalcTime
                    - Ahora: $currentTime
                """.trimIndent())

                // Si nunca se ha calculado o ha pasado el intervalo mínimo, forzar recálculo
                if (lastCalcTime == 0L || timeSinceLastCalc >= MIN_RECALC_INTERVAL) {
                    android.util.Log.d(TAG, "🔄 Forzando recálculo de scores...")
                    forceRecalculate()
                } else {
                    // Cargar desde caché/Firestore
                    android.util.Log.d(TAG, "📦 Cargando score desde caché...")
                    loadScore()
                }

            } catch (e: Exception) {
                _state.value = ScoringState.Error(e.message ?: "Error desconocido")
                android.util.Log.e(TAG, "❌ Error en loadScoreWithSmartRefresh", e)
            }
        }
    }

    /**
     * Cargar score actual (usa la versión guardada en caché/DB)
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
     * ✅ MEJORADO: Forzar recálculo con protección contra concurrencia
     */
    fun forceRecalculate() {
        viewModelScope.launch {
            // ✅ Usar mutex para prevenir múltiples cálculos simultáneos
            calculationMutex.withLock {
                try {
                    _state.value = ScoringState.Loading
                    android.util.Log.d(TAG, "🔄 Iniciando recálculo forzado...")

                    val result = repository.calculateAllScores()

                    result.onSuccess { score ->
                        _healthScore.value = score
                        _state.value = ScoringState.Success(score)

                        android.util.Log.d(TAG, """
                            ✅ Recálculo completado
                            - Overall: ${score.overallScore}
                            - Riesgo: ${score.overallRisk.displayName}
                            - Top concerns: ${score.topConcerns.size}
                        """.trimIndent())
                    }.onFailure { exception ->
                        _state.value = ScoringState.Error(
                            exception.message ?: "Error al calcular scores"
                        )
                        android.util.Log.e(TAG, "❌ Error en recálculo", exception)
                    }

                } catch (e: Exception) {
                    _state.value = ScoringState.Error(e.message ?: "Error desconocido")
                    android.util.Log.e(TAG, "❌ Error en forceRecalculate", e)
                }
            }
        }
    }

    /**
     * ✅ NUEVA: Obtener tendencia histórica
     */
    fun loadScoreTrend(days: Int = 30) {
        viewModelScope.launch {
            try {
                val userId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
                if (userId != null) {
                    val result = repository.getScoreTrend(userId, days)
                    result.onSuccess { trend ->
                        android.util.Log.d(TAG, "✅ Tendencia cargada: ${trend.size} registros")
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e(TAG, "❌ Error cargando tendencia", e)
            }
        }
    }

    /**
     * Limpiar scores (para testing)
     */
    fun clearScores() {
        repository.clearScores()
        _healthScore.value = null
        _state.value = ScoringState.Idle
        android.util.Log.d(TAG, "🗑️ Scores limpiados")
    }

    /**
     * ✅ HELPERS PARA LA UI
     */

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

    /**
     * Obtener emoji según nivel de riesgo
     */
    fun getRiskEmoji(risk: RiskLevel): String {
        return when (risk) {
            RiskLevel.BAJO -> "✅"
            RiskLevel.MODERADO -> "⚠️"
            RiskLevel.ALTO -> "🚨"
            RiskLevel.MUY_ALTO -> "🆘"
        }
    }

    /**
     * Verificar si el score es reciente (menos de 1 día)
     */
    fun isScoreRecent(score: HealthScore): Boolean {
        val oneDayInMillis = 24 * 60 * 60 * 1000L
        val currentTime = System.currentTimeMillis()
        return (currentTime - score.timestamp) < oneDayInMillis
    }

    /**
     * Obtener tiempo transcurrido desde el último cálculo
     */
    fun getTimeSinceLastCalculation(): String {
        val lastCalcTime = repository.getLastCalculationTime()
        if (lastCalcTime == 0L) return "Nunca calculado"

        val diff = System.currentTimeMillis() - lastCalcTime
        val minutes = diff / (60 * 1000)
        val hours = minutes / 60
        val days = hours / 24

        return when {
            days > 0 -> "Hace $days día${if (days > 1) "s" else ""}"
            hours > 0 -> "Hace $hours hora${if (hours > 1) "s" else ""}"
            minutes > 0 -> "Hace $minutes minuto${if (minutes > 1) "s" else ""}"
            else -> "Hace menos de 1 minuto"
        }
    }
}