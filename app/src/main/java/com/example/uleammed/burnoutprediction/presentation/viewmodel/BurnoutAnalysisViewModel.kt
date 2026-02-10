package com.example.uleammed.burnoutprediction.presentation.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.uleammed.burnoutprediction.model.*
import com.example.uleammed.scoring.HealthScore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ✅ VIEWMODEL MEJORADO PARA ANÁLISIS DE BURNOUT
 *
 * Cambios principales:
 * 1. ✅ Soporte para predicción desde HealthScore completo
 * 2. ✅ Integración de patrones críticos
 * 3. ✅ Estados mejorados con más información
 * 4. ✅ Manejo de errores más robusto
 */
class BurnoutAnalysisViewModel(context: Context) : ViewModel() {

    private val model = BurnoutPredictionModel(context)

    private val _uiState = MutableStateFlow<BurnoutUiState>(BurnoutUiState.Idle)
    val uiState: StateFlow<BurnoutUiState> = _uiState.asStateFlow()

    /**
     * ✅ NUEVO: Método principal recomendado usando HealthScore completo
     */
    fun analyzeBurnoutFromHealthScore(healthScore: HealthScore) {
        viewModelScope.launch {
            try {
                _uiState.value = BurnoutUiState.Loading

                android.util.Log.d(TAG, """
                    🔍 Iniciando análisis mejorado de burnout:
                    - Versión scoring: ${healthScore.version}
                    - Patrones críticos: ${healthScore.criticalPatterns.size}
                    - Overall score: ${healthScore.overallScore}
                """.trimIndent())

                // Ejecutar predicción mejorada
                val prediction = model.predictFromHealthScore(healthScore)

                android.util.Log.d(TAG, """
                    ✅ Predicción completada:
                    - Nivel de riesgo: ${prediction.nivelRiesgo.displayName}
                    - Probabilidad bajo: ${(prediction.probabilidadBajo * 100).toInt()}%
                    - Probabilidad medio: ${(prediction.probabilidadMedio * 100).toInt()}%
                    - Probabilidad alto: ${(prediction.probabilidadAlto * 100).toInt()}%
                    - Confianza: ${(prediction.confianza * 100).toInt()}%
                    - Patrones críticos: ${prediction.criticalPatterns.size}
                    - Requiere atención urgente: ${prediction.requiresUrgentAttention}
                    - Factores de riesgo: ${prediction.factoresRiesgo.size}
                    - Recomendaciones: ${prediction.recomendaciones.size}
                """.trimIndent())

                _uiState.value = BurnoutUiState.EnhancedSuccess(prediction)

            } catch (e: IllegalArgumentException) {
                android.util.Log.e(TAG, "Error de validación: ${e.message}", e)
                _uiState.value = BurnoutUiState.Error(
                    "Datos incompletos para el análisis: ${e.message}"
                )
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Error en análisis de burnout", e)
                _uiState.value = BurnoutUiState.Error(
                    e.message ?: "Error desconocido en el análisis"
                )
            }
        }
    }

    /**
     * Método original que acepta QuestionnaireData directamente
     * ⚠️ DEPRECADO: Usar analyzeBurnoutFromHealthScore() cuando sea posible
     */
    @Deprecated(
        message = "Usar analyzeBurnoutFromHealthScore() para aprovechar mejoras del scoring",
        replaceWith = ReplaceWith("analyzeBurnoutFromHealthScore(healthScore)")
    )
    fun analyzeBurnout(data: QuestionnaireData) {
        viewModelScope.launch {
            try {
                _uiState.value = BurnoutUiState.Loading

                val prediction = model.predict(data)

                _uiState.value = BurnoutUiState.Success(prediction)
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Error en análisis", e)
                _uiState.value = BurnoutUiState.Error(
                    e.message ?: "Error desconocido en el análisis"
                )
            }
        }
    }

