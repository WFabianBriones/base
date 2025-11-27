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

// ViewModel
class SintomasVisualesViewModel : ViewModel() {
    private val repository = AuthRepository()

    private val _state = MutableStateFlow<QuestionnaireState>(QuestionnaireState.Idle)
    val state: StateFlow<QuestionnaireState> = _state.asStateFlow()

    var ojosSecosFrecuencia by mutableStateOf(0)
    var ojosSecosIntensidad by mutableStateOf(0)
    var ardorOjosFrecuencia by mutableStateOf(0)
    var ardorOjosIntensidad by mutableStateOf(0)
    var ojosRojosFrecuencia by mutableStateOf(0)
    var lagrimeoFrecuencia by mutableStateOf(0)
    var visionBorrosaFrecuencia by mutableStateOf(0)
    var dificultadEnfocarFrecuencia by mutableStateOf(0)
    var sensibilidadLuzFrecuencia by mutableStateOf(0)
    var visionDobleFrecuencia by mutableStateOf(0)
    var ojosCansadosFinDia by mutableStateOf("")
    var esfuerzoVerNitidamente by mutableStateOf("")
    var usaLentes by mutableStateOf("")
    var ultimoExamenVisual by mutableStateOf("")
    var aplicaRegla202020 by mutableStateOf("")
    var brilloPantalla by mutableStateOf("")

