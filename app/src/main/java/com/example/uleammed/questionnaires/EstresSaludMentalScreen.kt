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
import androidx.compose.ui.text.font.FontWeight
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
class EstresSaludMentalViewModel : ViewModel() {
    private val repository = AuthRepository()

    private val _state = MutableStateFlow<QuestionnaireState>(QuestionnaireState.Idle)
    val state: StateFlow<QuestionnaireState> = _state.asStateFlow()

    var nivelEstresGeneral by mutableStateOf(5)
    var estresAumento6Meses by mutableStateOf("")
    var fatigaAgotamiento by mutableStateOf(0)
    var dificultadConcentracion by mutableStateOf(0)
    var problemasMemoria by mutableStateOf(0)
    var irritabilidad by mutableStateOf(0)
    var ansiedadTrabajo by mutableStateOf(0)
    var preocupacionesConstantes by mutableStateOf(0)
    var sensacionAbrumado by mutableStateOf(0)
    var dificultadDesconectar by mutableStateOf(0)
    var perdidaMotivacion by mutableStateOf(0)
    var sensacionInproductividad by mutableStateOf(0)
    var actitudNegativa by mutableStateOf(0)
    var sentimientoIneficacia by mutableStateOf(0)
    var agotamientoEmocional by mutableStateOf("")
    var despersonalizacion by mutableStateOf("")
    var estresAfectaVidaPersonal by mutableStateOf("")
    var consideraCambiarTrabajo by mutableStateOf("")
    var trabajoInterfiereTiempoDescanso by mutableStateOf("")

