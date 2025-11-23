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
import com.example.uleammed.AuthRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// ViewModel
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
                title = { Text("Hábitos de Sueño") },
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
                        text = "Evalúa la cantidad, calidad e higiene de tu sueño. 9 preguntas, 3-4 minutos.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // CANTIDAD DE SUEÑO
            SectionHeader("Cantidad de Sueño")

            // 100. Horas de sueño semana
            QuestionTitle("100. Horas de sueño promedio por noche (entre semana)")
            SingleChoiceQuestion(
                options = listOf(
                    "Menos de 5 horas",
                    "5-6 horas",
                    "7-8 horas",
                    "Más de 8 horas"
                ),
                selectedOption = viewModel.horasSuenoSemana,
                onOptionSelected = { viewModel.horasSuenoSemana = it }
            )

            // 101. Horas fin de semana
            QuestionTitle("101. Horas de sueño en fines de semana")
            SingleChoiceQuestion(
                options = listOf(
                    "Igual que entre semana",
                    "1-2 horas más",
                    "3-4 horas más",
                    "Duermo menos"
                ),
                selectedOption = viewModel.horasSuenoFinSemana,
                onOptionSelected = { viewModel.horasSuenoFinSemana = it }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // CALIDAD DEL SUEÑO
            SectionHeader("Calidad del Sueño")

            // 102. Calidad general
            QuestionTitle("102. Calidad general del sueño")
            SingleChoiceQuestion(
                options = listOf(
                    "Excelente (despierto descansado)",
                    "Buena (generalmente descansado)",
                    "Regular (a veces descansado)",
                    "Mala (rara vez descansado)",
                    "Muy mala (nunca descansado)"
                ),
                selectedOption = viewModel.calidadSueno,
                onOptionSelected = { viewModel.calidadSueno = it }
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Frecuencia: 0=Nunca, 1=Rara vez, 2=Ocasionalmente, 3=Frecuentemente, 4=Muy frecuentemente, 5=Constantemente",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 103. Dificultad para conciliar
            QuestionTitle("103. Dificultad para conciliar el sueño")
            ScaleQuestion(
                value = viewModel.dificultadConciliarFrecuencia,
                onValueChange = { viewModel.dificultadConciliarFrecuencia = it }
            )

            // 104. Despertares nocturnos
            QuestionTitle("104. Despertares nocturnos")
            ScaleQuestion(
                value = viewModel.despertaresNocturnosFrecuencia,
                onValueChange = { viewModel.despertaresNocturnosFrecuencia = it }
            )

            // 105. Despertar temprano
            QuestionTitle("105. Despertar muy temprano sin poder volver a dormir")
            ScaleQuestion(
                value = viewModel.despertarTempranoFrecuencia,
                onValueChange = { viewModel.despertarTempranoFrecuencia = it }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // HIGIENE DEL SUEÑO
            SectionHeader("Higiene del Sueño")

            // 106. Dispositivos electrónicos
            QuestionTitle("106. ¿Usas dispositivos electrónicos antes de dormir?")
            SingleChoiceQuestion(
                options = listOf(
                    "Sí, hasta el momento de dormir",
                    "Sí, pero dejo de usarlos 30 min antes",
                    "Sí, pero dejo de usarlos 1 hora antes",
                    "Sí, pero dejo de usarlos 2+ horas antes",
                    "No los uso por la noche"
                ),
                selectedOption = viewModel.usaDispositivosAntesDormir,
                onOptionSelected = { viewModel.usaDispositivosAntesDormir = it }
            )

            // 107. Pensar en trabajo
            QuestionTitle("107. ¿Piensas en problemas del trabajo antes de dormir?")
            SingleChoiceQuestion(
                options = listOf(
                    "Siempre/Muy frecuentemente",
                    "Frecuentemente",
                    "A veces",
                    "Rara vez",
                    "Nunca"
                ),
                selectedOption = viewModel.piensaProblemasTrabajoAntesDormir,
                onOptionSelected = { viewModel.piensaProblemasTrabajoAntesDormir = it }
            )

            // 108. Revisar correos
            QuestionTitle("108. ¿Revisas correos/mensajes de trabajo fuera del horario?")
            SingleChoiceQuestion(
                options = listOf(
                    "Sí, constantemente",
                    "Sí, varias veces al día",
                    "Ocasionalmente",
                    "Rara vez",
                    "Nunca"
                ),
                selectedOption = viewModel.revisaCorreosFueraHorario,
                onOptionSelected = { viewModel.revisaCorreosFueraHorario = it }
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