package com.example.uleammed.burnoutprediction.model

data class QuestionnaireData(
    val estresIndex: Float,
    val ergonomiaIndex: Float,
    val cargaTrabajoIndex: Float,
    val calidadSuenoIndex: Float,
    val actividadFisicaIndex: Float,
    val sintomasMuscularesIndex: Float,
    val sintomasVisualesIndex: Float,
    val saludGeneralIndex: Float
) {
    init {
        require(listOf(
            estresIndex, ergonomiaIndex, cargaTrabajoIndex,
            calidadSuenoIndex, actividadFisicaIndex,
            sintomasMuscularesIndex, sintomasVisualesIndex,
            saludGeneralIndex
        ).all { it in 0f..10f }) {
            "Todos los índices deben estar entre 0 y 10"
        }
    }
}