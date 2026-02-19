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
                title = { Text("Tu carga de trabajo") },
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
                        text = "Cuéntanos cómo vives el día a día en tu trabajo: cuánto tienes que hacer, qué libertad tienes y cómo te llevas con tu equipo. 15 preguntas, 5-7 minutos.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── Cantidad de trabajo ───────────────────────────────────────
            SectionHeader("¿Cuánto trabajo tienes?")

            QuestionTitle("¿Cómo describes la cantidad de trabajo que tienes actualmente?")
            SingleChoiceQuestion(
                options = listOf(
                    "Muy poca, tengo mucho tiempo libre",
                    "Poca, puedo hacer todo cómodamente",
                    "La justa, está bien equilibrada",
                    "Bastante, me exige esfuerzo extra",
                    "Demasiada, no puedo con todo"
                ),
                selectedOption = viewModel.cargaTrabajoActual,
                onOptionSelected = { viewModel.cargaTrabajoActual = it }
            )

            QuestionTitle("¿Cuánta presión sientes por cumplir plazos y fechas límite?")
            SingleChoiceQuestion(
                options = listOf(
                    "Muy poca o ninguna",
                    "Poca",
                    "Normal, manejable",
                    "Alta",
                    "Muy alta, es constante"
                ),
                selectedOption = viewModel.presionTiempoPlazos,
                onOptionSelected = { viewModel.presionTiempoPlazos = it }
            )

            QuestionTitle("¿Cómo describirías el ritmo de tu trabajo diario?")
            SingleChoiceQuestion(
                options = listOf(
                    "Tranquilo, puedo manejarlo sin apuros",
                    "Constante pero manejable",
                    "Acelerado con frecuencia",
                    "Frenético, me siento agobiado"
                ),
                selectedOption = viewModel.ritmoTrabajo,
                onOptionSelected = { viewModel.ritmoTrabajo = it }
            )

            QuestionTitle("¿Con qué frecuencia llevas trabajo a casa fuera del horario laboral?")
            SingleChoiceQuestion(
                options = listOf(
                    "Nunca",
                    "Alguna vez al mes",
                    "1 o 2 veces por semana",
                    "3 o 4 veces por semana",
                    "Todos los días"
                ),
                selectedOption = viewModel.llevaTrabajoCasa,
                onOptionSelected = { viewModel.llevaTrabajoCasa = it }
            )

            QuestionTitle("¿Trabajas durante los fines de semana?")
            SingleChoiceQuestion(
                options = listOf(
                    "Nunca",
                    "1 o 2 veces al mes",
                    "1 o 2 fines de semana al mes",
                    "Casi todos los fines de semana",
                    "Todos los fines de semana"
                ),
                selectedOption = viewModel.trabajaFinesSemana,
                onOptionSelected = { viewModel.trabajaFinesSemana = it }
            )

            QuestionTitle("¿Cuántas horas extra trabajas a la semana, fuera de tu horario oficial?")
            SingleChoiceQuestion(
                options = listOf(
                    "Ninguna",
                    "Entre 1 y 3 horas",
                    "Entre 4 y 7 horas",
                    "Entre 8 y 12 horas",
                    "Más de 12 horas"
                ),
                selectedOption = viewModel.horasFueraHorario,
                onOptionSelected = { viewModel.horasFueraHorario = it }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ── Libertad en el trabajo ────────────────────────────────────
            SectionHeader("¿Cuánta libertad tienes en tu trabajo?")

            QuestionTitle("¿Puedes decidir cómo hacer tu trabajo o todo está muy controlado?")
            SingleChoiceQuestion(
                options = listOf(
                    "Tengo total libertad",
                    "Sí, en la mayoría de casos",
                    "A medias",
                    "Muy poca libertad",
                    "No, todo está muy controlado"
                ),
                selectedOption = viewModel.puedeDecidirComoTrabajar,
                onOptionSelected = { viewModel.puedeDecidirComoTrabajar = it }
            )

            QuestionTitle("¿Puedes tomar un descanso cuando lo necesitas?")
            SingleChoiceQuestion(
                options = listOf(
                    "Sí, cuando quiero",
                    "Sí, con algunas restricciones",
                    "Me cuesta, pero a veces puedo",
                    "No puedo"
                ),
                selectedOption = viewModel.puedePlanificarPausas,
                onOptionSelected = { viewModel.puedePlanificarPausas = it }
            )

            QuestionTitle("¿Te incluyen en las decisiones que afectan tu trabajo?")
            SingleChoiceQuestion(
                options = listOf(
                    "Sí, con frecuencia",
                    "A veces",
                    "Rara vez",
                    "Nunca"
                ),
                selectedOption = viewModel.participaDecisiones,
                onOptionSelected = { viewModel.participaDecisiones = it }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ── Relaciones laborales ──────────────────────────────────────
            SectionHeader("Relaciones en el trabajo")

            QuestionTitle("¿Cómo es el apoyo que recibes de tu jefe o superior inmediato?")
            SingleChoiceQuestion(
                options = listOf(
                    "Muy bueno, siempre está disponible",
                    "Bueno",
                    "Regular",
                    "Malo",
                    "Muy malo o inexistente"
                ),
                selectedOption = viewModel.apoyoSuperior,
                onOptionSelected = { viewModel.apoyoSuperior = it }
            )

            QuestionTitle("¿Cómo es tu relación con los compañeros de trabajo?")
            SingleChoiceQuestion(
                options = listOf(
                    "Excelente, muy colaborativa",
                    "Buena",
                    "Regular",
                    "Mala, hay muchos conflictos",
                    "Trabajo solo, no tengo compañeros"
                ),
                selectedOption = viewModel.relacionCompaneros,
                onOptionSelected = { viewModel.relacionCompaneros = it }
            )

            QuestionTitle("¿Has vivido situaciones de acoso, intimidación o maltrato en el trabajo?")
            SingleChoiceQuestion(
                options = listOf(
                    "No, nunca",
                    "Alguna vez aislada",
                    "De vez en cuando",
                    "Con frecuencia",
                    "Constantemente"
                ),
                selectedOption = viewModel.acosoLaboral,
                onOptionSelected = { viewModel.acosoLaboral = it }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ── Reconocimiento y satisfacción ─────────────────────────────
            SectionHeader("Reconocimiento y satisfacción")

            QuestionTitle("¿Sientes que tu trabajo es valorado y reconocido?")
            SingleChoiceQuestion(
                options = listOf(
                    "Sí, siempre",
                    "En general sí",
                    "A veces",
                    "Casi nunca",
                    "Nunca"
                ),
                selectedOption = viewModel.trabajoValorado,
                onOptionSelected = { viewModel.trabajoValorado = it }
            )

            QuestionTitle("En general, ¿qué tan satisfecho/a estás con tu trabajo?")
            SingleChoiceQuestion(
                options = listOf(
                    "Muy satisfecho/a",
                    "Satisfecho/a",
                    "Ni satisfecho ni insatisfecho",
                    "Insatisfecho/a",
                    "Muy insatisfecho/a"
                ),
                selectedOption = viewModel.satisfaccionGeneral,
                onOptionSelected = { viewModel.satisfaccionGeneral = it }
            )

            QuestionTitle("¿Sientes que tu salario es justo para la cantidad de trabajo que haces?")
            SingleChoiceQuestion(
                options = listOf(
                    "Sí, muy justo",
                    "Sí, adecuado",
                    "Apenas suficiente",
                    "No, es insuficiente",
                    "Es muy insuficiente"
                ),
                selectedOption = viewModel.salarioAdecuado,
                onOptionSelected = { viewModel.salarioAdecuado = it }
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