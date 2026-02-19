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
class BalanceVidaTrabajoViewModel : ViewModel() {
    private val repository = AuthRepository()

    private val _state = MutableStateFlow<QuestionnaireState>(QuestionnaireState.Idle)
    val state: StateFlow<QuestionnaireState> = _state.asStateFlow()

    var equilibrioTrabajoVida by mutableStateOf("")
    var tiempoLibreCalidad by mutableStateOf("")
    var actividadesRecreativas by mutableStateOf("")
    var trabajoAfectaRelaciones by mutableStateOf("")
    var tiempoFamiliaAmigos by mutableStateOf("")
    var puedeDesconectarseDiasLibres by mutableStateOf("")
    var revisaCorreosVacaciones by mutableStateOf("")
    var ultimasVacaciones by mutableStateOf("")

    fun isFormValid(): Boolean {
        return equilibrioTrabajoVida.isNotEmpty() &&
                tiempoLibreCalidad.isNotEmpty() &&
                actividadesRecreativas.isNotEmpty() &&
                trabajoAfectaRelaciones.isNotEmpty() &&
                tiempoFamiliaAmigos.isNotEmpty() &&
                puedeDesconectarseDiasLibres.isNotEmpty() &&
                revisaCorreosVacaciones.isNotEmpty() &&
                ultimasVacaciones.isNotEmpty()
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
            val questionnaire = BalanceVidaTrabajoQuestionnaire(
                userId = userId,
                equilibrioTrabajoVida = equilibrioTrabajoVida,
                tiempoLibreCalidad = tiempoLibreCalidad,
                actividadesRecreativas = actividadesRecreativas,
                trabajoAfectaRelaciones = trabajoAfectaRelaciones,
                tiempoFamiliaAmigos = tiempoFamiliaAmigos,
                puedeDesconectarseDiasLibres = puedeDesconectarseDiasLibres,
                revisaCorreosVacaciones = revisaCorreosVacaciones,
                ultimasVacaciones = ultimasVacaciones
            )
            val result = repository.saveBalanceVidaTrabajoQuestionnaire(questionnaire)
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
fun BalanceVidaTrabajoQuestionnaireScreen(
    onComplete: () -> Unit,
    onBack: () -> Unit,
    viewModel: BalanceVidaTrabajoViewModel = viewModel()
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
                title = { Text("Trabajo y vida personal") },
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
                        text = "Cuéntanos si sientes que el trabajo te deja tiempo y energía para disfrutar tu vida personal. 8 preguntas, 3-4 minutos.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── Equilibrio personal ───────────────────────────────────────
            SectionHeader("¿Cómo está tu equilibrio?")

            QuestionTitle("¿Sientes que tienes un buen balance entre el trabajo y tu vida personal?")
            SingleChoiceQuestion(
                options = listOf(
                    "Sí, excelente balance",
                    "Sí, buen balance",
                    "Más o menos equilibrado",
                    "El trabajo me quita más tiempo del que quisiera",
                    "El trabajo domina por completo mi vida"
                ),
                selectedOption = viewModel.equilibrioTrabajoVida,
                onOptionSelected = { viewModel.equilibrioTrabajoVida = it }
            )

            QuestionTitle("¿Cuánto tiempo libre de calidad tienes a la semana, sin pensar en el trabajo?")
            SingleChoiceQuestion(
                options = listOf(
                    "Más de 20 horas",
                    "Entre 15 y 20 horas",
                    "Entre 10 y 15 horas",
                    "Entre 5 y 10 horas",
                    "Menos de 5 horas"
                ),
                selectedOption = viewModel.tiempoLibreCalidad,
                onOptionSelected = { viewModel.tiempoLibreCalidad = it }
            )

            QuestionTitle("¿Tienes tiempo para pasatiempos o actividades que disfrutes?")
            SingleChoiceQuestion(
                options = listOf(
                    "Sí, varias veces a la semana",
                    "Sí, una vez a la semana",
                    "De vez en cuando (1 o 2 veces al mes)",
                    "Rara vez",
                    "No, no tengo tiempo para nada"
                ),
                selectedOption = viewModel.actividadesRecreativas,
                onOptionSelected = { viewModel.actividadesRecreativas = it }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ── Relaciones personales ─────────────────────────────────────
            SectionHeader("Familia y amigos")

            QuestionTitle("¿El trabajo afecta negativamente tus relaciones con tu familia o amigos?")
            SingleChoiceQuestion(
                options = listOf(
                    "No, para nada",
                    "Un poco",
                    "Moderadamente",
                    "Bastante",
                    "Mucho, ha afectado mis relaciones seriamente"
                ),
                selectedOption = viewModel.trabajoAfectaRelaciones,
                onOptionSelected = { viewModel.trabajoAfectaRelaciones = it }
            )

            QuestionTitle("¿Cuánto tiempo de calidad pasas con tu familia o amigos a la semana?")
            SingleChoiceQuestion(
                options = listOf(
                    "Más de 10 horas",
                    "Entre 5 y 10 horas",
                    "Entre 2 y 5 horas",
                    "Menos de 2 horas",
                    "Casi ninguno"
                ),
                selectedOption = viewModel.tiempoFamiliaAmigos,
                onOptionSelected = { viewModel.tiempoFamiliaAmigos = it }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ── Desconexión del trabajo ────────────────────────────────────
            SectionHeader("¿Puedes desconectarte del trabajo?")

            QuestionTitle("En tus días libres, ¿logras desconectarte completamente del trabajo?")
            SingleChoiceQuestion(
                options = listOf(
                    "Sí, sin problema",
                    "Sí, la mayor parte del tiempo",
                    "Me cuesta, pero a veces lo logro",
                    "Rara vez lo consigo",
                    "Nunca puedo desconectarme"
                ),
                selectedOption = viewModel.puedeDesconectarseDiasLibres,
                onOptionSelected = { viewModel.puedeDesconectarseDiasLibres = it }
            )

            QuestionTitle("Cuando estás de vacaciones, ¿revisas correos o mensajes del trabajo?")
            SingleChoiceQuestion(
                options = listOf(
                    "No tomo vacaciones",
                    "Nunca, me desconecto totalmente",
                    "Rara vez",
                    "De vez en cuando",
                    "Con frecuencia",
                    "Constantemente, casi como si trabajara"
                ),
                selectedOption = viewModel.revisaCorreosVacaciones,
                onOptionSelected = { viewModel.revisaCorreosVacaciones = it }
            )

            QuestionTitle("¿Cuándo fue la última vez que tomaste vacaciones sin pensar en el trabajo?")
            SingleChoiceQuestion(
                options = listOf(
                    "En los últimos 6 meses",
                    "Hace entre 6 y 12 meses",
                    "Hace entre 1 y 2 años",
                    "Hace más de 2 años",
                    "Nunca o no lo recuerdo"
                ),
                selectedOption = viewModel.ultimasVacaciones,
                onOptionSelected = { viewModel.ultimasVacaciones = it }
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