    fun isFormValid(): Boolean {
        return estresAumento6Meses.isNotEmpty() &&
                agotamientoEmocional.isNotEmpty() &&
                despersonalizacion.isNotEmpty() &&
                estresAfectaVidaPersonal.isNotEmpty() &&
                consideraCambiarTrabajo.isNotEmpty() &&
                trabajoInterfiereTiempoDescanso.isNotEmpty()
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
            val questionnaire = EstresSaludMentalQuestionnaire(
                userId = userId,
                nivelEstresGeneral = nivelEstresGeneral,
                estresAumento6Meses = estresAumento6Meses,
                fatigaAgotamiento = fatigaAgotamiento,
                dificultadConcentracion = dificultadConcentracion,
                problemasMemoria = problemasMemoria,
                irritabilidad = irritabilidad,
                ansiedadTrabajo = ansiedadTrabajo,
                preocupacionesConstantes = preocupacionesConstantes,
                sensacionAbrumado = sensacionAbrumado,
                dificultadDesconectar = dificultadDesconectar,
                perdidaMotivacion = perdidaMotivacion,
                sensacionInproductividad = sensacionInproductividad,
                actitudNegativa = actitudNegativa,
                sentimientoIneficacia = sentimientoIneficacia,
                agotamientoEmocional = agotamientoEmocional,
                despersonalizacion = despersonalizacion,
                estresAfectaVidaPersonal = estresAfectaVidaPersonal,
                consideraCambiarTrabajo = consideraCambiarTrabajo,
                trabajoInterfiereTiempoDescanso = trabajoInterfiereTiempoDescanso
            )
            val result = repository.saveEstresSaludMentalQuestionnaire(questionnaire)
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
fun EstresSaludMentalQuestionnaireScreen(
    onComplete: () -> Unit,
    onBack: () -> Unit,
    viewModel: EstresSaludMentalViewModel = viewModel()
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
                title = { Text("Estrés y bienestar emocional") },
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
                        text = "Cuéntanos cómo te sientes emocionalmente en relación a tu trabajo. Tus respuestas son confidenciales. 19 preguntas, 7-9 minutos.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── Nivel de estrés ────────────────────────────────────────
            SectionHeader("¿Cuánto estrés sientes?")

            QuestionTitle("Del 1 al 10, ¿qué tan estresado/a te sientes por tu trabajo en general? (1 = nada, 10 = muchísimo)")
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Nivel seleccionado: ${viewModel.nivelEstresGeneral}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Slider(
                        value = viewModel.nivelEstresGeneral.toFloat(),
                        onValueChange = { viewModel.nivelEstresGeneral = it.toInt() },
                        valueRange = 1f..10f,
                        steps = 8
                    )
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("1 (nada)", style = MaterialTheme.typography.bodySmall)
                        Text("10 (muchísimo)", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            QuestionTitle("Comparado con hace 6 meses, ¿tu nivel de estrés ha cambiado?")
            SingleChoiceQuestion(
                options = listOf(
                    "Sí, ha aumentado bastante",
                    "Sí, ha aumentado un poco",
                    "Se ha mantenido igual",
                    "Ha bajado un poco",
                    "Ha bajado bastante"
                ),
                selectedOption = viewModel.estresAumento6Meses,
                onOptionSelected = { viewModel.estresAumento6Meses = it }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ── Síntomas ─────────────────────────────────────────────────
            SectionHeader("¿Cómo te afecta el estrés?")

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Escala:  0 = Nunca  ·  1 = Rara vez  ·  2 = A veces  ·  3 = Seguido  ·  4 = Muy seguido  ·  5 = Siempre",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            QuestionTitle("¿Llegas al final del día sintiéndote agotado/a, sin energía?")
            ScaleQuestion(value = viewModel.fatigaAgotamiento, onValueChange = { viewModel.fatigaAgotamiento = it })

            QuestionTitle("¿Te cuesta concentrarte en lo que estás haciendo?")
            ScaleQuestion(value = viewModel.dificultadConcentracion, onValueChange = { viewModel.dificultadConcentracion = it })

            QuestionTitle("¿Se te olvidan cosas con más frecuencia de lo normal?")
            ScaleQuestion(value = viewModel.problemasMemoria, onValueChange = { viewModel.problemasMemoria = it })

            QuestionTitle("¿Te irritas o cambias de humor con facilidad?")
            ScaleQuestion(value = viewModel.irritabilidad, onValueChange = { viewModel.irritabilidad = it })

            QuestionTitle("¿Sientes angustia o nerviosismo relacionado con el trabajo?")
            ScaleQuestion(value = viewModel.ansiedadTrabajo, onValueChange = { viewModel.ansiedadTrabajo = it })

            QuestionTitle("¿Tu cabeza no para de pensar en temas del trabajo?")
            ScaleQuestion(value = viewModel.preocupacionesConstantes, onValueChange = { viewModel.preocupacionesConstantes = it })

            QuestionTitle("¿Sientes que tienes más trabajo del que puedes manejar?")
            ScaleQuestion(value = viewModel.sensacionAbrumado, onValueChange = { viewModel.sensacionAbrumado = it })

            QuestionTitle("¿Te cuesta \"apagar\" la mente del trabajo cuando terminas la jornada?")
            ScaleQuestion(value = viewModel.dificultadDesconectar, onValueChange = { viewModel.dificultadDesconectar = it })

            Spacer(modifier = Modifier.height(16.dp))

            // ── Señales de agotamiento (burnout) ──────────────────────────
            SectionHeader("Señales de agotamiento")

            QuestionTitle("¿Has perdido el entusiasmo o las ganas de ir a trabajar?")
            ScaleQuestion(value = viewModel.perdidaMotivacion, onValueChange = { viewModel.perdidaMotivacion = it })

            QuestionTitle("¿Sientes que trabajas mucho pero logras poco?")
            ScaleQuestion(value = viewModel.sensacionInproductividad, onValueChange = { viewModel.sensacionInproductividad = it })

            QuestionTitle("¿Te has vuelto más negativo/a o indiferente hacia tu trabajo?")
            ScaleQuestion(value = viewModel.actitudNegativa, onValueChange = { viewModel.actitudNegativa = it })

            QuestionTitle("¿Sientes que no eres tan bueno/a en tu trabajo como antes?")
            ScaleQuestion(value = viewModel.sentimientoIneficacia, onValueChange = { viewModel.sentimientoIneficacia = it })

            QuestionTitle("¿Te sientes emocionalmente vacío/a o quemado/a por el trabajo?")
            SingleChoiceQuestion(
                options = listOf("Nunca", "Rara vez", "A veces", "Con frecuencia", "Siempre o casi siempre"),
                selectedOption = viewModel.agotamientoEmocional,
                onOptionSelected = { viewModel.agotamientoEmocional = it }
            )

            QuestionTitle("¿Te sientes desconectado/a o indiferente hacia las personas con quienes trabajas (compañeros, estudiantes, clientes)?")
            SingleChoiceQuestion(
                options = listOf("Nunca", "Rara vez", "A veces", "Con frecuencia", "Constantemente"),
                selectedOption = viewModel.despersonalizacion,
                onOptionSelected = { viewModel.despersonalizacion = it }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ── Impacto fuera del trabajo ─────────────────────────────────
            SectionHeader("¿Cómo afecta el estrés tu vida personal?")

            QuestionTitle("¿El estrés del trabajo afecta tu vida en casa o tus relaciones personales?")
            SingleChoiceQuestion(
                options = listOf(
                    "No, para nada",
                    "Un poco",
                    "Moderadamente",
                    "Bastante",
                    "Mucho, afecta todo"
                ),
                selectedOption = viewModel.estresAfectaVidaPersonal,
                onOptionSelected = { viewModel.estresAfectaVidaPersonal = it }
            )

            QuestionTitle("¿Has pensado en cambiar de trabajo por el estrés que sientes?")
            SingleChoiceQuestion(
                options = listOf(
                    "No, estoy bien",
                    "Lo he pensado vagamente",
                    "Lo pienso con frecuencia",
                    "Sí, estoy buscando activamente",
                    "Ya decidí cambiar"
                ),
                selectedOption = viewModel.consideraCambiarTrabajo,
                onOptionSelected = { viewModel.consideraCambiarTrabajo = it }
            )

            QuestionTitle("¿El trabajo te quita tiempo de descanso o sueño?")
            SingleChoiceQuestion(
                options = listOf("Nunca", "Rara vez", "A veces", "Con frecuencia", "Siempre"),
                selectedOption = viewModel.trabajoInterfiereTiempoDescanso,
                onOptionSelected = { viewModel.trabajoInterfiereTiempoDescanso = it }
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

@Composable
fun ScaleQuestion(
    value: Int,
    onValueChange: (Int) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        (0..5).forEach { scale ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
            ) {
                RadioButton(selected = value == scale, onClick = { onValueChange(scale) })
                Text(text = scale.toString(), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
    Spacer(modifier = Modifier.height(8.dp))
}