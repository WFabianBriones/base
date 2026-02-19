package com.example.uleammed.questionnaires

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.semantics.Role
import androidx.compose.material.icons.filled.Send
import com.example.uleammed.questionnaires.QuestionnaireState
import com.example.uleammed.questionnaires.QuestionnaireViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuestionnaireScreen(
    onComplete: () -> Unit,
    viewModel: QuestionnaireViewModel = viewModel()
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
                title = { Text("Tu salud general") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
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
            // Introducción
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Info, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Este cuestionario nos ayuda a conocer tu estado de salud general. Responde con sinceridad, tus datos son confidenciales.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── Sección 1: Datos básicos ──────────────────────────────────
            SectionHeader("Datos básicos")

            QuestionTitle("¿Cuál es tu rango de edad?")
            SingleChoiceQuestion(
                options = listOf("18-25", "26-35", "36-45", "46-55", "56-65", "66 o más"),
                selectedOption = viewModel.ageRange,
                onOptionSelected = { viewModel.ageRange = it }
            )

            QuestionTitle("¿Con qué género te identificas?")
            SingleChoiceQuestion(
                options = listOf("Masculino", "Femenino", "Otro", "Prefiero no decirlo"),
                selectedOption = viewModel.gender,
                onOptionSelected = { viewModel.gender = it }
            )

            // ── Campo Peso — solo números y un punto decimal ──────────────
            QuestionTitle("¿Cuánto pesas? (kg)")
            OutlinedTextField(
                value = viewModel.weight,
                onValueChange = { input ->
                    // Solo permite dígitos y un único punto decimal
                    val filtered = input.filter { it.isDigit() || it == '.' }
                    val dotCount = filtered.count { it == '.' }
                    if (dotCount <= 1) {
                        viewModel.weight = filtered
                        viewModel.calculateBMI()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                placeholder = { Text("Ej: 70") },
                supportingText = { Text("Solo números, por ejemplo: 68 o 68.5") },
                singleLine = true
            )

            // ── Campo Altura — solo números enteros ───────────────────────
            QuestionTitle("¿Cuánto mides? (cm)")
            OutlinedTextField(
                value = viewModel.height,
                onValueChange = { input ->
                    // Solo permite dígitos (la altura en cm es un número entero)
                    val filtered = input.filter { it.isDigit() }
                    viewModel.height = filtered
                    viewModel.calculateBMI()
                },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                placeholder = { Text("Ej: 170") },
                supportingText = { Text("Solo números, por ejemplo: 165") },
                singleLine = true
            )

            // IMC calculado automáticamente
            if (viewModel.bmi.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Tu IMC: ${viewModel.bmi}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Categoría: ${viewModel.bmiCategory}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── Sección 2: Hábitos ────────────────────────────────────────
            SectionHeader("Hábitos de salud")

            QuestionTitle("¿Fumas?")
            SingleChoiceQuestion(
                options = listOf(
                    "No",
                    "Sí, de vez en cuando",
                    "Sí, regularmente (menos de 10 cigarrillos al día)",
                    "Sí, regularmente (más de 10 cigarrillos al día)"
                ),
                selectedOption = viewModel.smokingStatus,
                onOptionSelected = { viewModel.smokingStatus = it }
            )

            QuestionTitle("¿Con qué frecuencia consumes alcohol?")
            SingleChoiceQuestion(
                options = listOf(
                    "No consumo",
                    "De vez en cuando (1 o 2 veces al mes)",
                    "Moderadamente (1 o 2 veces a la semana)",
                    "Con frecuencia (3 o más veces a la semana)"
                ),
                selectedOption = viewModel.alcoholConsumption,
                onOptionSelected = { viewModel.alcoholConsumption = it }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ── Sección 3: Condiciones médicas ────────────────────────────
            SectionHeader("Condiciones médicas")

            QuestionTitle("¿Tienes alguna de estas condiciones médicas? (puedes marcar varias)")
            MultipleChoiceQuestion(
                options = listOf(
                    "Diabetes",
                    "Hipertensión (presión alta)",
                    "Problemas cardíacos",
                    "Asma",
                    "Artritis",
                    "Problemas de tiroides",
                    "Migrañas crónicas",
                    "Ansiedad o depresión",
                    "Hernia discal",
                    "Problemas de columna",
                    "Ninguna"
                ),
                selectedOptions = viewModel.preexistingConditions,
                onOptionToggled = { option ->
                    viewModel.preexistingConditions = if (option == "Ninguna") {
                        if (viewModel.preexistingConditions.contains("Ninguna")) emptyList()
                        else listOf("Ninguna")
                    } else {
                        val newList = viewModel.preexistingConditions.toMutableList()
                        newList.remove("Ninguna")
                        if (newList.contains(option)) newList.remove(option) else newList.add(option)
                        newList
                    }
                }
            )

            QuestionTitle("¿Tomas alguno de estos medicamentos de forma regular? (puedes marcar varios)")
            MultipleChoiceQuestion(
                options = listOf(
                    "Ninguno",
                    "Analgésicos (para el dolor)",
                    "Antiinflamatorios",
                    "Antidepresivos o ansiolíticos",
                    "Medicamentos para la presión",
                    "Medicamentos para la diabetes",
                    "Relajantes musculares"
                ),
                selectedOptions = viewModel.medications,
                onOptionToggled = { option ->
                    viewModel.medications = if (option == "Ninguno") {
                        if (viewModel.medications.contains("Ninguno")) emptyList()
                        else listOf("Ninguno")
                    } else {
                        val newList = viewModel.medications.toMutableList()
                        newList.remove("Ninguno")
                        if (newList.contains(option)) newList.remove(option) else newList.add(option)
                        newList
                    }
                }
            )

            QuestionTitle("¿Has tenido alguna cirugía en los últimos 5 años?")
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = !viewModel.recentSurgeries,
                        onClick = {
                            viewModel.recentSurgeries = false
                            viewModel.surgeryDetails = ""
                        }
                    )
                    Text("No")
                }
                Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = viewModel.recentSurgeries,
                        onClick = { viewModel.recentSurgeries = true }
                    )
                    Text("Sí")
                }
            }

            if (viewModel.recentSurgeries) {
                OutlinedTextField(
                    value = viewModel.surgeryDetails,
                    onValueChange = { viewModel.surgeryDetails = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("¿Qué tipo de cirugía?") },
                    placeholder = { Text("Ej: Apendicitis, rodilla, etc.") }
                )
            }

            QuestionTitle("¿Hay enfermedades que se repitan en tu familia? (puedes marcar varias)")
            MultipleChoiceQuestion(
                options = listOf(
                    "Enfermedades del corazón",
                    "Diabetes",
                    "Cáncer",
                    "Enfermedades autoinmunes",
                    "Problemas de espalda o articulaciones",
                    "Trastornos mentales",
                    "Ninguna conocida"
                ),
                selectedOptions = viewModel.familyHistory,
                onOptionToggled = { option ->
                    viewModel.familyHistory = if (option == "Ninguna conocida") {
                        if (viewModel.familyHistory.contains("Ninguna conocida")) emptyList()
                        else listOf("Ninguna conocida")
                    } else {
                        val newList = viewModel.familyHistory.toMutableList()
                        newList.remove("Ninguna conocida")
                        if (newList.contains(option)) newList.remove(option) else newList.add(option)
                        newList
                    }
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ── Sección 4: Estado general ─────────────────────────────────
            SectionHeader("¿Cómo te sientes en general?")

            QuestionTitle("¿Cómo describirías tu nivel de energía durante el día?")
            SingleChoiceQuestion(
                options = listOf(
                    "Muy bajo, siempre me siento cansado/a",
                    "Bajo, me canso con frecuencia",
                    "Normal",
                    "Alto, rara vez me siento cansado/a"
                ),
                selectedOption = viewModel.energyLevel,
                onOptionSelected = { viewModel.energyLevel = it }
            )

            QuestionTitle("¿Has tenido COVID-19?")
            SingleChoiceQuestion(
                options = listOf(
                    "No",
                    "Sí, me recuperé sin secuelas",
                    "Sí, y aún tengo síntomas persistentes (COVID largo)"
                ),
                selectedOption = viewModel.hadCovid,
                onOptionSelected = { viewModel.hadCovid = it }
            )

            if (viewModel.hadCovid.contains("secuelas")) {
                QuestionTitle("¿Qué síntomas de COVID largo tienes? (puedes marcar varios)")
                MultipleChoiceQuestion(
                    options = listOf(
                        "Cansancio prolongado",
                        "Problemas respiratorios",
                        "Dificultad para concentrarse (niebla mental)",
                        "Dolor muscular o en articulaciones",
                        "Ninguno"
                    ),
                    selectedOptions = viewModel.covidSymptoms,
                    onOptionToggled = { option ->
                        viewModel.covidSymptoms = if (option == "Ninguno") {
                            if (viewModel.covidSymptoms.contains("Ninguno")) emptyList()
                            else listOf("Ninguno")
                        } else {
                            val newList = viewModel.covidSymptoms.toMutableList()
                            newList.remove("Ninguno")
                            if (newList.contains(option)) newList.remove(option) else newList.add(option)
                            newList
                        }
                    }
                )
            }

            QuestionTitle("En general, ¿cómo calificarías tu salud?")
            SingleChoiceQuestion(
                options = listOf("Excelente", "Muy buena", "Buena", "Regular", "Mala"),
                selectedOption = viewModel.generalHealthStatus,
                onOptionSelected = { viewModel.generalHealthStatus = it }
            )

            QuestionTitle("¿Te haces chequeos médicos de rutina?")
            SingleChoiceQuestion(
                options = listOf(
                    "Sí, regularmente",
                    "De vez en cuando",
                    "Rara vez",
                    "Nunca"
                ),
                selectedOption = viewModel.annualCheckups,
                onOptionSelected = { viewModel.annualCheckups = it }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ── Sección 5: Indicadores de salud ──────────────────────────
            SectionHeader("Indicadores de salud (si los conoces)")

            QuestionTitle("¿Cómo está tu presión arterial?")
            SingleChoiceQuestion(
                options = listOf(
                    "Normal (menos de 120/80)",
                    "Algo elevada (120-129 / menos de 80)",
                    "Hipertensión leve (130-139 / 80-89)",
                    "Hipertensión moderada o severa (140 o más / 90 o más)",
                    "No lo sé"
                ),
                selectedOption = viewModel.bloodPressure,
                onOptionSelected = { viewModel.bloodPressure = it }
            )

            QuestionTitle("¿Cómo está tu nivel de colesterol?")
            SingleChoiceQuestion(
                options = listOf(
                    "Normal (menos de 200 mg/dL)",
                    "Un poco alto (200-239 mg/dL)",
                    "Alto (240 mg/dL o más)",
                    "No lo sé"
                ),
                selectedOption = viewModel.cholesterolLevel,
                onOptionSelected = { viewModel.cholesterolLevel = it }
            )

            QuestionTitle("¿Cómo está tu glucosa en sangre (azúcar)?")
            SingleChoiceQuestion(
                options = listOf(
                    "Normal (menos de 100 mg/dL)",
                    "Prediabetes (100-125 mg/dL)",
                    "Diabetes (126 mg/dL o más)",
                    "No lo sé"
                ),
                selectedOption = viewModel.bloodGlucose,
                onOptionSelected = { viewModel.bloodGlucose = it }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ── Sección 6: Alergias y vida laboral ────────────────────────
            SectionHeader("Alergias y trabajo")

            QuestionTitle("¿Tienes alguna alergia conocida? (puedes marcar varias)")
            MultipleChoiceQuestion(
                options = listOf(
                    "No",
                    "Alergias a alimentos",
                    "Alergias a medicamentos",
                    "Alergias ambientales (polen, polvo, etc.)",
                    "Alergias a productos químicos"
                ),
                selectedOptions = viewModel.allergies,
                onOptionToggled = { option ->
                    viewModel.allergies = if (option == "No") {
                        if (viewModel.allergies.contains("No")) emptyList()
                        else listOf("No")
                    } else {
                        val newList = viewModel.allergies.toMutableList()
                        newList.remove("No")
                        if (newList.contains(option)) newList.remove(option) else newList.add(option)
                        newList
                    }
                }
            )

            QuestionTitle("¿Algún problema de salud te limita o dificulta tu trabajo? (puedes marcar varios)")
            MultipleChoiceQuestion(
                options = listOf(
                    "Ninguno",
                    "Dolor crónico",
                    "Limitaciones físicas",
                    "Problemas de movilidad",
                    "Problemas de vista o audición",
                    "Cansancio crónico"
                ),
                selectedOptions = viewModel.workInterference,
                onOptionToggled = { option ->
                    viewModel.workInterference = if (option == "Ninguno") {
                        if (viewModel.workInterference.contains("Ninguno")) emptyList()
                        else listOf("Ninguno")
                    } else {
                        val newList = viewModel.workInterference.toMutableList()
                        newList.remove("Ninguno")
                        if (newList.contains(option)) newList.remove(option) else newList.add(option)
                        newList
                    }
                }
            )

            Spacer(modifier = Modifier.height(32.dp))

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
                    Icon(Icons.Filled.Send, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Enviar Cuestionario", style = MaterialTheme.typography.bodyLarge)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

// ── Componentes compartidos ────────────────────────────────────────────────────

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(vertical = 8.dp)
    )
    HorizontalDivider(thickness = 2.dp, color = MaterialTheme.colorScheme.primary)
    Spacer(modifier = Modifier.height(16.dp))
}

@Composable
fun QuestionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

@Composable
fun SingleChoiceQuestion(
    options: List<String>,
    selectedOption: String,
    onOptionSelected: (String) -> Unit
) {
    Column(modifier = Modifier.selectableGroup()) {
        options.forEach { option ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(
                        selected = (option == selectedOption),
                        onClick = { onOptionSelected(option) },
                        role = Role.RadioButton
                    )
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(selected = (option == selectedOption), onClick = null)
                Text(
                    text = option,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
    }
    Spacer(modifier = Modifier.height(8.dp))
}

@Composable
fun MultipleChoiceQuestion(
    options: List<String>,
    selectedOptions: List<String>,
    onOptionToggled: (String) -> Unit
) {
    Column {
        options.forEach { option ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = selectedOptions.contains(option),
                    onCheckedChange = { onOptionToggled(option) }
                )
                Text(
                    text = option,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
    }
    Spacer(modifier = Modifier.height(8.dp))
}