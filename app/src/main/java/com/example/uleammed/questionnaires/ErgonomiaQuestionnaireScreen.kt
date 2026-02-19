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
class ErgonomiaViewModel : ViewModel() {
    private val repository = AuthRepository()

    private val _state = MutableStateFlow<QuestionnaireState>(QuestionnaireState.Idle)
    val state: StateFlow<QuestionnaireState> = _state.asStateFlow()

    var tipoSilla by mutableStateOf("")
    var soporteLumbar by mutableStateOf("")
    var alturaEscritorio by mutableStateOf("")
    var espacioEscritorio by mutableStateOf("")
    var tipoMonitor by mutableStateOf("")
    var alturaMonitor by mutableStateOf("")
    var distanciaMonitor by mutableStateOf("")
    var usaMasDeUnMonitor by mutableStateOf("")
    var posicionTeclado by mutableStateOf("")
    var tipoMouse by mutableStateOf("")
    var usaAlmohadilla by mutableStateOf("")
    var iluminacionPrincipal by mutableStateOf("")
    var reflejosPantalla by mutableStateOf("")
    var lamparaEscritorio by mutableStateOf("")
    var temperatura by mutableStateOf("")
    var nivelRuido by mutableStateOf("")
    var ventilacion by mutableStateOf("")
    var pausasActivas by mutableStateOf("")
    var duracionPausas by mutableStateOf("")
    var realizaEstiramientos by mutableStateOf("")
    var tiempoSentadoContinuo by mutableStateOf("")

