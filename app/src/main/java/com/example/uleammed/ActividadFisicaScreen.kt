package com.example.uleammed

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// ViewModel
class ActividadFisicaViewModel : ViewModel() {
    private val repository = AuthRepository()

    private val _state = MutableStateFlow<QuestionnaireState>(QuestionnaireState.Idle)
    val state: StateFlow<QuestionnaireState> = _state.asStateFlow()

    var frecuenciaEjercicio by mutableStateOf("")
    var duracionEjercicio by mutableStateOf("")
    var tipoActividadPrincipal by mutableStateOf("")
    var realizaEstiramientos by mutableStateOf("")
    var frecuenciaComidasDia by mutableStateOf("")
    var saltaDesayuno by mutableStateOf("")
    var comeEnEscritorio by mutableStateOf("")
    var consumoAguaDiario by mutableStateOf("")
    var consumoCafeTe by mutableStateOf("")
    var consumeBebidasEnergizantes by mutableStateOf("")

    fun isFormValid(): Boolean {
        return frecuenciaEjercicio.isNotEmpty() &&
                duracionEjercicio.isNotEmpty() &&
                tipoActividadPrincipal.isNotEmpty() &&
                realizaEstiramientos.isNotEmpty() &&
                frecuenciaComidasDia.isNotEmpty() &&
                saltaDesayuno.isNotEmpty() &&
                comeEnEscritorio.isNotEmpty() &&
                consumoAguaDiario.isNotEmpty() &&
                consumoCafeTe.isNotEmpty() &&
                consumeBebidasEnergizantes.isNotEmpty()
    }

    fun submitQuestionnaire() {
        viewModelScope.launch {
            if (!isFormValid()) {
                _state.value = QuestionnaireState.Error("Por favor completa todas las preguntas")
                return@launch
            }

            val userId = FirebaseAuth.getInstance().currentUser?.uid
            if (userId == null) {
                _state.value = QuestionnaireState.Error("Usuario no autenticado")
                return@launch
            }

            _state.value = QuestionnaireState.Loading

            val questionnaire = ActividadFisicaQuestionnaire(
                userId = userId,
                frecuenciaEjercicio = frecuenciaEjercicio,
                duracionEjercicio = duracionEjercicio,
                tipoActividadPrincipal = tipoActividadPrincipal,
                realizaEstiramientos = realizaEstiramientos,
                frecuenciaComidasDia = frecuenciaComidasDia,
                saltaDesayuno = saltaDesayuno,
                comeEnEscritorio = comeEnEscritorio,
                consumoAguaDiario = consumoAguaDiario,
                consumoCafeTe = consumoCafeTe,
                consumeBebidasEnergizantes = consumeBebidasEnergizantes
            )

            val result = repository.saveActividadFisicaQuestionnaire(questionnaire)
            result.onSuccess {
                _state.value = QuestionnaireState.Success
            }.onFailure { exception ->
                _state.value = QuestionnaireState.Error("Error al guardar: ${exception.message}")
            }
        }
    }

    fun resetState() {
        _state.value = QuestionnaireState.Idle
    }
}

