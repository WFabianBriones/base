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

// ViewModel
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
                title = { Text("Balance Vida-Trabajo") },
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
                        text = "Evalúa el equilibrio entre tu vida personal y profesional. 8 preguntas, 3-4 minutos.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // EQUILIBRIO PERSONAL
            SectionHeader("Equilibrio Personal")

            // 133. Equilibrio vida-trabajo
            QuestionTitle("133. ¿Sientes que tienes un buen equilibrio entre trabajo y vida personal?")
            SingleChoiceQuestion(
                options = listOf(
                    "Sí, excelente balance",
                    "Sí, buen balance",
                    "Parcialmente equilibrado",
                    "Más trabajo que vida personal",
                    "El trabajo domina completamente mi vida"
                ),
                selectedOption = viewModel.equilibrioTrabajoVida,
                onOptionSelected = { viewModel.equilibrioTrabajoVida = it }
            )

            // 134. Tiempo libre
            QuestionTitle("134. Tiempo libre de calidad por semana (sin pensar en trabajo)")
            SingleChoiceQuestion(
                options = listOf(
                    "Más de 20 horas",
                    "15-20 horas",
                    "10-15 horas",
                    "5-10 horas",
                    "Menos de 5 horas"
                ),
                selectedOption = viewModel.tiempoLibreCalidad,
                onOptionSelected = { viewModel.tiempoLibreCalidad = it }
            )

            // 135. Actividades recreativas
            QuestionTitle("135. ¿Realizas actividades recreativas/hobbies regularmente?")
            SingleChoiceQuestion(
                options = listOf(
                    "Sí, varias veces por semana",
                    "Sí, una vez por semana",
                    "Ocasionalmente (1-2 veces al mes)",
                    "Rara vez",
                    "Nunca, no tengo tiempo"
                ),
                selectedOption = viewModel.actividadesRecreativas,
                onOptionSelected = { viewModel.actividadesRecreativas = it }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // RELACIONES PERSONALES
            SectionHeader("Relaciones Personales")

            // 136. Afecta relaciones
            QuestionTitle("136. ¿Tu trabajo afecta negativamente tus relaciones personales/familiares?")
            SingleChoiceQuestion(
                options = listOf(
                    "No, para nada",
                    "Un poco",
                    "Moderadamente",
                    "Bastante",
                    "Severamente"
                ),
                selectedOption = viewModel.trabajoAfectaRelaciones,
                onOptionSelected = { viewModel.trabajoAfectaRelaciones = it }
            )

            // 137. Tiempo con familia/amigos
            QuestionTitle("137. Tiempo de calidad con familia/amigos por semana")
            SingleChoiceQuestion(
                options = listOf(
                    "Más de 10 horas",
                    "5-10 horas",
                    "2-5 horas",
                    "Menos de 2 horas",
                    "Casi ninguno"
                ),
                selectedOption = viewModel.tiempoFamiliaAmigos,
                onOptionSelected = { viewModel.tiempoFamiliaAmigos = it }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // DESCONEXIÓN LABORAL
            SectionHeader("Desconexión Laboral")

            // 138. Desconectar días libres
            QuestionTitle("138. ¿Puedes desconectarte completamente del trabajo en tus días libres?")
            SingleChoiceQuestion(
                options = listOf(
                    "Sí, completamente",
                    "Mayormente sí",
                    "Con dificultad",
                    "Rara vez",
                    "Nunca puedo desconectar"
                ),
                selectedOption = viewModel.puedeDesconectarseDiasLibres,
                onOptionSelected = { viewModel.puedeDesconectarseDiasLibres = it }
            )

            // 139. Correos en vacaciones
            QuestionTitle("139. ¿Revisas correos/mensajes de trabajo en vacaciones?")
            SingleChoiceQuestion(
                options = listOf(
                    "No tomo vacaciones",
                    "Nunca",
                    "Rara vez",
                    "Ocasionalmente",
                    "Frecuentemente",
                    "Constantemente"
                ),
                selectedOption = viewModel.revisaCorreosVacaciones,
                onOptionSelected = { viewModel.revisaCorreosVacaciones = it }
            )

            // 140. Últimas vacaciones
            QuestionTitle("140. Última vez que tomaste vacaciones completas (sin pensar en trabajo)")
            SingleChoiceQuestion(
                options = listOf(
                    "En los últimos 6 meses",
                    "Hace 6-12 meses",
                    "Hace 1-2 años",
                    "Hace más de 2 años",
                    "Nunca/No recuerdo"
                ),
                selectedOption = viewModel.ultimasVacaciones,
                onOptionSelected = { viewModel.ultimasVacaciones = it }
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