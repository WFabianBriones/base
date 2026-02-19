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
class SintomasMuscularesViewModel : ViewModel() {
    private val repository = AuthRepository()

    private val _state = MutableStateFlow<QuestionnaireState>(QuestionnaireState.Idle)
    val state: StateFlow<QuestionnaireState> = _state.asStateFlow()

    var dolorCuelloFrecuencia by mutableStateOf(0)
    var dolorCuelloIntensidad by mutableStateOf(0)
    var rigidezCuelloFrecuencia by mutableStateOf(0)
    var rigidezCuelloIntensidad by mutableStateOf(0)
    var dolorHombrosFrecuencia by mutableStateOf(0)
    var dolorHombrosIntensidad by mutableStateOf(0)
    var dolorAumentaFinDia by mutableStateOf("")
    var dolorEspaldaAltaFrecuencia by mutableStateOf(0)
    var dolorEspaldaAltaIntensidad by mutableStateOf(0)
    var dolorEspaldaBajaFrecuencia by mutableStateOf(0)
    var dolorEspaldaBajaIntensidad by mutableStateOf(0)
    var rigidezEspaldaMañanaFrecuencia by mutableStateOf(0)
    var rigidezEspaldaMañanaIntensidad by mutableStateOf(0)
    var dolorConMovimiento by mutableStateOf("")
    var dolorMunecasFrecuencia by mutableStateOf(0)
    var dolorMunecasIntensidad by mutableStateOf(0)
    var dolorManosFrecuencia by mutableStateOf(0)
    var dolorManosIntensidad by mutableStateOf(0)
    var hormigueoManosFrecuencia by mutableStateOf(0)
    var hormigueoManosIntensidad by mutableStateOf(0)
    var hormigueoPorNoche by mutableStateOf("")
    var dolorCodosFrecuencia by mutableStateOf(0)
    var dolorCodosIntensidad by mutableStateOf(0)
    var debilidadAgarrar by mutableStateOf(0)
    var dolorCabezaFrecuencia by mutableStateOf(0)
    var dolorCabezaIntensidad by mutableStateOf(0)
    var momentoDolorCabeza by mutableStateOf("")
    var dolorImpidenActividades by mutableStateOf("")
    var haConsultadoMedico by mutableStateOf("")

