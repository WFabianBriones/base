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
                title = { Text("Cuestionario de Salud") },
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
            // Información introductoria
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
                        text = "Este cuestionario nos ayudará a crear un diagnóstico personalizado de tu salud. Por favor responde con honestidad.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Sección 1: Información Básica
            SectionHeader("Información Básica")

            // 1. Edad
            QuestionTitle("1. Edad")
            SingleChoiceQuestion(
                options = listOf("18-25", "26-35", "36-45", "46-55", "56-65", "66+"),
                selectedOption = viewModel.ageRange,
                onOptionSelected = { viewModel.ageRange = it }
            )

            // 2. Género
            QuestionTitle("2. Género")
            SingleChoiceQuestion(
                options = listOf("Masculino", "Femenino", "Otro", "Prefiero no decir"),
                selectedOption = viewModel.gender,
                onOptionSelected = { viewModel.gender = it }
            )

            // 3. Peso
            QuestionTitle("3. Peso (kg)")
            OutlinedTextField(
                value = viewModel.weight,
                onValueChange = {
                    viewModel.weight = it
                    viewModel.calculateBMI()
                },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                placeholder = { Text("Ej: 70") }
            )

            // 4. Altura
            QuestionTitle("4. Altura (cm)")
            OutlinedTextField(
                value = viewModel.height,
                onValueChange = {
                    viewModel.height = it
                    viewModel.calculateBMI()
                },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                placeholder = { Text("Ej: 170") }
            )

            // 5. IMC (auto-calculado)
            if (viewModel.bmi.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
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
                Spacer(modifier = Modifier.height(8.dp))
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Sección 2: Hábitos
            SectionHeader("Hábitos de Salud")

            // 6. ¿Fumas?
            QuestionTitle("6. ¿Fumas?")
            SingleChoiceQuestion(
                options = listOf(
                    "No",
                    "Sí, ocasionalmente",
                    "Sí, regularmente (menos de 10 cigarrillos/día)",
                    "Sí, regularmente (más de 10 cigarrillos/día)"
                ),
                selectedOption = viewModel.smokingStatus,
                onOptionSelected = { viewModel.smokingStatus = it }
            )

            // 7. Consumo de alcohol
            QuestionTitle("7. Consumo de alcohol")
            SingleChoiceQuestion(
                options = listOf(
                    "No consumo",
                    "Ocasional (1-2 veces/mes)",
                    "Moderado (1-2 veces/semana)",
                    "Frecuente (3+ veces/semana)"
                ),
                selectedOption = viewModel.alcoholConsumption,
                onOptionSelected = { viewModel.alcoholConsumption = it }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Sección 3: Condiciones Médicas
            SectionHeader("Condiciones Médicas")

            // 8. Condiciones médicas preexistentes
            QuestionTitle("8. Condiciones médicas preexistentes (selecciona todas las que apliquen)")
            MultipleChoiceQuestion(
                options = listOf(
                    "Diabetes", "Hipertensión", "Problemas cardíacos", "Asma",
                    "Artritis", "Problemas de tiroides", "Migrañas crónicas",
                    "Ansiedad/Depresión", "Hernia discal", "Problemas de columna", "Ninguna"
                ),
                selectedOptions = viewModel.preexistingConditions,
                onOptionToggled = { option ->
                    viewModel.preexistingConditions = if (option == "Ninguna") {
                        if (viewModel.preexistingConditions.contains("Ninguna")) {
                            emptyList()
                        } else {
                            listOf("Ninguna")
                        }
                    } else {
                        val newList = viewModel.preexistingConditions.toMutableList()
                        newList.remove("Ninguna")
                        if (newList.contains(option)) {
                            newList.remove(option)
                        } else {
                            newList.add(option)
                        }
                        newList
                    }
                }
            )

            // 9. Medicamentos
            QuestionTitle("9. Medicamentos que toma regularmente")
            MultipleChoiceQuestion(
                options = listOf(
                    "Ninguno", "Analgésicos", "Antiinflamatorios",
                    "Antidepresivos/Ansiolíticos", "Medicamentos para presión",
                    "Medicamentos para diabetes", "Relajantes musculares"
                ),
                selectedOptions = viewModel.medications,
                onOptionToggled = { option ->
                    viewModel.medications = if (option == "Ninguno") {
                        if (viewModel.medications.contains("Ninguno")) {
                            emptyList()
                        } else {
                            listOf("Ninguno")
                        }
                    } else {
                        val newList = viewModel.medications.toMutableList()
                        newList.remove("Ninguno")
                        if (newList.contains(option)) {
                            newList.remove(option)
                        } else {
                            newList.add(option)
                        }
                        newList
                    }
                }
            )

            // 10. Cirugías recientes
            QuestionTitle("10. ¿Ha tenido cirugías en los últimos 5 años?")
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = !viewModel.recentSurgeries,
                        onClick = {
                            viewModel.recentSurgeries = false
                            viewModel.surgeryDetails = ""
                        }
                    )
                    Text("No")
                }
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
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
                    label = { Text("Especificar tipo de cirugía") },
                    placeholder = { Text("Ej: Apendicectomía") }
                )
            }

            // 11. Historial familiar
            QuestionTitle("11. Historial familiar de enfermedades")
            MultipleChoiceQuestion(
                options = listOf(
                    "Enfermedades cardíacas", "Diabetes", "Cáncer",
                    "Enfermedades autoinmunes", "Problemas de espalda/articulaciones",
                    "Trastornos mentales", "Ninguno conocido"
                ),
                selectedOptions = viewModel.familyHistory,
                onOptionToggled = { option ->
                    viewModel.familyHistory = if (option == "Ninguno conocido") {
                        if (viewModel.familyHistory.contains("Ninguno conocido")) {
                            emptyList()
                        } else {
                            listOf("Ninguno conocido")
                        }
                    } else {
                        val newList = viewModel.familyHistory.toMutableList()
                        newList.remove("Ninguno conocido")
                        if (newList.contains(option)) {
                            newList.remove(option)
                        } else {
                            newList.add(option)
                        }
                        newList
                    }
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Sección 4: Estado General
            SectionHeader("Estado General de Salud")

            // 12. Nivel de energía
            QuestionTitle("12. Nivel de energía general")
            SingleChoiceQuestion(
                options = listOf(
                    "Muy bajo (constantemente cansado)",
                    "Bajo (frecuentemente cansado)",
                    "Normal",
                    "Alto (raramente cansado)"
                ),
                selectedOption = viewModel.energyLevel,
                onOptionSelected = { viewModel.energyLevel = it }
            )

            // 13. COVID-19
            QuestionTitle("13. ¿Ha tenido COVID-19?")
            SingleChoiceQuestion(
                options = listOf(
                    "No",
                    "Sí, sin secuelas",
                    "Sí, con secuelas persistentes (COVID largo)"
                ),
                selectedOption = viewModel.hadCovid,
                onOptionSelected = { viewModel.hadCovid = it }
            )

            // 14. Síntomas post-COVID
            if (viewModel.hadCovid.contains("secuelas")) {
                QuestionTitle("14. Síntomas post-COVID")
                MultipleChoiceQuestion(
                    options = listOf(
                        "Fatiga prolongada",
                        "Problemas respiratorios",
                        "Problemas de concentración (niebla mental)",
                        "Dolor muscular/articular",
                        "Ninguno"
                    ),
                    selectedOptions = viewModel.covidSymptoms,
                    onOptionToggled = { option ->
                        viewModel.covidSymptoms = if (option == "Ninguno") {
                            if (viewModel.covidSymptoms.contains("Ninguno")) {
                                emptyList()
                            } else {
                                listOf("Ninguno")
                            }
                        } else {
                            val newList = viewModel.covidSymptoms.toMutableList()
                            newList.remove("Ninguno")
                            if (newList.contains(option)) {
                                newList.remove(option)
                            } else {
                                newList.add(option)
                            }
                            newList
                        }
                    }
                )
            }

            // 15. Estado general de salud
            QuestionTitle("15. Estado general de salud percibido")
            SingleChoiceQuestion(
                options = listOf("Excelente", "Muy bueno", "Bueno", "Regular", "Malo"),
                selectedOption = viewModel.generalHealthStatus,
                onOptionSelected = { viewModel.generalHealthStatus = it }
            )

            // 16. Chequeos médicos
            QuestionTitle("16. ¿Realiza chequeos médicos anuales?")
            SingleChoiceQuestion(
                options = listOf("Sí, regularmente", "Ocasionalmente", "Rara vez", "Nunca"),
                selectedOption = viewModel.annualCheckups,
                onOptionSelected = { viewModel.annualCheckups = it }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Sección 5: Indicadores de Salud
            SectionHeader("Indicadores de Salud")

            // 17. Presión arterial
            QuestionTitle("17. Presión arterial (si la conoce)")
            SingleChoiceQuestion(
                options = listOf(
                    "Normal (<120/80)",
                    "Elevada (120-129/<80)",
                    "Hipertensión leve (130-139/80-89)",
                    "Hipertensión moderada/severa (140+/90+)",
                    "No sé"
                ),
                selectedOption = viewModel.bloodPressure,
                onOptionSelected = { viewModel.bloodPressure = it }
            )

            // 18. Colesterol
            QuestionTitle("18. Nivel de colesterol (si lo conoce)")
            SingleChoiceQuestion(
                options = listOf(
                    "Normal (<200 mg/dL)",
                    "Límite alto (200-239)",
                    "Alto (240+)",
                    "No sé"
                ),
                selectedOption = viewModel.cholesterolLevel,
                onOptionSelected = { viewModel.cholesterolLevel = it }
            )

            // 19. Glucosa
            QuestionTitle("19. Glucosa en sangre (si la conoce)")
            SingleChoiceQuestion(
                options = listOf(
                    "Normal (<100 mg/dL)",
                    "Prediabetes (100-125)",
                    "Diabetes (126+)",
                    "No sé"
                ),
                selectedOption = viewModel.bloodGlucose,
                onOptionSelected = { viewModel.bloodGlucose = it }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Sección 6: Alergias y Problemas Laborales
            SectionHeader("Alergias y Vida Laboral")

            // 20. Alergias
            QuestionTitle("20. ¿Tiene alergias conocidas?")
            MultipleChoiceQuestion(
                options = listOf(
                    "No",
                    "Alergias alimentarias",
                    "Alergias a medicamentos",
                    "Alergias ambientales (polen, polvo)",
                    "Alergias a productos químicos"
                ),
                selectedOptions = viewModel.allergies,
                onOptionToggled = { option ->
                    viewModel.allergies = if (option == "No") {
                        if (viewModel.allergies.contains("No")) {
                            emptyList()
                        } else {
                            listOf("No")
                        }
                    } else {
                        val newList = viewModel.allergies.toMutableList()
                        newList.remove("No")
                        if (newList.contains(option)) {
                            newList.remove(option)
                        } else {
                            newList.add(option)
                        }
                        newList
                    }
                }
            )

            // 21. Problemas laborales
            QuestionTitle("21. Problemas de salud que interfieren con el trabajo")
            MultipleChoiceQuestion(
                options = listOf(
                    "Ninguno",
                    "Dolor crónico",
                    "Limitaciones físicas",
                    "Problemas de movilidad",
                    "Problemas de visión/audición",
                    "Fatiga crónica"
                ),
                selectedOptions = viewModel.workInterference,
                onOptionToggled = { option ->
                    viewModel.workInterference = if (option == "Ninguno") {
                        if (viewModel.workInterference.contains("Ninguno")) {
                            emptyList()
                        } else {
                            listOf("Ninguno")
                        }
                    } else {
                        val newList = viewModel.workInterference.toMutableList()
                        newList.remove("Ninguno")
                        if (newList.contains(option)) {
                            newList.remove(option)
                        } else {
                            newList.add(option)
                        }
                        newList
                    }
                }
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Botón de enviar
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
                    Text("Enviar Cuestionario", style = MaterialTheme.typography.bodyLarge)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(vertical = 8.dp)
    )
    HorizontalDivider(
        thickness = 2.dp,
        color = MaterialTheme.colorScheme.primary
    )
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
                RadioButton(
                    selected = (option == selectedOption),
                    onClick = null
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