    fun isFormValid(): Boolean {
        return tipoSilla.isNotEmpty() &&
                soporteLumbar.isNotEmpty() &&
                alturaEscritorio.isNotEmpty() &&
                espacioEscritorio.isNotEmpty() &&
                tipoMonitor.isNotEmpty() &&
                alturaMonitor.isNotEmpty() &&
                distanciaMonitor.isNotEmpty() &&
                usaMasDeUnMonitor.isNotEmpty() &&
                posicionTeclado.isNotEmpty() &&
                tipoMouse.isNotEmpty() &&
                usaAlmohadilla.isNotEmpty() &&
                iluminacionPrincipal.isNotEmpty() &&
                reflejosPantalla.isNotEmpty() &&
                lamparaEscritorio.isNotEmpty() &&
                temperatura.isNotEmpty() &&
                nivelRuido.isNotEmpty() &&
                ventilacion.isNotEmpty() &&
                pausasActivas.isNotEmpty() &&
                duracionPausas.isNotEmpty() &&
                realizaEstiramientos.isNotEmpty() &&
                tiempoSentadoContinuo.isNotEmpty()
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
            val questionnaire = ErgonomiaQuestionnaire(
                userId = userId,
                tipoSilla = tipoSilla,
                soporteLumbar = soporteLumbar,
                alturaEscritorio = alturaEscritorio,
                espacioEscritorio = espacioEscritorio,
                tipoMonitor = tipoMonitor,
                alturaMonitor = alturaMonitor,
                distanciaMonitor = distanciaMonitor,
                usaMasDeUnMonitor = usaMasDeUnMonitor,
                posicionTeclado = posicionTeclado,
                tipoMouse = tipoMouse,
                usaAlmohadilla = usaAlmohadilla,
                iluminacionPrincipal = iluminacionPrincipal,
                reflejosPantalla = reflejosPantalla,
                lamparaEscritorio = lamparaEscritorio,
                temperatura = temperatura,
                nivelRuido = nivelRuido,
                ventilacion = ventilacion,
                pausasActivas = pausasActivas,
                duracionPausas = duracionPausas,
                realizaEstiramientos = realizaEstiramientos,
                tiempoSentadoContinuo = tiempoSentadoContinuo
            )
            val result = repository.saveErgonomiaQuestionnaire(questionnaire)
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
fun ErgonomiaQuestionnaireScreen(
    onComplete: () -> Unit,
    onBack: () -> Unit,
    viewModel: ErgonomiaViewModel = viewModel()
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
                title = { Text("Ergonomía y Ambiente") },
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
                        text = "Cuéntanos cómo está organizado tu puesto de trabajo: muebles, pantalla y condiciones del ambiente. 22 preguntas, 8-10 minutos.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── SECCIÓN 1: Silla y escritorio ──────────────────────────────
            SectionHeader("Tu silla y escritorio")

            QuestionTitle("¿Qué tipo de silla usas para trabajar?")
            SingleChoiceQuestion(
                options = listOf(
                    "Silla ergonómica con ajuste de altura, respaldo y apoyabrazos",
                    "Silla con ajuste de altura y respaldo",
                    "Silla solo con ajuste de altura",
                    "Silla básica sin ningún ajuste",
                    "Silla inapropiada (comedor, taburete, etc.)"
                ),
                selectedOption = viewModel.tipoSilla,
                onOptionSelected = { viewModel.tipoSilla = it }
            )

            QuestionTitle("¿Tu silla tiene un apoyo para la parte baja de la espalda (zona lumbar)?")
            SingleChoiceQuestion(
                options = listOf("Sí, y se puede ajustar", "Sí, pero es fijo", "No tiene"),
                selectedOption = viewModel.soporteLumbar,
                onOptionSelected = { viewModel.soporteLumbar = it }
            )

            QuestionTitle("¿Cómo es la altura de tu escritorio o mesa de trabajo?")
            SingleChoiceQuestion(
                options = listOf(
                    "Se puede ajustar",
                    "Está a buena altura para mí",
                    "Está demasiado alto",
                    "Está demasiado bajo",
                    "No tengo un escritorio fijo"
                ),
                selectedOption = viewModel.alturaEscritorio,
                onOptionSelected = { viewModel.alturaEscritorio = it }
            )

            QuestionTitle("¿Tienes suficiente espacio en tu escritorio para trabajar cómodamente?")
            SingleChoiceQuestion(
                options = listOf(
                    "Sí, tengo mucho espacio",
                    "Sí, espacio suficiente",
                    "Poco espacio",
                    "Muy poco, me resulta incómodo"
                ),
                selectedOption = viewModel.espacioEscritorio,
                onOptionSelected = { viewModel.espacioEscritorio = it }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ── SECCIÓN 2: Pantalla ────────────────────────────────────────
            SectionHeader("Tu pantalla (monitor)")

            QuestionTitle("¿Qué pantalla usas principalmente para trabajar?")
            SingleChoiceQuestion(
                options = listOf(
                    "Monitor de escritorio externo (19\" o más)",
                    "Laptop con monitor externo adicional",
                    "Solo laptop elevada con soporte",
                    "Solo laptop sin soporte",
                    "Tablet o iPad"
                ),
                selectedOption = viewModel.tipoMonitor,
                onOptionSelected = { viewModel.tipoMonitor = it }
            )

            QuestionTitle("¿A qué altura está tu pantalla en relación con tus ojos?")
            SingleChoiceQuestion(
                options = listOf(
                    "A la altura de los ojos",
                    "Un poco por debajo de los ojos (correcto)",
                    "Por encima de los ojos",
                    "Mucho más abajo de los ojos",
                    "Cambia constantemente"
                ),
                selectedOption = viewModel.alturaMonitor,
                onOptionSelected = { viewModel.alturaMonitor = it }
            )

            QuestionTitle("¿A qué distancia está tu pantalla de tus ojos?")
            SingleChoiceQuestion(
                options = listOf(
                    "Entre 50 y 70 cm (la longitud de un brazo extendido) — lo ideal",
                    "Menos de 50 cm (muy cerca)",
                    "Más de 70 cm (muy lejos)",
                    "No lo sé / no lo he medido"
                ),
                selectedOption = viewModel.distanciaMonitor,
                onOptionSelected = { viewModel.distanciaMonitor = it }
            )

            QuestionTitle("¿Trabajas con más de una pantalla a la vez?")
            SingleChoiceQuestion(
                options = listOf("No, solo una", "Sí, dos pantallas", "Sí, tres o más"),
                selectedOption = viewModel.usaMasDeUnMonitor,
                onOptionSelected = { viewModel.usaMasDeUnMonitor = it }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ── SECCIÓN 3: Teclado y mouse ────────────────────────────────
            SectionHeader("Teclado y mouse")

            QuestionTitle("Cuando escribes, ¿el teclado está a la altura de tus codos?")
            SingleChoiceQuestion(
                options = listOf(
                    "Sí, a la altura de los codos (codos doblados en 90°)",
                    "Más alto que los codos",
                    "Más bajo que los codos",
                    "Varía todo el tiempo"
                ),
                selectedOption = viewModel.posicionTeclado,
                onOptionSelected = { viewModel.posicionTeclado = it }
            )

            QuestionTitle("¿Qué tipo de mouse usas?")
            SingleChoiceQuestion(
                options = listOf(
                    "Mouse ergonómico vertical",
                    "Mouse estándar",
                    "Trackpad de la laptop",
                    "Mouse inalámbrico estándar"
                ),
                selectedOption = viewModel.tipoMouse,
                onOptionSelected = { viewModel.tipoMouse = it }
            )

            QuestionTitle("¿Usas algún apoyo o cojín bajo las muñecas al escribir o usar el mouse?")
            SingleChoiceQuestion(
                options = listOf(
                    "Sí, para teclado y mouse",
                    "Sí, solo para el mouse",
                    "Sí, solo para el teclado",
                    "No uso ningún apoyo"
                ),
                selectedOption = viewModel.usaAlmohadilla,
                onOptionSelected = { viewModel.usaAlmohadilla = it }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ── SECCIÓN 4: Ambiente ───────────────────────────────────────
            SectionHeader("Luz, temperatura y ruido")

            QuestionTitle("¿Cómo es la iluminación en el lugar donde trabajas?")
            SingleChoiceQuestion(
                options = listOf(
                    "Mucha luz natural (ventana grande)",
                    "Algo de luz natural",
                    "Luz artificial blanca o neutra",
                    "Luz artificial amarilla o cálida",
                    "Mezcla de luz natural y artificial",
                    "Poca luz, está oscuro"
                ),
                selectedOption = viewModel.iluminacionPrincipal,
                onOptionSelected = { viewModel.iluminacionPrincipal = it }
            )

            QuestionTitle("¿Ves reflejos o brillos molestos en tu pantalla?")
            SingleChoiceQuestion(
                options = listOf("Nunca", "De vez en cuando", "Con frecuencia", "Siempre"),
                selectedOption = viewModel.reflejosPantalla,
                onOptionSelected = { viewModel.reflejosPantalla = it }
            )

            QuestionTitle("¿Tienes una lámpara extra en tu escritorio?")
            SingleChoiceQuestion(
                options = listOf("Sí, con ajuste de dirección", "Sí, fija", "No"),
                selectedOption = viewModel.lamparaEscritorio,
                onOptionSelected = { viewModel.lamparaEscritorio = it }
            )

            QuestionTitle("¿Cómo es la temperatura donde trabajas?")
            SingleChoiceQuestion(
                options = listOf(
                    "Cómoda la mayor parte del tiempo",
                    "Suele estar frío",
                    "Suele hacer calor",
                    "Cambia mucho"
                ),
                selectedOption = viewModel.temperatura,
                onOptionSelected = { viewModel.temperatura = it }
            )

            QuestionTitle("¿Cuánto ruido hay en tu espacio de trabajo?")
            SingleChoiceQuestion(
                options = listOf(
                    "Silencioso",
                    "Algo de ruido, pero soportable",
                    "Bastante ruidoso",
                    "Muy ruidoso, me distrae"
                ),
                selectedOption = viewModel.nivelRuido,
                onOptionSelected = { viewModel.nivelRuido = it }
            )

            QuestionTitle("¿Cómo es el aire en tu lugar de trabajo?")
            SingleChoiceQuestion(
                options = listOf(
                    "Muy fresco, con buena ventilación",
                    "Buena ventilación",
                    "Regular, a veces el aire se siente pesado",
                    "Malo, el aire está viciado"
                ),
                selectedOption = viewModel.ventilacion,
                onOptionSelected = { viewModel.ventilacion = it }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ── SECCIÓN 5: Pausas ────────────────────────────────────────
            SectionHeader("Pausas y movimiento")

            QuestionTitle("¿Con qué frecuencia te levantas o cambias de posición durante tu jornada?")
            SingleChoiceQuestion(
                options = listOf(
                    "Cada 30 a 60 minutos",
                    "Cada 1 o 2 horas",
                    "Cada 3 o 4 horas",
                    "Solo cuando voy al baño",
                    "Casi nunca me levanto"
                ),
                selectedOption = viewModel.pausasActivas,
                onOptionSelected = { viewModel.pausasActivas = it }
            )

            QuestionTitle("Cuando haces una pausa, ¿cuánto tiempo suele durar?")
            SingleChoiceQuestion(
                options = listOf(
                    "Entre 5 y 10 minutos",
                    "Entre 2 y 5 minutos",
                    "Menos de 2 minutos",
                    "No hago pausas"
                ),
                selectedOption = viewModel.duracionPausas,
                onOptionSelected = { viewModel.duracionPausas = it }
            )

            QuestionTitle("Durante las pausas, ¿haces estiramientos o ejercicios cortos?")
            SingleChoiceQuestion(
                options = listOf("Sí, siempre", "A veces", "Rara vez", "Nunca"),
                selectedOption = viewModel.realizaEstiramientos,
                onOptionSelected = { viewModel.realizaEstiramientos = it }
            )

            QuestionTitle("¿Cuánto tiempo seguido puedes estar sentado sin levantarte?")
            SingleChoiceQuestion(
                options = listOf(
                    "Menos de 1 hora",
                    "Entre 1 y 2 horas",
                    "Entre 2 y 3 horas",
                    "Más de 3 horas"
                ),
                selectedOption = viewModel.tiempoSentadoContinuo,
                onOptionSelected = { viewModel.tiempoSentadoContinuo = it }
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