// Screen
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActividadFisicaQuestionnaireScreen(
    onComplete: () -> Unit,
    onBack: () -> Unit,
    viewModel: ActividadFisicaViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    var showErrorDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    LaunchedEffect(state) {
        when (state) {
            is QuestionnaireState.Success -> {
                onComplete()
            }
            is QuestionnaireState.Error -> {
                errorMessage = (state as QuestionnaireState.Error).message
                showErrorDialog = true
                viewModel.resetState()
            }
            else -> {}
        }
    }

    if (showErrorDialog) {
        AlertDialog(
            onDismissRequest = { showErrorDialog = false },
            icon = { Icon(Icons.Filled.Error, contentDescription = null) },
            title = { Text("Error") },
            text = { Text(errorMessage) },
            confirmButton = {
                TextButton(onClick = { showErrorDialog = false }) {
                    Text("Entendido")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Actividad Física y Nutrición") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Evalúa tus hábitos de ejercicio, alimentación y consumo de sustancias. 10 preguntas, 4-5 minutos.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // EJERCICIO REGULAR
            SectionHeader("Ejercicio Regular")

            // 109. Frecuencia de ejercicio
            QuestionTitle("109. Frecuencia de ejercicio/actividad física moderada-intensa")
            SingleChoiceQuestion(
                options = listOf(
                    "Ninguna (sedentario)",
                    "1 vez por semana",
                    "2-3 veces por semana",
                    "4-5 veces por semana",
                    "Diariamente"
                ),
                selectedOption = viewModel.frecuenciaEjercicio,
                onOptionSelected = { viewModel.frecuenciaEjercicio = it }
            )

            // 110. Duración
            QuestionTitle("110. Duración típica del ejercicio")
            SingleChoiceQuestion(
                options = listOf(
                    "No hago ejercicio",
                    "Menos de 20 minutos",
                    "20-30 minutos",
                    "30-60 minutos",
                    "Más de 60 minutos"
                ),
                selectedOption = viewModel.duracionEjercicio,
                onOptionSelected = { viewModel.duracionEjercicio = it }
            )

            // 111. Tipo de actividad
            QuestionTitle("111. Tipo de actividad física principal")
            SingleChoiceQuestion(
                options = listOf(
                    "Caminar",
                    "Correr/Trotar",
                    "Gimnasio/Pesas",
                    "Deportes",
                    "Yoga/Pilates",
                    "Natación/Ciclismo",
                    "Ninguna"
                ),
                selectedOption = viewModel.tipoActividadPrincipal,
                onOptionSelected = { viewModel.tipoActividadPrincipal = it }
            )

            // 112. Estiramientos
            QuestionTitle("112. ¿Realizas estiramientos regularmente?")
            SingleChoiceQuestion(
                options = listOf(
                    "Sí, diariamente",
                    "Sí, 3-4 veces por semana",
                    "Ocasionalmente",
                    "Rara vez",
                    "Nunca"
                ),
                selectedOption = viewModel.realizaEstiramientos,
                onOptionSelected = { viewModel.realizaEstiramientos = it }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // HÁBITOS ALIMENTICIOS
            SectionHeader("Hábitos Alimenticios Laborales")

            // 113. Frecuencia de comidas
            QuestionTitle("113. Frecuencia de comidas al día")
            SingleChoiceQuestion(
                options = listOf(
                    "1-2 comidas",
                    "3 comidas",
                    "4-5 comidas (incluye snacks)",
                    "Irregular/Sin horario"
                ),
                selectedOption = viewModel.frecuenciaComidasDia,
                onOptionSelected = { viewModel.frecuenciaComidasDia = it }
            )

            // 114. Desayuno
            QuestionTitle("114. ¿Saltas el desayuno?")
            SingleChoiceQuestion(
                options = listOf("Nunca", "Ocasionalmente", "Frecuentemente", "Siempre"),
                selectedOption = viewModel.saltaDesayuno,
                onOptionSelected = { viewModel.saltaDesayuno = it }
            )

            // 115. Come en escritorio
            QuestionTitle("115. ¿Comes en tu escritorio mientras trabajas?")
            SingleChoiceQuestion(
                options = listOf("Siempre", "Frecuentemente", "A veces", "Rara vez", "Nunca"),
                selectedOption = viewModel.comeEnEscritorio,
                onOptionSelected = { viewModel.comeEnEscritorio = it }
            )

            // 116. Consumo de agua
            QuestionTitle("116. Consumo de agua diario")
            SingleChoiceQuestion(
                options = listOf(
                    "Menos de 1 litro",
                    "1-1.5 litros",
                    "1.5-2 litros",
                    "Más de 2 litros"
                ),
                selectedOption = viewModel.consumoAguaDiario,
                onOptionSelected = { viewModel.consumoAguaDiario = it }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // SUSTANCIAS ESTIMULANTES
            SectionHeader("Sustancias Estimulantes")

            // 117. Consumo de café/té
            QuestionTitle("117. Consumo de café/té")
            SingleChoiceQuestion(
                options = listOf(
                    "No consumo",
                    "1 taza al día",
                    "2-3 tazas al día",
                    "4-5 tazas al día",
                    "Más de 5 tazas al día"
                ),
                selectedOption = viewModel.consumoCafeTe,
                onOptionSelected = { viewModel.consumoCafeTe = it }
            )

            // 118. Bebidas energizantes
            QuestionTitle("118. ¿Consumes bebidas energizantes?")
            SingleChoiceQuestion(
                options = listOf(
                    "No",
                    "Ocasionalmente (1-2 por mes)",
                    "Regularmente (1-2 por semana)",
                    "Frecuentemente (3+ por semana)",
                    "Diariamente"
                ),
                selectedOption = viewModel.consumeBebidasEnergizantes,
                onOptionSelected = { viewModel.consumeBebidasEnergizantes = it }
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Botón enviar
            Button(
                onClick = { viewModel.submitQuestionnaire() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = state !is QuestionnaireState.Loading && viewModel.isFormValid()
            ) {
                if (state is QuestionnaireState.Loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Icon(
                        imageVector = Icons.Filled.Send,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Enviar Cuestionario")
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}