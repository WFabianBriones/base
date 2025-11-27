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
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.example.uleammed.auth.AuthRepository

// ViewModel
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
            // Información
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
                        text = "Evalúa tu espacio de trabajo, mobiliario y condiciones ambientales. 22 preguntas, 8-10 minutos.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // SECCIÓN 1: Mobiliario
            SectionHeader("Mobiliario y Equipamiento")

            // 13. Tipo de silla
            QuestionTitle("13. Tipo de silla que utilizas")
            SingleChoiceQuestion(
                options = listOf(
                    "Silla ergonómica ajustable (altura, respaldo, apoyabrazos)",
                    "Silla con ajuste de altura y respaldo",
                    "Silla con ajuste de altura solamente",
                    "Silla básica sin ajustes",
                    "Silla inadecuada (comedor, cocina, etc.)"
                ),
                selectedOption = viewModel.tipoSilla,
                onOptionSelected = { viewModel.tipoSilla = it }
            )

            // 14. Soporte lumbar
            QuestionTitle("14. ¿Tu silla tiene soporte lumbar?")
            SingleChoiceQuestion(
                options = listOf("Sí, ajustable", "Sí, fijo", "No tiene"),
                selectedOption = viewModel.soporteLumbar,
                onOptionSelected = { viewModel.soporteLumbar = it }
            )

            // 15. Altura del escritorio
            QuestionTitle("15. Altura del escritorio/mesa")
            SingleChoiceQuestion(
                options = listOf(
                    "Altura ajustable",
                    "Altura adecuada para mi estatura",
                    "Muy alto",
                    "Muy bajo",
                    "No tengo escritorio dedicado"
                ),
                selectedOption = viewModel.alturaEscritorio,
                onOptionSelected = { viewModel.alturaEscritorio = it }
            )

            // 16. Espacio en escritorio
            QuestionTitle("16. Espacio en el escritorio")
            SingleChoiceQuestion(
                options = listOf(
                    "Amplio (puedo extender brazos)",
                    "Adecuado",
                    "Limitado",
                    "Muy reducido"
                ),
                selectedOption = viewModel.espacioEscritorio,
                onOptionSelected = { viewModel.espacioEscritorio = it }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // SECCIÓN 2: Configuración del Monitor
            SectionHeader("Configuración del Monitor")

            // 17. Tipo de monitor
            QuestionTitle("17. Tipo de monitor principal")
            SingleChoiceQuestion(
                options = listOf(
                    "Monitor externo de escritorio (19\" o más)",
                    "Laptop con monitor externo adicional",
                    "Solo laptop (con soporte elevado)",
                    "Solo laptop (sin soporte)",
                    "Tablet/iPad"
                ),
                selectedOption = viewModel.tipoMonitor,
                onOptionSelected = { viewModel.tipoMonitor = it }
            )

            // 18. Altura del monitor
            QuestionTitle("18. Altura del monitor respecto a tus ojos")
            SingleChoiceQuestion(
                options = listOf(
                    "A la altura de los ojos (correcto)",
                    "10-15cm por debajo de los ojos (correcto)",
                    "Por encima de los ojos",
                    "Más de 15cm por debajo de los ojos",
                    "Varía constantemente"
                ),
                selectedOption = viewModel.alturaMonitor,
                onOptionSelected = { viewModel.alturaMonitor = it }
            )

            // 19. Distancia del monitor
            QuestionTitle("19. Distancia del monitor a tus ojos")
            SingleChoiceQuestion(
                options = listOf(
                    "50-70 cm (longitud de brazo) - correcto",
                    "Menos de 50 cm (muy cerca)",
                    "Más de 70 cm (muy lejos)",
                    "No sé/No lo he medido"
                ),
                selectedOption = viewModel.distanciaMonitor,
                onOptionSelected = { viewModel.distanciaMonitor = it }
            )

            // 20. Múltiples monitores
            QuestionTitle("20. ¿Usas más de un monitor?")
            SingleChoiceQuestion(
                options = listOf(
                    "No, solo uno",
                    "Sí, dos monitores",
                    "Sí, tres o más"
                ),
                selectedOption = viewModel.usaMasDeUnMonitor,
                onOptionSelected = { viewModel.usaMasDeUnMonitor = it }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // SECCIÓN 3: Teclado y Mouse
            SectionHeader("Teclado y Mouse")

            // 21. Posición del teclado
            QuestionTitle("21. Posición del teclado respecto a tus codos")
            SingleChoiceQuestion(
                options = listOf(
                    "A la altura de los codos (codos en 90°)",
                    "Por encima de los codos",
                    "Por debajo de los codos",
                    "Varía constantemente"
                ),
                selectedOption = viewModel.posicionTeclado,
                onOptionSelected = { viewModel.posicionTeclado = it }
            )

            // 22. Tipo de mouse
            QuestionTitle("22. Tipo de mouse")
            SingleChoiceQuestion(
                options = listOf(
                    "Mouse ergonómico vertical",
                    "Mouse estándar",
                    "Trackpad de laptop",
                    "Mouse inalámbrico estándar"
                ),
                selectedOption = viewModel.tipoMouse,
                onOptionSelected = { viewModel.tipoMouse = it }
            )

            // 23. Almohadilla
            QuestionTitle("23. ¿Usas almohadilla de apoyo para muñeca?")
            SingleChoiceQuestion(
                options = listOf(
                    "Sí, para teclado y mouse",
                    "Sí, solo para mouse",
                    "Sí, solo para teclado",
                    "No uso"
                ),
                selectedOption = viewModel.usaAlmohadilla,
                onOptionSelected = { viewModel.usaAlmohadilla = it }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // SECCIÓN 4: Iluminación
            SectionHeader("Iluminación y Ambiente")

            // 24. Iluminación principal
            QuestionTitle("24. Iluminación principal del espacio")
            SingleChoiceQuestion(
                options = listOf(
                    "Luz natural abundante (ventana grande)",
                    "Luz natural moderada",
                    "Luz artificial (LED blanco/neutro)",
                    "Luz artificial (amarilla/cálida)",
                    "Mezcla de natural y artificial",
                    "Insuficiente/Tenue"
                ),
                selectedOption = viewModel.iluminacionPrincipal,
                onOptionSelected = { viewModel.iluminacionPrincipal = it }
            )

            // 25. Reflejos
            QuestionTitle("25. ¿Hay reflejos en tu pantalla?")
            SingleChoiceQuestion(
                options = listOf("Nunca", "Ocasionalmente", "Frecuentemente", "Constantemente"),
                selectedOption = viewModel.reflejosPantalla,
                onOptionSelected = { viewModel.reflejosPantalla = it }
            )

            // 26. Lámpara de escritorio
            QuestionTitle("26. ¿Usas lámpara de escritorio adicional?")
            SingleChoiceQuestion(
                options = listOf("Sí, ajustable", "Sí, fija", "No"),
                selectedOption = viewModel.lamparaEscritorio,
                onOptionSelected = { viewModel.lamparaEscritorio = it }
            )

            // 27. Temperatura
            QuestionTitle("27. Temperatura del espacio de trabajo")
            SingleChoiceQuestion(
                options = listOf(
                    "Confortable",
                    "Frío frecuentemente",
                    "Calor frecuentemente",
                    "Varía mucho"
                ),
                selectedOption = viewModel.temperatura,
                onOptionSelected = { viewModel.temperatura = it }
            )

            // 28. Ruido
            QuestionTitle("28. Nivel de ruido")
            SingleChoiceQuestion(
                options = listOf("Silencioso", "Ruido moderado", "Ruidoso", "Muy ruidoso"),
                selectedOption = viewModel.nivelRuido,
                onOptionSelected = { viewModel.nivelRuido = it }
            )

            // 29. Ventilación
            QuestionTitle("29. Ventilación del espacio")
            SingleChoiceQuestion(
                options = listOf(
                    "Excelente (aire fresco)",
                    "Buena",
                    "Regular (algo cargado)",
                    "Mala (aire viciado)"
                ),
                selectedOption = viewModel.ventilacion,
                onOptionSelected = { viewModel.ventilacion = it }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // SECCIÓN 5: Pausas
            SectionHeader("Pausas y Movimiento")

            // 30. Pausas activas
            QuestionTitle("30. ¿Realizas pausas activas durante tu jornada?")
            SingleChoiceQuestion(
                options = listOf(
                    "Sí, cada 30-60 minutos",
                    "Sí, cada 1-2 horas",
                    "Sí, cada 3-4 horas",
                    "Solo cuando voy al baño",
                    "Nunca/Muy rara vez"
                ),
                selectedOption = viewModel.pausasActivas,
                onOptionSelected = { viewModel.pausasActivas = it }
            )

            // 31. Duración pausas
            QuestionTitle("31. Duración típica de las pausas")
            SingleChoiceQuestion(
                options = listOf(
                    "5-10 minutos",
                    "2-5 minutos",
                    "Menos de 2 minutos",
                    "No hago pausas"
                ),
                selectedOption = viewModel.duracionPausas,
                onOptionSelected = { viewModel.duracionPausas = it }
            )

            // 32. Estiramientos
            QuestionTitle("32. Durante las pausas, ¿realizas estiramientos?")
            SingleChoiceQuestion(
                options = listOf("Sí, siempre", "A veces", "Rara vez", "Nunca"),
                selectedOption = viewModel.realizaEstiramientos,
                onOptionSelected = { viewModel.realizaEstiramientos = it }
            )

            // 33. Tiempo sentado
            QuestionTitle("33. Tiempo promedio continuo sentado sin levantarte")
            SingleChoiceQuestion(
                options = listOf(
                    "Menos de 1 hora",
                    "1-2 horas",
                    "2-3 horas",
                    "Más de 3 horas"
                ),
                selectedOption = viewModel.tiempoSentadoContinuo,
                onOptionSelected = { viewModel.tiempoSentadoContinuo = it }
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