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
                title = { Text("Molestias en los ojos") },
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
                        text = "Cuéntanos sobre molestias en los ojos o problemas para ver que puedas tener por el uso de pantallas. 16 preguntas, 5-7 minutos.",
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

            // ── Molestias oculares ───────────────────────────────────────
            SectionHeader("Molestias en los ojos")

            QuestionTitle("¿Sientes los ojos secos mientras trabajas?")
            Text("Frecuencia:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
            ScaleQuestion(value = viewModel.ojosSecosFrecuencia, onValueChange = { viewModel.ojosSecosFrecuencia = it })
            Text("¿Qué tan molesto es?", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
            ScaleQuestion(value = viewModel.ojosSecosIntensidad, onValueChange = { viewModel.ojosSecosIntensidad = it })

            QuestionTitle("¿Sientes ardor o picazón en los ojos?")
            Text("Frecuencia:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
            ScaleQuestion(value = viewModel.ardorOjosFrecuencia, onValueChange = { viewModel.ardorOjosFrecuencia = it })
            Text("¿Qué tan molesto es?", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
            ScaleQuestion(value = viewModel.ardorOjosIntensidad, onValueChange = { viewModel.ardorOjosIntensidad = it })

            QuestionTitle("¿Se te ponen los ojos rojos o irritados?")
            Text("Frecuencia:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
            ScaleQuestion(value = viewModel.ojosRojosFrecuencia, onValueChange = { viewModel.ojosRojosFrecuencia = it })

            QuestionTitle("¿Se te llenan los ojos de lágrimas sin querer?")
            Text("Frecuencia:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
            ScaleQuestion(value = viewModel.lagrimeoFrecuencia, onValueChange = { viewModel.lagrimeoFrecuencia = it })

            Spacer(modifier = Modifier.height(16.dp))

            // ── Problemas para ver ────────────────────────────────────────
            SectionHeader("Problemas para ver")

            QuestionTitle("¿Se te nubla la vista por momentos?")
            Text("Frecuencia:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
            ScaleQuestion(value = viewModel.visionBorrosaFrecuencia, onValueChange = { viewModel.visionBorrosaFrecuencia = it })

            QuestionTitle("¿Te cuesta enfocar cuando cambias la vista de la pantalla a algo lejano (o al revés)?")
            Text("Frecuencia:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
            ScaleQuestion(value = viewModel.dificultadEnfocarFrecuencia, onValueChange = { viewModel.dificultadEnfocarFrecuencia = it })

            QuestionTitle("¿Te molesta mucho la luz (te encandila o irrita)?")
            Text("Frecuencia:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
            ScaleQuestion(value = viewModel.sensibilidadLuzFrecuencia, onValueChange = { viewModel.sensibilidadLuzFrecuencia = it })

            QuestionTitle("¿Ves doble de forma ocasional?")
            Text("Frecuencia:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
            ScaleQuestion(value = viewModel.visionDobleFrecuencia, onValueChange = { viewModel.visionDobleFrecuencia = it })

            Spacer(modifier = Modifier.height(16.dp))

            // ── Cansancio visual ─────────────────────────────────────────
            SectionHeader("Cansancio visual")

            QuestionTitle("¿Sientes los ojos cansados o pesados al terminar el día?")
            SingleChoiceQuestion(
                options = listOf("Siempre", "Con frecuencia", "A veces", "Nunca"),
                selectedOption = viewModel.ojosCansadosFinDia,
                onOptionSelected = { viewModel.ojosCansadosFinDia = it }
            )

            QuestionTitle("¿Tienes que esforzarte más de lo normal para ver con claridad?")
            SingleChoiceQuestion(
                options = listOf(
                    "Sí, todo el tiempo",
                    "Sí, sobre todo al final del día",
                    "De vez en cuando",
                    "No, veo bien sin esfuerzo"
                ),
                selectedOption = viewModel.esfuerzoVerNitidamente,
                onOptionSelected = { viewModel.esfuerzoVerNitidamente = it }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ── Cuidado de la vista ───────────────────────────────────────
            SectionHeader("Cuidado de la vista")

            QuestionTitle("¿Usas lentes o gafas?")
            SingleChoiceQuestion(
                options = listOf(
                    "Sí, para ver de lejos o de cerca (graduados)",
                    "Sí, solo para leer",
                    "Sí, con filtro de luz azul",
                    "Sí, lentes de contacto",
                    "No uso lentes"
                ),
                selectedOption = viewModel.usaLentes,
                onOptionSelected = { viewModel.usaLentes = it }
            )

            QuestionTitle("¿Cuándo fue tu último examen de la vista?")
            SingleChoiceQuestion(
                options = listOf(
                    "Hace menos de 6 meses",
                    "Hace entre 6 y 12 meses",
                    "Hace entre 1 y 2 años",
                    "Hace más de 2 años",
                    "Nunca me he hecho uno"
                ),
                selectedOption = viewModel.ultimoExamenVisual,
                onOptionSelected = { viewModel.ultimoExamenVisual = it }
            )

            QuestionTitle("¿Aplicas la regla 20-20-20? (Cada 20 minutos, mira algo a 20 pies de distancia durante 20 segundos)")
            SingleChoiceQuestion(
                options = listOf(
                    "Sí, lo hago regularmente",
                    "A veces lo recuerdo",
                    "Rara vez",
                    "No sabía de esa regla",
                    "No lo hago"
                ),
                selectedOption = viewModel.aplicaRegla202020,
                onOptionSelected = { viewModel.aplicaRegla202020 = it }
            )

            QuestionTitle("¿Cómo tienes configurado el brillo de tu pantalla?")
            SingleChoiceQuestion(
                options = listOf(
                    "Ajustado según la luz del ambiente",
                    "Siempre en brillo alto",
                    "Siempre en brillo bajo",
                    "No lo he ajustado nunca"
                ),
                selectedOption = viewModel.brilloPantalla,
                onOptionSelected = { viewModel.brilloPantalla = it }
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