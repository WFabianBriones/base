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
import com.example.uleammed.AuthRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// ViewModel
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
                title = { Text("Síntomas Músculo-Esqueléticos") },
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
                        text = "Evalúa la frecuencia e intensidad de síntomas en los últimos 3 meses. 18 preguntas, 6-8 minutos.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Escalas
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Escalas de Valoración:",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Frecuencia: 0=Nunca, 1=Rara vez, 2=Ocasionalmente, 3=Frecuentemente, 4=Muy frecuentemente, 5=Constantemente",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Intensidad: 0=Sin dolor, 1=Muy leve, 2=Leve, 3=Moderado, 4=Severo, 5=Muy severo",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // CUELLO Y HOMBROS
            SectionHeader("Cuello y Hombros")

            // 34. Dolor en cuello
            QuestionTitle("34. Dolor en cuello/nuca")
            Text("Frecuencia:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
            ScaleQuestion(
                value = viewModel.dolorCuelloFrecuencia,
                onValueChange = { viewModel.dolorCuelloFrecuencia = it }
            )
            Text("Intensidad:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
            ScaleQuestion(
                value = viewModel.dolorCuelloIntensidad,
                onValueChange = { viewModel.dolorCuelloIntensidad = it }
            )

            // 35. Rigidez de cuello
            QuestionTitle("35. Rigidez de cuello")
            Text("Frecuencia:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
            ScaleQuestion(
                value = viewModel.rigidezCuelloFrecuencia,
                onValueChange = { viewModel.rigidezCuelloFrecuencia = it }
            )
            Text("Intensidad:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
            ScaleQuestion(
                value = viewModel.rigidezCuelloIntensidad,
                onValueChange = { viewModel.rigidezCuelloIntensidad = it }
            )

            // 36. Dolor en hombros
            QuestionTitle("36. Dolor en hombros")
            Text("Frecuencia:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
            ScaleQuestion(
                value = viewModel.dolorHombrosFrecuencia,
                onValueChange = { viewModel.dolorHombrosFrecuencia = it }
            )
            Text("Intensidad:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
            ScaleQuestion(
                value = viewModel.dolorHombrosIntensidad,
                onValueChange = { viewModel.dolorHombrosIntensidad = it }
            )

            // 37. Dolor aumenta al final del día
            QuestionTitle("37. ¿El dolor aumenta al final del día?")
            SingleChoiceQuestion(
                options = listOf(
                    "Sí, significativamente",
                    "Sí, un poco",
                    "No cambia",
                    "No aplica (no tengo dolor)"
                ),
                selectedOption = viewModel.dolorAumentaFinDia,
                onOptionSelected = { viewModel.dolorAumentaFinDia = it }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ESPALDA
            SectionHeader("Espalda")

            // 38. Dolor espalda alta
            QuestionTitle("38. Dolor en espalda alta (dorsal/entre omóplatos)")
            Text("Frecuencia:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
            ScaleQuestion(
                value = viewModel.dolorEspaldaAltaFrecuencia,
                onValueChange = { viewModel.dolorEspaldaAltaFrecuencia = it }
            )
            Text("Intensidad:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
            ScaleQuestion(
                value = viewModel.dolorEspaldaAltaIntensidad,
                onValueChange = { viewModel.dolorEspaldaAltaIntensidad = it }
            )

            // 39. Dolor espalda baja
            QuestionTitle("39. Dolor en espalda baja (lumbar)")
            Text("Frecuencia:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
            ScaleQuestion(
                value = viewModel.dolorEspaldaBajaFrecuencia,
                onValueChange = { viewModel.dolorEspaldaBajaFrecuencia = it }
            )
            Text("Intensidad:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
            ScaleQuestion(
                value = viewModel.dolorEspaldaBajaIntensidad,
                onValueChange = { viewModel.dolorEspaldaBajaIntensidad = it }
            )

            // 40. Rigidez al despertar
            QuestionTitle("40. Rigidez muscular en espalda al despertar")
            Text("Frecuencia:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
            ScaleQuestion(
                value = viewModel.rigidezEspaldaMañanaFrecuencia,
                onValueChange = { viewModel.rigidezEspaldaMañanaFrecuencia = it }
            )
            Text("Intensidad:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
            ScaleQuestion(
                value = viewModel.rigidezEspaldaMañanaIntensidad,
                onValueChange = { viewModel.rigidezEspaldaMañanaIntensidad = it }
            )

            // 41. Dolor con movimiento
            QuestionTitle("41. ¿El dolor mejora con movimiento o empeora?")
            SingleChoiceQuestion(
                options = listOf(
                    "Mejora con movimiento",
                    "Empeora con movimiento",
                    "No cambia",
                    "No tengo dolor"
                ),
                selectedOption = viewModel.dolorConMovimiento,
                onOptionSelected = { viewModel.dolorConMovimiento = it }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // BRAZOS Y MANOS
            SectionHeader("Brazos y Manos")

            // 42. Dolor muñecas
            QuestionTitle("42. Dolor en muñecas")
            Text("Frecuencia:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
            ScaleQuestion(
                value = viewModel.dolorMunecasFrecuencia,
                onValueChange = { viewModel.dolorMunecasFrecuencia = it }
            )
            Text("Intensidad:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
            ScaleQuestion(
                value = viewModel.dolorMunecasIntensidad,
                onValueChange = { viewModel.dolorMunecasIntensidad = it }
            )

            // 43. Dolor manos
            QuestionTitle("43. Dolor en manos/dedos")
            Text("Frecuencia:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
            ScaleQuestion(
                value = viewModel.dolorManosFrecuencia,
                onValueChange = { viewModel.dolorManosFrecuencia = it }
            )
            Text("Intensidad:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
            ScaleQuestion(
                value = viewModel.dolorManosIntensidad,
                onValueChange = { viewModel.dolorManosIntensidad = it }
            )

            // 44. Hormigueo
            QuestionTitle("44. Hormigueo o entumecimiento en manos")
            Text("Frecuencia:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
            ScaleQuestion(
                value = viewModel.hormigueoManosFrecuencia,
                onValueChange = { viewModel.hormigueoManosFrecuencia = it }
            )
            Text("Intensidad:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
            ScaleQuestion(
                value = viewModel.hormigueoManosIntensidad,
                onValueChange = { viewModel.hormigueoManosIntensidad = it }
            )

            // 45. Hormigueo por la noche
            QuestionTitle("45. ¿El hormigueo se presenta más por la noche?")
            SingleChoiceQuestion(
                options = listOf(
                    "Sí, me despierta",
                    "Sí, ocasionalmente",
                    "No",
                    "No tengo hormigueo"
                ),
                selectedOption = viewModel.hormigueoPorNoche,
                onOptionSelected = { viewModel.hormigueoPorNoche = it }
            )

            // 46. Dolor codos
            QuestionTitle("46. Dolor en codos")
            Text("Frecuencia:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
            ScaleQuestion(
                value = viewModel.dolorCodosFrecuencia,
                onValueChange = { viewModel.dolorCodosFrecuencia = it }
            )
            Text("Intensidad:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
            ScaleQuestion(
                value = viewModel.dolorCodosIntensidad,
                onValueChange = { viewModel.dolorCodosIntensidad = it }
            )

            // 47. Debilidad
            QuestionTitle("47. Debilidad al agarrar objetos")
            Text("Frecuencia (0-5):", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
            ScaleQuestion(
                value = viewModel.debilidadAgarrar,
                onValueChange = { viewModel.debilidadAgarrar = it }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // DOLOR DE CABEZA
            SectionHeader("Dolor de Cabeza")

            // 48. Dolor de cabeza
            QuestionTitle("48. Dolor de cabeza tensional")
            Text("Frecuencia:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
            ScaleQuestion(
                value = viewModel.dolorCabezaFrecuencia,
                onValueChange = { viewModel.dolorCabezaFrecuencia = it }
            )
            Text("Intensidad:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
            ScaleQuestion(
                value = viewModel.dolorCabezaIntensidad,
                onValueChange = { viewModel.dolorCabezaIntensidad = it }
            )

            // 49. Momento del dolor
            QuestionTitle("49. ¿En qué momento del día aparece más el dolor de cabeza?")
            SingleChoiceQuestion(
                options = listOf(
                    "Por la mañana",
                    "A media tarde",
                    "Al final del día",
                    "Sin patrón específico",
                    "No tengo dolor de cabeza"
                ),
                selectedOption = viewModel.momentoDolorCabeza,
                onOptionSelected = { viewModel.momentoDolorCabeza = it }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // IMPACTO FUNCIONAL
            SectionHeader("Impacto Funcional")

            // 50. Impiden actividades
            QuestionTitle("50. ¿Algún dolor te ha impedido realizar actividades cotidianas?")
            SingleChoiceQuestion(
                options = listOf(
                    "Sí, frecuentemente",
                    "Sí, ocasionalmente",
                    "Rara vez",
                    "Nunca"
                ),
                selectedOption = viewModel.dolorImpidenActividades,
                onOptionSelected = { viewModel.dolorImpidenActividades = it }
            )

            // 51. Consulta médica
            QuestionTitle("51. ¿Has consultado a un médico por estos dolores?")
            SingleChoiceQuestion(
                options = listOf(
                    "Sí, y estoy en tratamiento",
                    "Sí, pero no continué tratamiento",
                    "No, pero debería",
                    "No, no lo considero necesario"
                ),
                selectedOption = viewModel.haConsultadoMedico,
                onOptionSelected = { viewModel.haConsultadoMedico = it }
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