    /**
     * ✅ MEJORADO: Método sobrecargado que acepta Map<String, Float> desde el dashboard
     * Ahora con mejor logging y validación
     */
    fun analyzeBurnout(indices: Map<String, Float>) {
        viewModelScope.launch {
            try {
                _uiState.value = BurnoutUiState.Loading

                // Extraer valores en el ORDEN EXACTO esperado por la red neuronal
                val estresIndex = indices["estres_index"]
                    ?: throw IllegalArgumentException("Falta índice de estrés")
                val ergonomiaIndex = indices["ergonomia_index"]
                    ?: throw IllegalArgumentException("Falta índice de ergonomía")
                val cargaTrabajoIndex = indices["carga_trabajo_index"]
                    ?: throw IllegalArgumentException("Falta índice de carga de trabajo")
                val calidadSuenoIndex = indices["calidad_sueno_index"]
                    ?: throw IllegalArgumentException("Falta índice de calidad de sueño")
                val actividadFisicaIndex = indices["actividad_fisica_index"]
                    ?: throw IllegalArgumentException("Falta índice de actividad física")
                val sintomasMuscularesIndex = indices["sintomas_musculares_index"]
                    ?: throw IllegalArgumentException("Falta índice de síntomas musculares")
                val sintomasVisualesIndex = indices["sintomas_visuales_index"]
                    ?: throw IllegalArgumentException("Falta índice de síntomas visuales")
                val saludGeneralIndex = indices["salud_general_index"]
                    ?: throw IllegalArgumentException("Falta índice de salud general")

                android.util.Log.d(TAG, """
                    📊 Índices recibidos para predicción:
                    1. Estrés: $estresIndex
                    2. Ergonomía: $ergonomiaIndex (ya invertido)
                    3. Carga Trabajo: $cargaTrabajoIndex
                    4. Calidad Sueño: $calidadSuenoIndex
                    5. Actividad Física: $actividadFisicaIndex
                    6. Síntomas Musculares: $sintomasMuscularesIndex
                    7. Síntomas Visuales: $sintomasVisualesIndex
                    8. Salud General: $saludGeneralIndex
                """.trimIndent())

                // Construir objeto QuestionnaireData
                val data = QuestionnaireData(
                    estresIndex = estresIndex,
                    ergonomiaIndex = ergonomiaIndex,
                    cargaTrabajoIndex = cargaTrabajoIndex,
                    calidadSuenoIndex = calidadSuenoIndex,
                    actividadFisicaIndex = actividadFisicaIndex,
                    sintomasMuscularesIndex = sintomasMuscularesIndex,
                    sintomasVisualesIndex = sintomasVisualesIndex,
                    saludGeneralIndex = saludGeneralIndex
                )

                // Ejecutar predicción
                val prediction = model.predict(data)

                android.util.Log.d(TAG, """
                    ✅ Predicción completada:
                    - Nivel de riesgo: ${prediction.nivelRiesgo.displayName}
                    - Probabilidad bajo: ${(prediction.probabilidadBajo * 100).toInt()}%
                    - Probabilidad medio: ${(prediction.probabilidadMedio * 100).toInt()}%
                    - Probabilidad alto: ${(prediction.probabilidadAlto * 100).toInt()}%
                    - Confianza: ${(prediction.confianza * 100).toInt()}%
                """.trimIndent())

                _uiState.value = BurnoutUiState.Success(prediction)

            } catch (e: IllegalArgumentException) {
                android.util.Log.e(TAG, "Error de validación: ${e.message}", e)
                _uiState.value = BurnoutUiState.Error(
                    "Datos incompletos para el análisis: ${e.message}"
                )
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Error en análisis de burnout", e)
                _uiState.value = BurnoutUiState.Error(
                    e.message ?: "Error desconocido en el análisis"
                )
            }
        }
    }

    /**
     * ✅ NUEVO: Reiniciar estado
     */
    fun resetState() {
        _uiState.value = BurnoutUiState.Idle
    }

    override fun onCleared() {
        super.onCleared()
        model.close()
        android.util.Log.d(TAG, "ViewModel cleared, modelo cerrado")
    }

    companion object {
        private const val TAG = "BurnoutViewModel"
    }
}

/**
 * ✅ MEJORADO: Estados de UI con más información
 */
sealed class BurnoutUiState {
    object Idle : BurnoutUiState()
    object Loading : BurnoutUiState()

    data class Success(val prediction: BurnoutPrediction) : BurnoutUiState()

    /**
     * ✅ NUEVO: Estado para predicción mejorada
     */
    data class EnhancedSuccess(val prediction: EnhancedBurnoutPrediction) : BurnoutUiState()

    data class Error(val message: String) : BurnoutUiState()
}