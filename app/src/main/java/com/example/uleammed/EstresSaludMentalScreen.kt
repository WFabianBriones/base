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
                title = { Text("Estrés y Salud Mental") },
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
                        text = "Identifica niveles de estrés, burnout y bienestar emocional. 19 preguntas, 7-9 minutos.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Nivel de Estrés
            SectionHeader("Nivel de Estrés")

            // 81. Nivel de estrés general
            QuestionTitle("81. Nivel de estrés laboral general (1-10)")
            Text(
                text = "Valor actual: ${viewModel.nivelEstresGeneral}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Slider(
                value = viewModel.nivelEstresGeneral.toFloat(),
                onValueChange = { viewModel.nivelEstresGeneral = it.toInt() },
                valueRange = 1f..10f,
                steps = 8,
                modifier = Modifier.fillMaxWidth()
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("1 (Muy bajo)", style = MaterialTheme.typography.bodySmall)
                Text("10 (Extremo)", style = MaterialTheme.typography.bodySmall)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 82. Aumento del estrés
            QuestionTitle("82. ¿El estrés ha aumentado en los últimos 6 meses?")
            SingleChoiceQuestion(
                options = listOf(
                    "Sí, significativamente",
                    "Sí, un poco",
                    "Se ha mantenido igual",
                    "Ha disminuido"
                ),
                selectedOption = viewModel.estresAumento6Meses,
                onOptionSelected = { viewModel.estresAumento6Meses = it }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Síntomas de Estrés Crónico
            SectionHeader("Síntomas de Estrés Crónico")
            Text(
                text = "Frecuencia: 0=Nunca, 1=Rara vez, 2=Ocasionalmente, 3=Frecuentemente, 4=Muy frecuentemente, 5=Constantemente",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // 83. Fatiga/agotamiento
            QuestionTitle("83. Fatiga/agotamiento extremo")
            ScaleQuestion(
                value = viewModel.fatigaAgotamiento,
                onValueChange = { viewModel.fatigaAgotamiento = it }
            )

            // 84. Dificultad concentración
            QuestionTitle("84. Dificultad para concentrarse")
            ScaleQuestion(
                value = viewModel.dificultadConcentracion,
                onValueChange = { viewModel.dificultadConcentracion = it }
            )

            // 85. Problemas memoria
            QuestionTitle("85. Problemas de memoria a corto plazo")
            ScaleQuestion(
                value = viewModel.problemasMemoria,
                onValueChange = { viewModel.problemasMemoria = it }
            )

            // 86. Irritabilidad
            QuestionTitle("86. Irritabilidad/cambios de humor")
            ScaleQuestion(
                value = viewModel.irritabilidad,
                onValueChange = { viewModel.irritabilidad = it }
            )

            // 87. Ansiedad
            QuestionTitle("87. Ansiedad relacionada con el trabajo")
            ScaleQuestion(
                value = viewModel.ansiedadTrabajo,
                onValueChange = { viewModel.ansiedadTrabajo = it }
            )

            // 88. Preocupaciones constantes
            QuestionTitle("88. Preocupaciones constantes sobre el trabajo")
            ScaleQuestion(
                value = viewModel.preocupacionesConstantes,
                onValueChange = { viewModel.preocupacionesConstantes = it }
            )

            // 89. Sensación abrumado
            QuestionTitle("89. Sensación de estar abrumado/a")
            ScaleQuestion(
                value = viewModel.sensacionAbrumado,
                onValueChange = { viewModel.sensacionAbrumado = it }
            )

            // 90. Dificultad desconectar
            QuestionTitle("90. Dificultad para desconectar del trabajo")
            ScaleQuestion(
                value = viewModel.dificultadDesconectar,
                onValueChange = { viewModel.dificultadDesconectar = it }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Síntomas de Burnout
            SectionHeader("Síntomas de Burnout")

            // 91. Pérdida de motivación
            QuestionTitle("91. Pérdida de motivación/entusiasmo por el trabajo")
            ScaleQuestion(
                value = viewModel.perdidaMotivacion,
                onValueChange = { viewModel.perdidaMotivacion = it }
            )

            // 92. Sensación de improductividad
            QuestionTitle("92. Sensación de no lograr nada/improductividad")
            ScaleQuestion(
                value = viewModel.sensacionInproductividad,
                onValueChange = { viewModel.sensacionInproductividad = it }
            )

            // 93. Actitud negativa
            QuestionTitle("93. Cinismo o actitud negativa hacia el trabajo")
            ScaleQuestion(
                value = viewModel.actitudNegativa,
                onValueChange = { viewModel.actitudNegativa = it }
            )

            // 94. Sentimiento de ineficacia
            QuestionTitle("94. Sentimiento de ineficacia profesional")
            ScaleQuestion(
                value = viewModel.sentimientoIneficacia,
                onValueChange = { viewModel.sentimientoIneficacia = it }
            )

            // 95. Agotamiento emocional
            QuestionTitle("95. Agotamiento emocional")
            SingleChoiceQuestion(
                options = listOf(
                    "Nunca",
                    "Rara vez",
                    "Ocasionalmente",
                    "Frecuentemente",
                    "Constantemente/Siempre"
                ),
                selectedOption = viewModel.agotamientoEmocional,
                onOptionSelected = { viewModel.agotamientoEmocional = it }
            )

            // 96. Despersonalización
            QuestionTitle("96. Despersonalización (distanciamiento de estudiantes/colegas)")
            SingleChoiceQuestion(
                options = listOf(
                    "Nunca",
                    "Rara vez",
                    "Ocasionalmente",
                    "Frecuentemente",
                    "Constantemente"
                ),
                selectedOption = viewModel.despersonalizacion,
                onOptionSelected = { viewModel.despersonalizacion = it }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Impacto en Vida Personal
            SectionHeader("Impacto en la Vida Personal")

            // 97. Afecta vida personal
            QuestionTitle("97. ¿El estrés laboral afecta tu vida personal/familiar?")
            SingleChoiceQuestion(
                options = listOf(
                    "No, para nada",
                    "Un poco",
                    "Moderadamente",
                    "Significativamente",
                    "Severamente"
                ),
                selectedOption = viewModel.estresAfectaVidaPersonal,
                onOptionSelected = { viewModel.estresAfectaVidaPersonal = it }
            )

            // 98. Cambiar de trabajo
            QuestionTitle("98. ¿Has considerado cambiar de trabajo por el estrés?")
            SingleChoiceQuestion(
                options = listOf(
                    "No, estoy bien",
                    "Lo he pensado vagamente",
                    "Lo pienso frecuentemente",
                    "Sí, estoy buscando activamente",
                    "Ya decidí cambiar"
                ),
                selectedOption = viewModel.consideraCambiarTrabajo,
                onOptionSelected = { viewModel.consideraCambiarTrabajo = it }
            )

            // 99. Interfiere con descanso
            QuestionTitle("99. ¿El trabajo interfiere con tu tiempo de descanso?")
            SingleChoiceQuestion(
                options = listOf(
                    "Nunca",
                    "Rara vez",
                    "Ocasionalmente",
                    "Frecuentemente",
                    "Siempre"
                ),
                selectedOption = viewModel.trabajoInterfiereTiempoDescanso,
                onOptionSelected = { viewModel.trabajoInterfiereTiempoDescanso = it }
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
                RadioButton(
                    selected = value == scale,
                    onClick = { onValueChange(scale) }
                )
                Text(
                    text = scale.toString(),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
    Spacer(modifier = Modifier.height(8.dp))
}