    fun isFormValid(): Boolean {
        return ojosCansadosFinDia.isNotEmpty() &&
                esfuerzoVerNitidamente.isNotEmpty() &&
                usaLentes.isNotEmpty() &&
                ultimoExamenVisual.isNotEmpty() &&
                aplicaRegla202020.isNotEmpty() &&
                brilloPantalla.isNotEmpty()
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

            val questionnaire = SintomasVisualesQuestionnaire(
                userId = userId,
                ojosSecosFrecuencia = ojosSecosFrecuencia,
                ojosSecosIntensidad = ojosSecosIntensidad,
                ardorOjosFrecuencia = ardorOjosFrecuencia,
                ardorOjosIntensidad = ardorOjosIntensidad,
                ojosRojosFrecuencia = ojosRojosFrecuencia,
                lagrimeoFrecuencia = lagrimeoFrecuencia,
                visionBorrosaFrecuencia = visionBorrosaFrecuencia,
                dificultadEnfocarFrecuencia = dificultadEnfocarFrecuencia,
                sensibilidadLuzFrecuencia = sensibilidadLuzFrecuencia,
                visionDobleFrecuencia = visionDobleFrecuencia,
                ojosCansadosFinDia = ojosCansadosFinDia,
                esfuerzoVerNitidamente = esfuerzoVerNitidamente,
                usaLentes = usaLentes,
                ultimoExamenVisual = ultimoExamenVisual,
                aplicaRegla202020 = aplicaRegla202020,
                brilloPantalla = brilloPantalla
            )

            val result = repository.saveSintomasVisualesQuestionnaire(questionnaire)
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
fun SintomasVisualesQuestionnaireScreen(
    onComplete: () -> Unit,
    onBack: () -> Unit,
    viewModel: SintomasVisualesViewModel = viewModel()
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
                title = { Text("Síntomas Visuales") },
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
                        text = "Evalúa el síndrome visual informático y fatiga ocular. 14 preguntas, 4-5 minutos.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Escala: 0=Nunca, 1=Rara vez, 2=Ocasionalmente, 3=Frecuentemente, 4=Muy frecuentemente, 5=Constantemente",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // MOLESTIAS OCULARES
            SectionHeader("Molestias Oculares")

            // 52. Ojos secos
            QuestionTitle("52. Ojos secos")
            Text("Frecuencia:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
            ScaleQuestion(
                value = viewModel.ojosSecosFrecuencia,
                onValueChange = { viewModel.ojosSecosFrecuencia = it }
            )
            Text("Intensidad:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
            ScaleQuestion(
                value = viewModel.ojosSecosIntensidad,
                onValueChange = { viewModel.ojosSecosIntensidad = it }
            )

            // 53. Ardor
            QuestionTitle("53. Sensación de ardor en los ojos")
            Text("Frecuencia:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
            ScaleQuestion(
                value = viewModel.ardorOjosFrecuencia,
                onValueChange = { viewModel.ardorOjosFrecuencia = it }
            )
            Text("Intensidad:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
            ScaleQuestion(
                value = viewModel.ardorOjosIntensidad,
                onValueChange = { viewModel.ardorOjosIntensidad = it }
            )

            // 54. Ojos rojos
            QuestionTitle("54. Ojos rojos/irritados")
            Text("Frecuencia:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
            ScaleQuestion(
                value = viewModel.ojosRojosFrecuencia,
                onValueChange = { viewModel.ojosRojosFrecuencia = it }
            )

            // 55. Lagrimeo
            QuestionTitle("55. Lagrimeo excesivo")
            Text("Frecuencia:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
            ScaleQuestion(
                value = viewModel.lagrimeoFrecuencia,
                onValueChange = { viewModel.lagrimeoFrecuencia = it }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // PROBLEMAS VISUALES
            SectionHeader("Problemas Visuales")

            // 56. Visión borrosa
            QuestionTitle("56. Visión borrosa temporal")
            Text("Frecuencia:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
            ScaleQuestion(
                value = viewModel.visionBorrosaFrecuencia,
                onValueChange = { viewModel.visionBorrosaFrecuencia = it }
            )

            // 57. Dificultad para enfocar
            QuestionTitle("57. Dificultad para enfocar (cambiar entre distancias)")
            Text("Frecuencia:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
            ScaleQuestion(
                value = viewModel.dificultadEnfocarFrecuencia,
                onValueChange = { viewModel.dificultadEnfocarFrecuencia = it }
            )

            // 58. Sensibilidad a la luz
            QuestionTitle("58. Sensibilidad a la luz (fotofobia)")
            Text("Frecuencia:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
            ScaleQuestion(
                value = viewModel.sensibilidadLuzFrecuencia,
                onValueChange = { viewModel.sensibilidadLuzFrecuencia = it }
            )

            // 59. Visión doble
            QuestionTitle("59. Visión doble ocasional")
            Text("Frecuencia:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
            ScaleQuestion(
                value = viewModel.visionDobleFrecuencia,
                onValueChange = { viewModel.visionDobleFrecuencia = it }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // FATIGA VISUAL
            SectionHeader("Fatiga Visual")

            // 60. Ojos cansados
            QuestionTitle("60. ¿Sientes los ojos cansados al final del día?")
            SingleChoiceQuestion(
                options = listOf("Siempre", "Frecuentemente", "A veces", "Nunca"),
                selectedOption = viewModel.ojosCansadosFinDia,
                onOptionSelected = { viewModel.ojosCansadosFinDia = it }
            )

            // 61. Esfuerzo para ver
            QuestionTitle("61. ¿Necesitas hacer más esfuerzo para ver nítidamente?")
            SingleChoiceQuestion(
                options = listOf(
                    "Sí, constantemente",
                    "Sí, al final del día",
                    "Ocasionalmente",
                    "No"
                ),
                selectedOption = viewModel.esfuerzoVerNitidamente,
                onOptionSelected = { viewModel.esfuerzoVerNitidamente = it }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // CUIDADO VISUAL
            SectionHeader("Cuidado Visual")

            // 62. Uso de lentes
            QuestionTitle("62. ¿Usas lentes?")
            SingleChoiceQuestion(
                options = listOf(
                    "Sí, graduados (miopía/hipermetropía/astigmatismo)",
                    "Sí, para leer (presbicia)",
                    "Sí, con filtro de luz azul",
                    "Sí, lentes de contacto",
                    "No uso"
                ),
                selectedOption = viewModel.usaLentes,
                onOptionSelected = { viewModel.usaLentes = it }
            )

            // 63. Último examen
            QuestionTitle("63. ¿Cuándo fue tu último examen visual?")
            SingleChoiceQuestion(
                options = listOf(
                    "Hace menos de 6 meses",
                    "Hace 6-12 meses",
                    "Hace 1-2 años",
                    "Hace más de 2 años",
                    "Nunca"
                ),
                selectedOption = viewModel.ultimoExamenVisual,
                onOptionSelected = { viewModel.ultimoExamenVisual = it }
            )

            // 64. Regla 20-20-20
            QuestionTitle("64. ¿Aplicas la regla 20-20-20?")
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
                )
            ) {
                Text(
                    text = "Cada 20 minutos, mira a 20 pies (6m) de distancia por 20 segundos",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(12.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            SingleChoiceQuestion(
                options = listOf(
                    "Sí, siempre",
                    "A veces",
                    "Rara vez",
                    "No sé qué es/Nunca"
                ),
                selectedOption = viewModel.aplicaRegla202020,
                onOptionSelected = { viewModel.aplicaRegla202020 = it }
            )

            // 65. Brillo de pantalla
            QuestionTitle("65. Brillo de tu pantalla")
            SingleChoiceQuestion(
                options = listOf(
                    "Ajustado a la iluminación ambiente",
                    "Muy brillante",
                    "Muy tenue",
                    "No lo ajusto"
                ),
                selectedOption = viewModel.brilloPantalla,
                onOptionSelected = { viewModel.brilloPantalla = it }
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