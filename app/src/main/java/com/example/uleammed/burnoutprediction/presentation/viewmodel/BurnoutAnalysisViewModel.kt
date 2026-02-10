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

    /**
     * Método original que acepta QuestionnaireData directamente
     */
    fun analyzeBurnout(data: QuestionnaireData) {
        viewModelScope.launch {
            try {
                _uiState.value = BurnoutUiState.Loading

                val prediction = model.predict(data)

                _uiState.value = BurnoutUiState.Success(prediction)
            } catch (e: Exception) {
                android.util.Log.e("BurnoutViewModel", "Error en análisis", e)
                _uiState.value = BurnoutUiState.Error(
                    e.message ?: "Error desconocido en el análisis"
                )
            }
        }
    }

    /**
     * Método sobrecargado que acepta Map<String, Float> desde el dashboard
     * ORDEN CRÍTICO: Debe coincidir con el orden de entrenamiento de la red neuronal
     */
    fun analyzeBurnout(indices: Map<String, Float>) {
        viewModelScope.launch {
            try {
                _uiState.value = BurnoutUiState.Loading

                // Extraer valores en el ORDEN EXACTO esperado por la red neuronal
                // Este orden debe coincidir con el LinkedHashMap creado en BurnoutAIAnalysisCard
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

                android.util.Log.d("BurnoutViewModel", """
                    📊 Datos recibidos para análisis de burnout:
                    1. Estrés: $estresIndex
                    2. Ergonomía: $ergonomiaIndex
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

                android.util.Log.d("BurnoutViewModel", """
                    ✅ Predicción completada:
                    - Nivel de riesgo: ${prediction.nivelRiesgo.displayName}
                    - Probabilidad bajo: ${(prediction.probabilidadBajo * 100).toInt()}%
                    - Probabilidad medio: ${(prediction.probabilidadMedio * 100).toInt()}%
                    - Probabilidad alto: ${(prediction.probabilidadAlto * 100).toInt()}%
                    - Confianza: ${(prediction.confianza * 100).toInt()}%
                """.trimIndent())

                _uiState.value = BurnoutUiState.Success(prediction)

            } catch (e: IllegalArgumentException) {
                // Error de validación de datos
                android.util.Log.e("BurnoutViewModel", "Error de validación: ${e.message}", e)
                _uiState.value = BurnoutUiState.Error(
                    "Datos incompletos para el análisis: ${e.message}"
                )
            } catch (e: Exception) {
                // Error general
                android.util.Log.e("BurnoutViewModel", "Error en análisis de burnout", e)
                _uiState.value = BurnoutUiState.Error(
                    e.message ?: "Error desconocido en el análisis"
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