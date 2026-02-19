package com.example.uleammed.tendencias

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.abs

class TendenciasViewModel(
    private val repository: TendenciasRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TendenciasUiState())
    val uiState: StateFlow<TendenciasUiState> = _uiState.asStateFlow()

    init {
        loadTendencias()
    }

    fun loadTendencias() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            repository.getLast4PeriodSnapshots()
                .onSuccess { (period, snapshots) ->
                    _uiState.update {
                        it.copy(
                            snapshots      = snapshots,
                            period         = period,
                            trendDirection = calculateTrendDirection(snapshots),
                            isLoading      = false
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error     = error.message ?: "Error al cargar tendencias"
                        )
                    }
                }
        }
    }

    fun toggleIndex(index: HealthIndex) {
        _uiState.update { state ->
            val current = state.selectedIndices.toMutableSet()
            if (index in current && current.size > 1) current.remove(index)
            else current.add(index)
            state.copy(selectedIndices = current)
        }
    }

    private fun calculateTrendDirection(
        snapshots: List<PeriodHealthSnapshot>
    ): TrendDirection {
        if (snapshots.size < 2) return TrendDirection.STABLE
        val diff = snapshots.last().globalScore - snapshots.first().globalScore
        return when {
            diff >  TREND_THRESHOLD -> TrendDirection.IMPROVING
            diff < -TREND_THRESHOLD -> TrendDirection.DECLINING
            else                    -> TrendDirection.STABLE
        }
    }

    companion object {
        private const val TREND_THRESHOLD = 5f
    }
}

class TendenciasViewModelFactory : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TendenciasViewModel::class.java)) {
            return TendenciasViewModel(TendenciasRepository()) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
