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
class CargaTrabajoViewModel : ViewModel() {
    private val repository = AuthRepository()

    private val _state = MutableStateFlow<QuestionnaireState>(QuestionnaireState.Idle)
    val state: StateFlow<QuestionnaireState> = _state.asStateFlow()

    var cargaTrabajoActual by mutableStateOf("")
    var presionTiempoPlazos by mutableStateOf("")
    var ritmoTrabajo by mutableStateOf("")
    var llevaTrabajoCasa by mutableStateOf("")
    var trabajaFinesSemana by mutableStateOf("")
    var horasFueraHorario by mutableStateOf("")
    var puedeDecidirComoTrabajar by mutableStateOf("")
    var puedePlanificarPausas by mutableStateOf("")
    var participaDecisiones by mutableStateOf("")
    var apoyoSuperior by mutableStateOf("")
    var relacionCompaneros by mutableStateOf("")
    var acosoLaboral by mutableStateOf("")
    var trabajoValorado by mutableStateOf("")
    var satisfaccionGeneral by mutableStateOf("")
    var salarioAdecuado by mutableStateOf("")

    fun isFormValid(): Boolean {
        return cargaTrabajoActual.isNotEmpty() &&
                presionTiempoPlazos.isNotEmpty() &&
                ritmoTrabajo.isNotEmpty() &&
                llevaTrabajoCasa.isNotEmpty() &&
                trabajaFinesSemana.isNotEmpty() &&
                horasFueraHorario.isNotEmpty() &&
                puedeDecidirComoTrabajar.isNotEmpty() &&
                puedePlanificarPausas.isNotEmpty() &&
                participaDecisiones.isNotEmpty() &&
                apoyoSuperior.isNotEmpty() &&
                relacionCompaneros.isNotEmpty() &&
                acosoLaboral.isNotEmpty() &&
                trabajoValorado.isNotEmpty() &&
                satisfaccionGeneral.isNotEmpty() &&
                salarioAdecuado.isNotEmpty()
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

            val questionnaire = CargaTrabajoQuestionnaire(
                userId = userId,
                cargaTrabajoActual = cargaTrabajoActual,
                presionTiempoPlazos = presionTiempoPlazos,
                ritmoTrabajo = ritmoTrabajo,
                llevaTrabajoCasa = llevaTrabajoCasa,
                trabajaFinesSemana = trabajaFinesSemana,
                horasFueraHorario = horasFueraHorario,
                puedeDecidirComoTrabajar = puedeDecidirComoTrabajar,
                puedePlanificarPausas = puedePlanificarPausas,
                participaDecisiones = participaDecisiones,
                apoyoSuperior = apoyoSuperior,
                relacionCompaneros = relacionCompaneros,
                acosoLaboral = acosoLaboral,
                trabajoValorado = trabajoValorado,
                satisfaccionGeneral = satisfaccionGeneral,
                salarioAdecuado = salarioAdecuado
            )

            val result = repository.saveCargaTrabajoQuestionnaire(questionnaire)
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
fun CargaTrabajoQuestionnaireScreen(
    onComplete: () -> Unit,
    onBack: () -> Unit,
    viewModel: CargaTrabajoViewModel = viewModel()
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
                title = { Text("Carga de Trabajo") },
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
                        text = "Evalúa la demanda laboral, control, apoyo social y satisfacción. 15 preguntas, 5-7 minutos.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // DEMANDAS LABORALES
            SectionHeader("Demandas Laborales")

            // 66. Carga de trabajo
            QuestionTitle("66. Carga de trabajo actual")
            SingleChoiceQuestion(
                options = listOf(
                    "Muy baja (tengo mucho tiempo libre)",
                    "Baja (puedo hacer todo cómodamente)",
                    "Adecuada (equilibrada)",
                    "Alta (requiere esfuerzo extra)",
                    "Excesiva (no puedo con todo)"
                ),
                selectedOption = viewModel.cargaTrabajoActual,
                onOptionSelected = { viewModel.cargaTrabajoActual = it }
            )

            // 67. Presión de tiempo
            QuestionTitle("67. Presión de tiempo y plazos")
            SingleChoiceQuestion(
                options = listOf(
                    "Muy baja",
                    "Baja",
                    "Moderada",
                    "Alta",
                    "Muy alta/Constante"
                ),
                selectedOption = viewModel.presionTiempoPlazos,
                onOptionSelected = { viewModel.presionTiempoPlazos = it }
            )

            // 68. Ritmo de trabajo
            QuestionTitle("68. Ritmo de trabajo")
            SingleChoiceQuestion(
                options = listOf(
                    "Puedo manejarlo con calma",
                    "Constante pero manejable",
                    "Acelerado frecuentemente",
                    "Frenético/Agobiante"
                ),
                selectedOption = viewModel.ritmoTrabajo,
                onOptionSelected = { viewModel.ritmoTrabajo = it }
            )

            // 69. Trabajo a casa
            QuestionTitle("69. ¿Llevas trabajo a casa?")
            SingleChoiceQuestion(
                options = listOf(
                    "Nunca",
                    "Ocasionalmente (1-2 veces al mes)",
                    "Frecuentemente (1-2 veces por semana)",
                    "Muy frecuentemente (3-4 veces por semana)",
                    "Siempre/Todos los días"
                ),
                selectedOption = viewModel.llevaTrabajoCasa,
                onOptionSelected = { viewModel.llevaTrabajoCasa = it }
            )

            // 70. Fines de semana
            QuestionTitle("70. ¿Trabajas en fines de semana?")
            SingleChoiceQuestion(
                options = listOf(
                    "Nunca",
                    "1-2 veces al mes",
                    "1-2 fines de semana al mes",
                    "3 fines de semana al mes",
                    "Todos los fines de semana"
                ),
                selectedOption = viewModel.trabajaFinesSemana,
                onOptionSelected = { viewModel.trabajaFinesSemana = it }
            )

            // 71. Horas extra
            QuestionTitle("71. Horas de trabajo fuera del horario oficial (semanal)")
            SingleChoiceQuestion(
                options = listOf(
                    "Ninguna",
                    "1-3 horas",
                    "4-7 horas",
                    "8-12 horas",
                    "Más de 12 horas"
                ),
                selectedOption = viewModel.horasFueraHorario,
                onOptionSelected = { viewModel.horasFueraHorario = it }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // CONTROL Y AUTONOMÍA
            SectionHeader("Control y Autonomía")

            // 72. Decidir cómo hacer el trabajo
            QuestionTitle("72. ¿Puedes decidir cómo hacer tu trabajo?")
            SingleChoiceQuestion(
                options = listOf(
                    "Sí, tengo total autonomía",
                    "Sí, en la mayoría de casos",
                    "Parcialmente",
                    "Poco",
                    "No, todo está muy controlado"
                ),
                selectedOption = viewModel.puedeDecidirComoTrabajar,
                onOptionSelected = { viewModel.puedeDecidirComoTrabajar = it }
            )

            // 73. Planificar pausas
            QuestionTitle("73. ¿Puedes planificar tus pausas cuando lo necesitas?")
            SingleChoiceQuestion(
                options = listOf(
                    "Sí, libremente",
                    "Sí, con algunas restricciones",
                    "Con dificultad",
                    "No puedo"
                ),
                selectedOption = viewModel.puedePlanificarPausas,
                onOptionSelected = { viewModel.puedePlanificarPausas = it }
            )

            // 74. Participar en decisiones
            QuestionTitle("74. ¿Participas en decisiones que afectan tu trabajo?")
            SingleChoiceQuestion(
                options = listOf(
                    "Sí, frecuentemente",
                    "A veces",
                    "Rara vez",
                    "Nunca"
                ),
                selectedOption = viewModel.participaDecisiones,
                onOptionSelected = { viewModel.participaDecisiones = it }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // APOYO SOCIAL
            SectionHeader("Apoyo Social")

            // 75. Apoyo del superior
            QuestionTitle("75. Apoyo de tu superior inmediato")
            SingleChoiceQuestion(
                options = listOf(
                    "Muy bueno (siempre disponible)",
                    "Bueno",
                    "Regular",
                    "Malo",
                    "Muy malo/Ninguno"
                ),
                selectedOption = viewModel.apoyoSuperior,
                onOptionSelected = { viewModel.apoyoSuperior = it }
            )

            // 76. Relación con compañeros
            QuestionTitle("76. Relación con compañeros de trabajo")
            SingleChoiceQuestion(
                options = listOf(
                    "Excelente (colaborativa)",
                    "Buena",
                    "Regular",
                    "Mala (conflictiva)",
                    "Trabajo solo"
                ),
                selectedOption = viewModel.relacionCompaneros,
                onOptionSelected = { viewModel.relacionCompaneros = it }
            )

            // 77. Acoso laboral
            QuestionTitle("77. ¿Has experimentado acoso laboral o mobbing?")
            SingleChoiceQuestion(
                options = listOf(
                    "No, nunca",
                    "Alguna vez",
                    "Ocasionalmente",
                    "Frecuentemente",
                    "Constantemente"
                ),
                selectedOption = viewModel.acosoLaboral,
                onOptionSelected = { viewModel.acosoLaboral = it }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // RECONOCIMIENTO Y SATISFACCIÓN
            SectionHeader("Reconocimiento y Satisfacción")

            // 78. Trabajo valorado
            QuestionTitle("78. ¿Sientes que tu trabajo es valorado/reconocido?")
            SingleChoiceQuestion(
                options = listOf(
                    "Sí, definitivamente",
                    "En general sí",
                    "A veces",
                    "Rara vez",
                    "Nunca"
                ),
                selectedOption = viewModel.trabajoValorado,
                onOptionSelected = { viewModel.trabajoValorado = it }
            )

            // 79. Satisfacción general
            QuestionTitle("79. Satisfacción general con tu trabajo")
            SingleChoiceQuestion(
                options = listOf(
                    "Muy satisfecho",
                    "Satisfecho",
                    "Neutral",
                    "Insatisfecho",
                    "Muy insatisfecho"
                ),
                selectedOption = viewModel.satisfaccionGeneral,
                onOptionSelected = { viewModel.satisfaccionGeneral = it }
            )

            // 80. Salario adecuado
            QuestionTitle("80. ¿El salario es adecuado para tu carga de trabajo?")
            SingleChoiceQuestion(
                options = listOf(
                    "Sí, muy adecuado",
                    "Sí, adecuado",
                    "Apenas suficiente",
                    "Insuficiente",
                    "Muy insuficiente"
                ),
                selectedOption = viewModel.salarioAdecuado,
                onOptionSelected = { viewModel.salarioAdecuado = it }
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