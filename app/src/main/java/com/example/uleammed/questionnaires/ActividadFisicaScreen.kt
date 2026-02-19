package com.example.uleammed.questionnaires

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.uleammed.auth.AuthRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// ViewModel — sin cambios de lógica
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
            result.onSuccess { _state.value = QuestionnaireState.Success }
                .onFailure { exception ->
                    _state.value = QuestionnaireState.Error("Error al guardar: ${exception.message}")
                }
        }
    }

    fun resetState() { _state.value = QuestionnaireState.Idle }
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
            is QuestionnaireState.Success -> onComplete()
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
                TextButton(onClick = { showErrorDialog = false }) { Text("Entendido") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ejercicio y alimentación") },
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
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Info, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Cuéntanos sobre tu actividad física, lo que comes y lo que bebes durante tu jornada. 10 preguntas, 4-5 minutos.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── Ejercicio ────────────────────────────────────────────────
            SectionHeader("¿Cuánto te mueves?")

            QuestionTitle("¿Con qué frecuencia haces ejercicio o actividad física intensa?")
            SingleChoiceQuestion(
                options = listOf(
                    "No hago ninguna actividad física",
                    "Una vez a la semana",
                    "2 o 3 veces a la semana",
                    "4 o 5 veces a la semana",
                    "Todos los días"
                ),
                selectedOption = viewModel.frecuenciaEjercicio,
                onOptionSelected = { viewModel.frecuenciaEjercicio = it }
            )

            QuestionTitle("Cuando haces ejercicio, ¿cuánto tiempo dura normalmente?")
            SingleChoiceQuestion(
                options = listOf(
                    "No hago ejercicio",
                    "Menos de 20 minutos",
                    "Entre 20 y 30 minutos",
                    "Entre 30 y 60 minutos",
                    "Más de 60 minutos"
                ),
                selectedOption = viewModel.duracionEjercicio,
                onOptionSelected = { viewModel.duracionEjercicio = it }
            )

            QuestionTitle("¿Cuál es la actividad física que más haces?")
            SingleChoiceQuestion(
                options = listOf(
                    "Caminar",
                    "Correr o trotar",
                    "Gimnasio o pesas",
                    "Deportes de equipo",
                    "Yoga o pilates",
                    "Natación o ciclismo",
                    "No hago ninguna"
                ),
                selectedOption = viewModel.tipoActividadPrincipal,
                onOptionSelected = { viewModel.tipoActividadPrincipal = it }
            )

            QuestionTitle("¿Con qué frecuencia haces estiramientos?")
            SingleChoiceQuestion(
                options = listOf(
                    "Sí, todos los días",
                    "Sí, 3 o 4 veces a la semana",
                    "De vez en cuando",
                    "Rara vez",
                    "Nunca"
                ),
                selectedOption = viewModel.realizaEstiramientos,
                onOptionSelected = { viewModel.realizaEstiramientos = it }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ── Alimentación ─────────────────────────────────────────────
            SectionHeader("¿Cómo comes durante la jornada?")

            QuestionTitle("¿Cuántas veces al día comes?")
            SingleChoiceQuestion(
                options = listOf(
                    "1 o 2 veces",
                    "3 veces",
                    "4 o 5 veces (incluyendo snacks)",
                    "No tengo un horario fijo"
                ),
                selectedOption = viewModel.frecuenciaComidasDia,
                onOptionSelected = { viewModel.frecuenciaComidasDia = it }
            )

            QuestionTitle("¿Te saltas el desayuno?")
            SingleChoiceQuestion(
                options = listOf("Nunca", "A veces", "Con frecuencia", "Siempre"),
                selectedOption = viewModel.saltaDesayuno,
                onOptionSelected = { viewModel.saltaDesayuno = it }
            )

            QuestionTitle("¿Comes en tu escritorio mientras trabajas, sin hacer una pausa real?")
            SingleChoiceQuestion(
                options = listOf("Siempre", "Con frecuencia", "A veces", "Rara vez", "Nunca"),
                selectedOption = viewModel.comeEnEscritorio,
                onOptionSelected = { viewModel.comeEnEscritorio = it }
            )

            QuestionTitle("¿Cuánta agua bebes al día?")
            SingleChoiceQuestion(
                options = listOf(
                    "Menos de 1 litro",
                    "Entre 1 y 1.5 litros",
                    "Entre 1.5 y 2 litros",
                    "Más de 2 litros"
                ),
                selectedOption = viewModel.consumoAguaDiario,
                onOptionSelected = { viewModel.consumoAguaDiario = it }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ── Estimulantes ─────────────────────────────────────────────
            SectionHeader("¿Tomas café, té u otras bebidas estimulantes?")

            QuestionTitle("¿Cuántas tazas de café o té tomas al día?")
            SingleChoiceQuestion(
                options = listOf(
                    "No tomo",
                    "1 taza al día",
                    "2 o 3 tazas al día",
                    "4 o 5 tazas al día",
                    "Más de 5 tazas al día"
                ),
                selectedOption = viewModel.consumoCafeTe,
                onOptionSelected = { viewModel.consumoCafeTe = it }
            )

            QuestionTitle("¿Consumes bebidas energizantes (Red Bull, Monster, etc.)?")
            SingleChoiceQuestion(
                options = listOf(
                    "No",
                    "De vez en cuando (1 o 2 al mes)",
                    "Regularmente (1 o 2 a la semana)",
                    "Con frecuencia (3 o más a la semana)",
                    "Todos los días"
                ),
                selectedOption = viewModel.consumeBebidasEnergizantes,
                onOptionSelected = { viewModel.consumeBebidasEnergizantes = it }
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = { viewModel.submitQuestionnaire() },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                enabled = state !is QuestionnaireState.Loading && viewModel.isFormValid()
            ) {
                if (state is QuestionnaireState.Loading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Icon(Icons.Filled.Send, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Enviar Cuestionario")
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}