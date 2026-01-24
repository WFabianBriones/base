package com.example.uleammed.burnoutprediction.presentation.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.uleammed.burnoutprediction.presentation.viewmodel.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BurnoutAnalysisScreen(
    viewModel: BurnoutAnalysisViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Análisis de Burnout") }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            when (val state = uiState) {
                is BurnoutUiState.Idle -> {
                    Text("Esperando análisis...")
                }

                is BurnoutUiState.Loading -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Analizando con IA...")
                    }
                }

                is BurnoutUiState.Success -> {
                    ResultView(prediction = state.prediction)
                }

                is BurnoutUiState.Error -> {
                    Text(
                        text = "Error: ${state.message}",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
private fun ResultView(prediction: com.example.uleammed.burnoutprediction.model.BurnoutPrediction) {
    Column(
        modifier = Modifier.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = prediction.nivelRiesgo.displayName,
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Confianza: ${(prediction.confianza * 100).toInt()}%",
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text("Probabilidades:")
        Text("Bajo: ${(prediction.probabilidadBajo * 100).toInt()}%")
        Text("Medio: ${(prediction.probabilidadMedio * 100).toInt()}%")
        Text("Alto: ${(prediction.probabilidadAlto * 100).toInt()}%")
    }
}