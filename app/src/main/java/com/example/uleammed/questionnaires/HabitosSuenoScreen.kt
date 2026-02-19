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
class HabitosSuenoViewModel : ViewModel() {
    private val repository = AuthRepository()

    private val _state = MutableStateFlow<QuestionnaireState>(QuestionnaireState.Idle)
    val state: StateFlow<QuestionnaireState> = _state.asStateFlow()

    var horasSuenoSemana by mutableStateOf("")
    var horasSuenoFinSemana by mutableStateOf("")
    var calidadSueno by mutableStateOf("")
    var dificultadConciliarFrecuencia by mutableStateOf(0)
    var despertaresNocturnosFrecuencia by mutableStateOf(0)
    var despertarTempranoFrecuencia by mutableStateOf(0)
    var usaDispositivosAntesDormir by mutableStateOf("")
    var piensaProblemasTrabajoAntesDormir by mutableStateOf("")
    var revisaCorreosFueraHorario by mutableStateOf("")

    fun isFormValid(): Boolean {
        return horasSuenoSemana.isNotEmpty() &&
                horasSuenoFinSemana.isNotEmpty() &&
                calidadSueno.isNotEmpty() &&
                usaDispositivosAntesDormir.isNotEmpty() &&
                piensaProblemasTrabajoAntesDormir.isNotEmpty() &&
                revisaCorreosFueraHorario.isNotEmpty()
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
            val questionnaire = HabitosSuenoQuestionnaire(
                userId = userId,
                horasSuenoSemana = horasSuenoSemana,
                horasSuenoFinSemana = horasSuenoFinSemana,
                calidadSueno = calidadSueno,
                dificultadConciliarFrecuencia = dificultadConciliarFrecuencia,
                despertaresNocturnosFrecuencia = despertaresNocturnosFrecuencia,
                despertarTempranoFrecuencia = despertarTempranoFrecuencia,
                usaDispositivosAntesDormir = usaDispositivosAntesDormir,
                piensaProblemasTrabajoAntesDormir = piensaProblemasTrabajoAntesDormir,
                revisaCorreosFueraHorario = revisaCorreosFueraHorario
            )
            val result = repository.saveHabitosSuenoQuestionnaire(questionnaire)
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
fun HabitosSuenoQuestionnaireScreen(
    onComplete: () -> Unit,
    onBack: () -> Unit,
    viewModel: HabitosSuenoViewModel = viewModel()
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
                title = { Text("Tu sueño") },
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
                        text = "Cuéntanos sobre la cantidad, la calidad de tu sueño y qué haces antes de acostarte. 9 preguntas, 3-4 minutos.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── Cuánto duermes ───────────────────────────────────────────
            SectionHeader("¿Cuántas horas duermes?")

            QuestionTitle("Entre semana (días de trabajo), ¿cuántas horas duermes por noche?")
            SingleChoiceQuestion(
                options = listOf(
                    "Menos de 5 horas",
                    "Entre 5 y 6 horas",
                    "Entre 7 y 8 horas",
                    "Más de 8 horas"
                ),
                selectedOption = viewModel.horasSuenoSemana,
                onOptionSelected = { viewModel.horasSuenoSemana = it }
            )

            QuestionTitle("Los fines de semana, ¿cuánto duermes comparado con entre semana?")
            SingleChoiceQuestion(
                options = listOf(
                    "Lo mismo que entre semana",
                    "1 o 2 horas más",
                    "3 o 4 horas más",
                    "Duermo menos que entre semana"
                ),
                selectedOption = viewModel.horasSuenoFinSemana,
                onOptionSelected = { viewModel.horasSuenoFinSemana = it }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ── Calidad del sueño ────────────────────────────────────────
            SectionHeader("¿Cómo es tu sueño?")

            QuestionTitle("En general, ¿cómo es la calidad de tu sueño?")
            SingleChoiceQuestion(
                options = listOf(
                    "Excelente, me despierto descansado/a",
                    "Buena, casi siempre descanso bien",
                    "Regular, a veces descanso y a veces no",
                    "Mala, casi nunca me siento descansado/a",
                    "Muy mala, nunca descanso bien"
                ),
                selectedOption = viewModel.calidadSueno,
                onOptionSelected = { viewModel.calidadSueno = it }
            )

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

            QuestionTitle("¿Te cuesta quedarte dormido/a cuando te acuestas?")
            ScaleQuestion(value = viewModel.dificultadConciliarFrecuencia, onValueChange = { viewModel.dificultadConciliarFrecuencia = it })

            QuestionTitle("¿Te despiertas varias veces durante la noche?")
            ScaleQuestion(value = viewModel.despertaresNocturnosFrecuencia, onValueChange = { viewModel.despertaresNocturnosFrecuencia = it })

            QuestionTitle("¿Te despiertas muy temprano sin poder volver a dormir?")
            ScaleQuestion(value = viewModel.despertarTempranoFrecuencia, onValueChange = { viewModel.despertarTempranoFrecuencia = it })

            Spacer(modifier = Modifier.height(16.dp))

            // ── Hábitos antes de dormir ───────────────────────────────────
            SectionHeader("¿Qué haces antes de dormir?")

            QuestionTitle("¿Usas el celular, tablet o computadora justo antes de acostarte?")
            SingleChoiceQuestion(
                options = listOf(
                    "Sí, hasta que me duermo",
                    "Sí, pero lo apago 30 minutos antes",
                    "Sí, pero lo apago 1 hora antes",
                    "Sí, pero lo apago 2 horas o más antes",
                    "No uso pantallas por las noches"
                ),
                selectedOption = viewModel.usaDispositivosAntesDormir,
                onOptionSelected = { viewModel.usaDispositivosAntesDormir = it }
            )

            QuestionTitle("¿Cuando te acuestas, tu mente sigue dando vueltas a temas del trabajo?")
            SingleChoiceQuestion(
                options = listOf(
                    "Siempre o casi siempre",
                    "Con frecuencia",
                    "A veces",
                    "Rara vez",
                    "Nunca"
                ),
                selectedOption = viewModel.piensaProblemasTrabajoAntesDormir,
                onOptionSelected = { viewModel.piensaProblemasTrabajoAntesDormir = it }
            )

            QuestionTitle("¿Revisas correos o mensajes del trabajo fuera de tu horario laboral?")
            SingleChoiceQuestion(
                options = listOf(
                    "Sí, constantemente",
                    "Sí, varias veces al día",
                    "De vez en cuando",
                    "Rara vez",
                    "Nunca"
                ),
                selectedOption = viewModel.revisaCorreosFueraHorario,
                onOptionSelected = { viewModel.revisaCorreosFueraHorario = it }
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