    fun isFormValid(): Boolean {
        return dolorAumentaFinDia.isNotEmpty() &&
                dolorConMovimiento.isNotEmpty() &&
                hormigueoPorNoche.isNotEmpty() &&
                momentoDolorCabeza.isNotEmpty() &&
                dolorImpidenActividades.isNotEmpty() &&
                haConsultadoMedico.isNotEmpty()
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
            val questionnaire = SintomasMuscularesQuestionnaire(
                userId = userId,
                dolorCuelloFrecuencia = dolorCuelloFrecuencia,
                dolorCuelloIntensidad = dolorCuelloIntensidad,
                rigidezCuelloFrecuencia = rigidezCuelloFrecuencia,
                rigidezCuelloIntensidad = rigidezCuelloIntensidad,
                dolorHombrosFrecuencia = dolorHombrosFrecuencia,
                dolorHombrosIntensidad = dolorHombrosIntensidad,
                dolorAumentaFinDia = dolorAumentaFinDia,
                dolorEspaldaAltaFrecuencia = dolorEspaldaAltaFrecuencia,
                dolorEspaldaAltaIntensidad = dolorEspaldaAltaIntensidad,
                dolorEspaldaBajaFrecuencia = dolorEspaldaBajaFrecuencia,
                dolorEspaldaBajaIntensidad = dolorEspaldaBajaIntensidad,
                rigidezEspaldaMañanaFrecuencia = rigidezEspaldaMañanaFrecuencia,
                rigidezEspaldaMañanaIntensidad = rigidezEspaldaMañanaIntensidad,
                dolorConMovimiento = dolorConMovimiento,
                dolorMunecasFrecuencia = dolorMunecasFrecuencia,
                dolorMunecasIntensidad = dolorMunecasIntensidad,
                dolorManosFrecuencia = dolorManosFrecuencia,
                dolorManosIntensidad = dolorManosIntensidad,
                hormigueoManosFrecuencia = hormigueoManosFrecuencia,
                hormigueoManosIntensidad = hormigueoManosIntensidad,
                hormigueoPorNoche = hormigueoPorNoche,
                dolorCodosFrecuencia = dolorCodosFrecuencia,
                dolorCodosIntensidad = dolorCodosIntensidad,
                debilidadAgarrar = debilidadAgarrar,
                dolorCabezaFrecuencia = dolorCabezaFrecuencia,
                dolorCabezaIntensidad = dolorCabezaIntensidad,
                momentoDolorCabeza = momentoDolorCabeza,
                dolorImpidenActividades = dolorImpidenActividades,
                haConsultadoMedico = haConsultadoMedico
            )
            val result = repository.saveSintomasMuscularesQuestionnaire(questionnaire)
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
fun SintomasMuscularesQuestionnaireScreen(
    onComplete: () -> Unit,
    onBack: () -> Unit,
    viewModel: SintomasMuscularesViewModel = viewModel()
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
                title = { Text("Dolores y molestias físicas") },
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
                        text = "Cuéntanos si has sentido dolores o molestias en el cuerpo relacionados con tu trabajo. Selecciona qué tan seguido ocurre y qué tan fuerte es.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

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

            Spacer(modifier = Modifier.height(24.dp))

            // ── Cuello ──────────────────────────────────────────────────
            SectionHeader("Cuello")

            QuestionTitle("¿Con qué frecuencia te duele el cuello?")
            Text("Frecuencia:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
            ScaleQuestion(value = viewModel.dolorCuelloFrecuencia, onValueChange = { viewModel.dolorCuelloFrecuencia = it })
            Text("¿Qué tan fuerte es ese dolor?", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
            ScaleQuestion(value = viewModel.dolorCuelloIntensidad, onValueChange = { viewModel.dolorCuelloIntensidad = it })

            QuestionTitle("¿Sientes el cuello agarrotado o rígido?")
            Text("Frecuencia:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
            ScaleQuestion(value = viewModel.rigidezCuelloFrecuencia, onValueChange = { viewModel.rigidezCuelloFrecuencia = it })
            Text("¿Qué tan fuerte es esa rigidez?", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
            ScaleQuestion(value = viewModel.rigidezCuelloIntensidad, onValueChange = { viewModel.rigidezCuelloIntensidad = it })

            QuestionTitle("¿Con qué frecuencia te duelen los hombros?")
            Text("Frecuencia:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
            ScaleQuestion(value = viewModel.dolorHombrosFrecuencia, onValueChange = { viewModel.dolorHombrosFrecuencia = it })
            Text("¿Qué tan fuerte es ese dolor?", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
            ScaleQuestion(value = viewModel.dolorHombrosIntensidad, onValueChange = { viewModel.dolorHombrosIntensidad = it })

            QuestionTitle("¿El dolor de cuello u hombros empeora hacia el final del día?")
            SingleChoiceQuestion(
                options = listOf("Sí, siempre empeora", "Sí, a veces", "No cambia", "No tengo ese dolor"),
                selectedOption = viewModel.dolorAumentaFinDia,
                onOptionSelected = { viewModel.dolorAumentaFinDia = it }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ── Espalda ─────────────────────────────────────────────────
            SectionHeader("Espalda")

            QuestionTitle("¿Con qué frecuencia te duele la parte alta de la espalda (entre los omóplatos)?")
            Text("Frecuencia:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
            ScaleQuestion(value = viewModel.dolorEspaldaAltaFrecuencia, onValueChange = { viewModel.dolorEspaldaAltaFrecuencia = it })
            Text("¿Qué tan fuerte es ese dolor?", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
            ScaleQuestion(value = viewModel.dolorEspaldaAltaIntensidad, onValueChange = { viewModel.dolorEspaldaAltaIntensidad = it })

            QuestionTitle("¿Con qué frecuencia te duele la parte baja de la espalda (zona lumbar)?")
            Text("Frecuencia:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
            ScaleQuestion(value = viewModel.dolorEspaldaBajaFrecuencia, onValueChange = { viewModel.dolorEspaldaBajaFrecuencia = it })
            Text("¿Qué tan fuerte es ese dolor?", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
            ScaleQuestion(value = viewModel.dolorEspaldaBajaIntensidad, onValueChange = { viewModel.dolorEspaldaBajaIntensidad = it })

            QuestionTitle("¿Al despertar, sientes la espalda rígida o entumecida?")
            Text("Frecuencia:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
            ScaleQuestion(value = viewModel.rigidezEspaldaMañanaFrecuencia, onValueChange = { viewModel.rigidezEspaldaMañanaFrecuencia = it })
            Text("¿Qué tan fuerte es esa rigidez?", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
            ScaleQuestion(value = viewModel.rigidezEspaldaMañanaIntensidad, onValueChange = { viewModel.rigidezEspaldaMañanaIntensidad = it })

            QuestionTitle("Cuando te mueves o cambias de posición, ¿el dolor de espalda...?")
            SingleChoiceQuestion(
                options = listOf(
                    "Mejora con el movimiento",
                    "Empeora con el movimiento",
                    "No cambia",
                    "No tengo dolor de espalda"
                ),
                selectedOption = viewModel.dolorConMovimiento,
                onOptionSelected = { viewModel.dolorConMovimiento = it }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ── Brazos y manos ───────────────────────────────────────────
            SectionHeader("Brazos y manos")

            QuestionTitle("¿Con qué frecuencia te duelen las muñecas?")
            Text("Frecuencia:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
            ScaleQuestion(value = viewModel.dolorMunecasFrecuencia, onValueChange = { viewModel.dolorMunecasFrecuencia = it })
            Text("¿Qué tan fuerte es ese dolor?", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
            ScaleQuestion(value = viewModel.dolorMunecasIntensidad, onValueChange = { viewModel.dolorMunecasIntensidad = it })

            QuestionTitle("¿Con qué frecuencia te duelen las manos o los dedos?")
            Text("Frecuencia:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
            ScaleQuestion(value = viewModel.dolorManosFrecuencia, onValueChange = { viewModel.dolorManosFrecuencia = it })
            Text("¿Qué tan fuerte es ese dolor?", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
            ScaleQuestion(value = viewModel.dolorManosIntensidad, onValueChange = { viewModel.dolorManosIntensidad = it })

            QuestionTitle("¿Sientes hormigueo o adormecimiento en las manos?")
            Text("Frecuencia:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
            ScaleQuestion(value = viewModel.hormigueoManosFrecuencia, onValueChange = { viewModel.hormigueoManosFrecuencia = it })
            Text("¿Qué tan molesto es?", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
            ScaleQuestion(value = viewModel.hormigueoManosIntensidad, onValueChange = { viewModel.hormigueoManosIntensidad = it })

            QuestionTitle("¿Ese hormigueo aparece o empeora por las noches?")
            SingleChoiceQuestion(
                options = listOf(
                    "Sí, me despierta por las noches",
                    "Sí, a veces de noche",
                    "No, solo de día",
                    "No tengo hormigueo"
                ),
                selectedOption = viewModel.hormigueoPorNoche,
                onOptionSelected = { viewModel.hormigueoPorNoche = it }
            )

            QuestionTitle("¿Con qué frecuencia te duelen los codos?")
            Text("Frecuencia:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
            ScaleQuestion(value = viewModel.dolorCodosFrecuencia, onValueChange = { viewModel.dolorCodosFrecuencia = it })
            Text("¿Qué tan fuerte es ese dolor?", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
            ScaleQuestion(value = viewModel.dolorCodosIntensidad, onValueChange = { viewModel.dolorCodosIntensidad = it })

            QuestionTitle("¿Sientes que te cuesta fuerza agarrar objetos?")
            Text("Frecuencia (0 = nunca  ·  5 = siempre):", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
            ScaleQuestion(value = viewModel.debilidadAgarrar, onValueChange = { viewModel.debilidadAgarrar = it })

            Spacer(modifier = Modifier.height(16.dp))

            // ── Cabeza ───────────────────────────────────────────────────
            SectionHeader("Dolores de cabeza")

            QuestionTitle("¿Con qué frecuencia sientes dolor de cabeza por tensión?")
            Text("Frecuencia:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
            ScaleQuestion(value = viewModel.dolorCabezaFrecuencia, onValueChange = { viewModel.dolorCabezaFrecuencia = it })
            Text("¿Qué tan fuerte es ese dolor?", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
            ScaleQuestion(value = viewModel.dolorCabezaIntensidad, onValueChange = { viewModel.dolorCabezaIntensidad = it })

            QuestionTitle("¿En qué momento del día te aparece más el dolor de cabeza?")
            SingleChoiceQuestion(
                options = listOf(
                    "Por la mañana",
                    "A media mañana o al mediodía",
                    "Al final del día o la tarde",
                    "No tiene un momento fijo",
                    "No tengo dolores de cabeza"
                ),
                selectedOption = viewModel.momentoDolorCabeza,
                onOptionSelected = { viewModel.momentoDolorCabeza = it }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ── Impacto en el día a día ───────────────────────────────────
            SectionHeader("¿Cómo te afectan estos dolores?")

            QuestionTitle("¿Alguno de estos dolores te ha impedido hacer actividades del día a día?")
            SingleChoiceQuestion(
                options = listOf(
                    "Sí, con frecuencia no puedo hacer cosas normales",
                    "Sí, de vez en cuando",
                    "Rara vez",
                    "No, nunca me han limitado"
                ),
                selectedOption = viewModel.dolorImpidenActividades,
                onOptionSelected = { viewModel.dolorImpidenActividades = it }
            )

            QuestionTitle("¿Has ido al médico por alguno de estos dolores?")
            SingleChoiceQuestion(
                options = listOf(
                    "Sí, y sigo en tratamiento",
                    "Sí, pero no continué el tratamiento",
                    "No, pero creo que debería ir",
                    "No, no lo considero necesario"
                ),
                selectedOption = viewModel.haConsultadoMedico,
                onOptionSelected = { viewModel.haConsultadoMedico = it }
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