package com.example.uleammed.perfil

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.example.uleammed.auth.AuthRepository
import com.example.uleammed.HealthQuestionnaire
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

// ─────────────────────────────────────────────────────────────────────────────
// Opciones predefinidas para los dropdowns
// ─────────────────────────────────────────────────────────────────────────────

// Opciones idénticas a las del QuestionnaireScreen original
private val AGE_RANGES      = listOf("18-25", "26-35", "36-45", "46-55", "56-65", "66+")
private val GENDERS         = listOf("Masculino", "Femenino", "Otro", "Prefiero no decir")
private val SMOKING_OPTIONS = listOf(
    "No",
    "Sí, ocasionalmente",
    "Sí, regularmente (menos de 10 cigarrillos/día)",
    "Sí, regularmente (más de 10 cigarrillos/día)"
)
private val ALCOHOL_OPTIONS = listOf(
    "No consumo",
    "Ocasional (1-2 veces/mes)",
    "Moderado (1-2 veces/semana)",
    "Frecuente (3+ veces/semana)"
)
private val ENERGY_OPTIONS  = listOf(
    "Muy bajo (constantemente cansado)",
    "Bajo (frecuentemente cansado)",
    "Normal",
    "Alto (raramente cansado)"
)
private val COVID_OPTIONS   = listOf(
    "No",
    "Sí, sin secuelas",
    "Sí, con secuelas persistentes (COVID largo)"
)
private val HEALTH_STATUS   = listOf("Excelente", "Muy bueno", "Bueno", "Regular", "Malo")
private val CHECKUP_OPTIONS = listOf("Sí, regularmente", "Ocasionalmente", "Rara vez", "Nunca")
private val BP_OPTIONS      = listOf(
    "Normal (<120/80)",
    "Elevada (120-129/<80)",
    "Hipertensión leve (130-139/80-89)",
    "Hipertensión moderada/severa (140+/90+)",
    "No sé"
)
private val CHOL_OPTIONS    = listOf(
    "Normal (<200 mg/dL)",
    "Límite alto (200-239)",
    "Alto (240+)",
    "No sé"
)
private val GLUCOSE_OPTIONS = listOf(
    "Normal (<100 mg/dL)",
    "Prediabetes (100-125)",
    "Diabetes (126+)",
    "No sé"
)
private val CONDITION_OPTIONS = listOf(
    "Diabetes", "Hipertensión", "Problemas cardíacos", "Asma",
    "Artritis", "Problemas de tiroides", "Migrañas crónicas",
    "Ansiedad/Depresión", "Hernia discal", "Problemas de columna", "Ninguna"
)
private val MEDICATION_OPTIONS = listOf(
    "Ninguno", "Analgésicos", "Antiinflamatorios",
    "Antidepresivos/Ansiolíticos", "Medicamentos para presión",
    "Medicamentos para diabetes", "Relajantes musculares"
)
private val FAMILY_OPTIONS = listOf(
    "Enfermedades cardíacas", "Diabetes", "Cáncer",
    "Enfermedades autoinmunes", "Problemas de espalda/articulaciones",
    "Trastornos mentales", "Ninguno conocido"
)
private val ALLERGY_OPTIONS = listOf(
    "No",
    "Alergias alimentarias",
    "Alergias a medicamentos",
    "Alergias ambientales (polen, polvo)",
    "Alergias a productos químicos"
)
private val WORK_OPTIONS = listOf(
    "Ninguno", "Dolor crónico", "Limitaciones físicas",
    "Problemas de movilidad", "Problemas de visión/audición", "Fatiga crónica"
)
private val COVID_SYMPTOM_OPTIONS = listOf(
    "Fatiga prolongada",
    "Problemas respiratorios",
    "Problemas de concentración (niebla mental)",
    "Dolor muscular/articular",
    "Ninguno"
)

// ─────────────────────────────────────────────────────────────────────────────
// Pantalla principal
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewQuestionnaireScreen(
    onBack: () -> Unit
) {
    var questionnaire by remember { mutableStateOf<HealthQuestionnaire?>(null) }
    var isLoading    by remember { mutableStateOf(true) }
    var isSaving     by remember { mutableStateOf(false) }
    var error        by remember { mutableStateOf<String?>(null) }
    var isEditMode   by remember { mutableStateOf(false) }
    var showSuccess  by remember { mutableStateOf(false) }
    var showError    by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    // Estado editable (copia de trabajo)
    var editState    by remember { mutableStateOf<HealthQuestionnaire?>(null) }

    val scope      = rememberCoroutineScope()
    val repository = remember { AuthRepository() }
    val userId     = FirebaseAuth.getInstance().currentUser?.uid

    // Carga inicial
    LaunchedEffect(Unit) {
        isLoading = true
        try {
            if (userId != null) {
                val result = repository.getQuestionnaire(userId)
                result.onSuccess { q ->
                    questionnaire = q
                    editState = q
                    error = if (q == null) "No has completado el cuestionario inicial" else null
                }.onFailure { e ->
                    error = e.message ?: "Error al cargar cuestionario"
                }
            } else {
                error = "Usuario no autenticado"
            }
        } catch (e: Exception) {
            error = e.message ?: "Error desconocido"
        } finally {
            isLoading = false
        }
    }

    // Dialogo de éxito
    if (showSuccess) {
        AlertDialog(
            onDismissRequest = { showSuccess = false },
            icon    = { Icon(Icons.Filled.CheckCircle, contentDescription = null) },
            title   = { Text("Cuestionario Actualizado") },
            text    = { Text("Los cambios se guardaron correctamente.") },
            confirmButton = {
                Button(onClick = { showSuccess = false }) { Text("Continuar") }
            }
        )
    }

    // Dialogo de error
    if (showError) {
        AlertDialog(
            onDismissRequest = { showError = false },
            icon    = { Icon(Icons.Filled.Error, contentDescription = null) },
            title   = { Text("Error al Guardar") },
            text    = { Text(errorMessage) },
            confirmButton = {
                Button(onClick = { showError = false }) { Text("Entendido") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(if (isEditMode) "Editar Cuestionario" else "Mi Cuestionario de Salud")
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (isEditMode) {
                            // Cancelar edición: restaurar estado original
                            editState  = questionnaire
                            isEditMode = false
                        } else {
                            onBack()
                        }
                    }) {
                        Icon(
                            if (isEditMode) Icons.Filled.Close else Icons.Filled.ArrowBack,
                            contentDescription = if (isEditMode) "Cancelar" else "Volver"
                        )
                    }
                },
                actions = {
                    if (!isLoading && questionnaire != null) {
                        if (isEditMode) {
                            // Botón guardar
                            IconButton(
                                onClick = {
                                    val updated = editState ?: return@IconButton
                                    isSaving = true
                                    scope.launch {
                                        try {
                                            // El userId ya está dentro del objeto HealthQuestionnaire
                                            val result = repository.saveQuestionnaire(updated)
                                            result.onSuccess {
                                                // Recargar desde Firestore para obtener
                                                // el completedAt actualizado por el repositorio
                                                val reloaded = repository.getQuestionnaire(userId!!)
                                                questionnaire = reloaded.getOrNull() ?: updated.copy(
                                                    completedAt = System.currentTimeMillis()
                                                )
                                                editState  = questionnaire
                                                isEditMode = false
                                                isSaving   = false
                                                showSuccess = true
                                            }.onFailure { e ->
                                                isSaving      = false
                                                errorMessage  = e.message ?: "Error desconocido"
                                                showError     = true
                                            }
                                        } catch (e: Exception) {
                                            isSaving     = false
                                            errorMessage = e.message ?: "Error al guardar"
                                            showError    = true
                                        }
                                    }
                                },
                                enabled = !isSaving
                            ) {
                                if (isSaving) {
                                    CircularProgressIndicator(
                                        modifier    = Modifier.size(20.dp),
                                        color       = MaterialTheme.colorScheme.onPrimary,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(Icons.Filled.Save, contentDescription = "Guardar")
                                }
                            }
                        } else {
                            // Botón editar
                            IconButton(onClick = {
                                editState  = questionnaire
                                isEditMode = true
                            }) {
                                Icon(Icons.Filled.Edit, contentDescription = "Editar cuestionario")
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor         = if (isEditMode) MaterialTheme.colorScheme.tertiary
                    else MaterialTheme.colorScheme.primary,
                    titleContentColor          = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor     = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        when {
            isLoading -> {
                Box(
                    modifier        = Modifier.fillMaxSize().padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator() }
            }

            error != null -> {
                Box(
                    modifier        = Modifier.fillMaxSize().padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment  = Alignment.CenterHorizontally,
                        verticalArrangement  = Arrangement.spacedBy(16.dp),
                        modifier             = Modifier.padding(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ErrorOutline,
                            contentDescription = null,
                            modifier    = Modifier.size(64.dp),
                            tint        = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text  = error ?: "Error desconocido",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                        Button(onClick = onBack) { Text("Volver") }
                    }
                }
            }

            questionnaire != null -> {
                if (isEditMode && editState != null) {
                    QuestionnaireEditContent(
                        editState  = editState!!,
                        onChange   = { editState = it },
                        modifier   = Modifier.padding(paddingValues)
                    )
                } else {
                    QuestionnaireContent(
                        questionnaire = questionnaire!!,
                        modifier      = Modifier.padding(paddingValues)
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Modo EDICIÓN
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun QuestionnaireEditContent(
    editState: HealthQuestionnaire,
    onChange:  (HealthQuestionnaire) -> Unit,
    modifier:  Modifier = Modifier
) {
    LazyColumn(
        modifier            = modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding      = PaddingValues(vertical = 16.dp)
    ) {

        // ── Banner informativo ────────────────────────────────────────────────
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
                )
            ) {
                Row(
                    modifier            = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        Icons.Filled.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary
                    )
                    Text(
                        text  = "Edita los campos que desees y pulsa el ícono 💾 para guardar los cambios.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // ── Información Básica ────────────────────────────────────────────────
        item {
            EditSectionHeader(title = "Información Básica", icon = Icons.Filled.Person)
        }

        item {
            DropdownField(
                label    = "Rango de edad",
                selected = editState.ageRange,
                options  = AGE_RANGES,
                onSelect = { onChange(editState.copy(ageRange = it)) }
            )
        }

        item {
            DropdownField(
                label    = "Género",
                selected = editState.gender,
                options  = GENDERS,
                onSelect = { onChange(editState.copy(gender = it)) }
            )
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                NumericField(
                    label    = "Peso (kg)",
                    value    = editState.weight.toString(),
                    modifier = Modifier.weight(1f),
                    onValueChange = { raw ->
                        val v = raw.toFloatOrNull() ?: return@NumericField
                        val newBmi = calcBmi(v, editState.height)
                        onChange(editState.copy(
                            weight      = v,
                            bmi         = newBmi,
                            bmiCategory = bmiCategory(newBmi)
                        ))
                    }
                )
                NumericField(
                    label    = "Altura (cm)",
                    value    = editState.height.toString(),
                    modifier = Modifier.weight(1f),
                    onValueChange = { raw ->
                        val v = raw.toFloatOrNull() ?: return@NumericField
                        val newBmi = calcBmi(editState.weight, v)
                        onChange(editState.copy(
                            height      = v,
                            bmi         = newBmi,
                            bmiCategory = bmiCategory(newBmi)
                        ))
                    }
                )
            }
        }

        item {
            // IMC calculado (solo lectura)
            OutlinedTextField(
                value         = "IMC: ${"%.1f".format(editState.bmi)}  —  ${editState.bmiCategory}",
                onValueChange = {},
                label         = { Text("IMC (calculado automáticamente)") },
                leadingIcon   = { Icon(Icons.Filled.MonitorHeart, contentDescription = null) },
                modifier      = Modifier.fillMaxWidth(),
                enabled       = false,
                colors        = OutlinedTextFieldDefaults.colors(
                    disabledTextColor        = MaterialTheme.colorScheme.onSurface,
                    disabledBorderColor      = MaterialTheme.colorScheme.outline,
                    disabledLabelColor       = MaterialTheme.colorScheme.onSurfaceVariant,
                    disabledLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }

        // ── Hábitos ───────────────────────────────────────────────────────────
        item { EditSectionHeader(title = "Hábitos de Salud", icon = Icons.Filled.LocalActivity) }

        item {
            DropdownField(
                label    = "Tabaquismo",
                selected = editState.smokingStatus,
                options  = SMOKING_OPTIONS,
                onSelect = { onChange(editState.copy(smokingStatus = it)) }
            )
        }

        item {
            DropdownField(
                label    = "Consumo de alcohol",
                selected = editState.alcoholConsumption,
                options  = ALCOHOL_OPTIONS,
                onSelect = { onChange(editState.copy(alcoholConsumption = it)) }
            )
        }

        // ── Condiciones preexistentes ─────────────────────────────────────────
        item {
            EditSectionHeader(
                title = "Condiciones Médicas Preexistentes",
                icon  = Icons.Filled.MedicalServices
            )
        }

        item {
            MultiSelectChipGroup(
                allOptions    = CONDITION_OPTIONS,
                selectedItems = editState.preexistingConditions,
                onSelectionChange = { onChange(editState.copy(preexistingConditions = it)) }
            )
        }

        // ── Medicamentos ──────────────────────────────────────────────────────
        item { EditSectionHeader(title = "Medicamentos", icon = Icons.Filled.Medication) }

        item {
            MultiSelectChipGroup(
                allOptions        = MEDICATION_OPTIONS,
                selectedItems     = editState.medications,
                onSelectionChange = { onChange(editState.copy(medications = it)) }
            )
        }

        // ── Cirugías recientes ────────────────────────────────────────────────
        item {
            EditSectionHeader(title = "Cirugías Recientes", icon = Icons.Filled.LocalHospital)
        }

        item {
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Checkbox(
                    checked         = editState.recentSurgeries,
                    onCheckedChange = {
                        onChange(editState.copy(
                            recentSurgeries = it,
                            surgeryDetails  = if (!it) "" else editState.surgeryDetails
                        ))
                    }
                )
                Text("He tenido cirugías recientes", style = MaterialTheme.typography.bodyMedium)
            }
        }

        if (editState.recentSurgeries) {
            item {
                OutlinedTextField(
                    value         = editState.surgeryDetails,
                    onValueChange = { onChange(editState.copy(surgeryDetails = it)) },
                    label         = { Text("Detalles de la cirugía") },
                    leadingIcon   = { Icon(Icons.Filled.LocalHospital, contentDescription = null) },
                    modifier      = Modifier.fillMaxWidth(),
                    minLines      = 2
                )
            }
        }

        // ── Historial familiar ────────────────────────────────────────────────
        item {
            EditSectionHeader(title = "Historial Familiar", icon = Icons.Filled.FamilyRestroom)
        }

        item {
            MultiSelectChipGroup(
                allOptions        = FAMILY_OPTIONS,
                selectedItems     = editState.familyHistory,
                onSelectionChange = { onChange(editState.copy(familyHistory = it)) }
            )
        }

        // ── Estado general ────────────────────────────────────────────────────
        item {
            EditSectionHeader(
                title = "Estado General de Salud",
                icon  = Icons.Filled.HealthAndSafety
            )
        }

        item {
            DropdownField(
                label    = "Nivel de energía",
                selected = editState.energyLevel,
                options  = ENERGY_OPTIONS,
                onSelect = { onChange(editState.copy(energyLevel = it)) }
            )
        }

        item {
            DropdownField(
                label    = "¿Ha tenido COVID-19?",
                selected = editState.hadCovid,
                options  = COVID_OPTIONS,
                onSelect = {
                    onChange(editState.copy(
                        hadCovid     = it,
                        covidSymptoms = if (it == "No") emptyList() else editState.covidSymptoms
                    ))
                }
            )
        }

        if (editState.hadCovid.contains("secuelas")) {
            item {
                EditSectionHeader(
                    title = "Síntomas Post-COVID",
                    icon  = Icons.Filled.Coronavirus
                )
            }
            item {
                MultiSelectChipGroup(
                    allOptions        = COVID_SYMPTOM_OPTIONS,
                    selectedItems     = editState.covidSymptoms,
                    onSelectionChange = { onChange(editState.copy(covidSymptoms = it)) }
                )
            }
        }

        item {
            DropdownField(
                label    = "Estado general de salud",
                selected = editState.generalHealthStatus,
                options  = HEALTH_STATUS,
                onSelect = { onChange(editState.copy(generalHealthStatus = it)) }
            )
        }

        item {
            DropdownField(
                label    = "Chequeos anuales",
                selected = editState.annualCheckups,
                options  = CHECKUP_OPTIONS,
                onSelect = { onChange(editState.copy(annualCheckups = it)) }
            )
        }

        // ── Indicadores de salud ──────────────────────────────────────────────
        item {
            EditSectionHeader(
                title = "Indicadores de Salud",
                icon  = Icons.Filled.MonitorHeart
            )
        }

        item {
            DropdownField(
                label    = "Presión arterial",
                selected = editState.bloodPressure,
                options  = BP_OPTIONS,
                onSelect = { onChange(editState.copy(bloodPressure = it)) }
            )
        }

        item {
            DropdownField(
                label    = "Colesterol",
                selected = editState.cholesterolLevel,
                options  = CHOL_OPTIONS,
                onSelect = { onChange(editState.copy(cholesterolLevel = it)) }
            )
        }

        item {
            DropdownField(
                label    = "Glucosa en sangre",
                selected = editState.bloodGlucose,
                options  = GLUCOSE_OPTIONS,
                onSelect = { onChange(editState.copy(bloodGlucose = it)) }
            )
        }

        // ── Alergias ──────────────────────────────────────────────────────────
        item { EditSectionHeader(title = "Alergias", icon = Icons.Filled.Warning) }

        item {
            MultiSelectChipGroup(
                allOptions        = ALLERGY_OPTIONS,
                selectedItems     = editState.allergies,
                onSelectionChange = { onChange(editState.copy(allergies = it)) }
            )
        }

        // ── Problemas que afectan el trabajo ──────────────────────────────────
        item {
            EditSectionHeader(
                title = "Problemas que Afectan el Trabajo",
                icon  = Icons.Filled.Work
            )
        }

        item {
            MultiSelectChipGroup(
                allOptions        = WORK_OPTIONS,
                selectedItems     = editState.workInterference,
                onSelectionChange = { onChange(editState.copy(workInterference = it)) }
            )
        }

        // Espacio final
        item { Spacer(modifier = Modifier.height(32.dp)) }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Componentes reutilizables de edición
// ─────────────────────────────────────────────────────────────────────────────

/** Encabezado de sección con icono y línea divisora */
@Composable
fun EditSectionHeader(
    title: String,
    icon:  androidx.compose.ui.graphics.vector.ImageVector
) {
    Column {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment     = Alignment.CenterVertically,
            modifier              = Modifier.padding(top = 8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text       = title,
                style      = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color      = MaterialTheme.colorScheme.primary
            )
        }
        HorizontalDivider(modifier = Modifier.padding(top = 4.dp))
    }
}

/** Dropdown (ExposedDropdownMenu) genérico */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropdownField(
    label:    String,
    selected: String,
    options:  List<String>,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded         = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value             = selected,
            onValueChange     = {},
            readOnly          = true,
            label             = { Text(label) },
            trailingIcon      = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier          = Modifier
                .fillMaxWidth()
                .menuAnchor(),
            colors            = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
        )
        ExposedDropdownMenu(
            expanded         = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text    = { Text(option) },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    },
                    trailingIcon = if (option == selected) ({
                        Icon(Icons.Filled.Check, contentDescription = null)
                    }) else null
                )
            }
        }
    }
}

/** Campo numérico de texto */
@Composable
fun NumericField(
    label:         String,
    value:         String,
    modifier:      Modifier = Modifier,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value             = value,
        onValueChange     = onValueChange,
        label             = { Text(label) },
        keyboardOptions   = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine        = true,
        modifier          = modifier
    )
}

/**
 * Grupo de chips de selección múltiple para listas de opciones predefinidas.
 * Las opciones seleccionadas se muestran con fondo de color.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MultiSelectChipGroup(
    allOptions:        List<String>,
    selectedItems:     List<String>,
    onSelectionChange: (List<String>) -> Unit
) {
    val selected = remember(selectedItems) { selectedItems.toMutableStateList() }

    // Sincronizar si cambia externamente
    LaunchedEffect(selectedItems) {
        if (selected.toList() != selectedItems) {
            selected.clear()
            selected.addAll(selectedItems)
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // Dividir en filas de 2
        allOptions.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { option ->
                    val isSelected = option in selected
                    FilterChip(
                        selected  = isSelected,
                        onClick   = {
                            if (isSelected) selected.remove(option)
                            else            selected.add(option)
                            onSelectionChange(selected.toList())
                        },
                        label     = { Text(option, style = MaterialTheme.typography.bodySmall) },
                        leadingIcon = if (isSelected) ({
                            Icon(
                                Icons.Filled.Check,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }) else null,
                        modifier  = Modifier.weight(1f)
                    )
                }
                // Si la fila tiene sólo 1 elemento, añadir un Spacer para equilibrar
                if (row.size == 1) Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

/**
 * Lista editable de texto libre (para medicamentos u otras entradas abiertas).
 * Permite añadir nuevos ítems con un campo de texto y eliminar con el botón X.
 */
@Composable
fun EditableTextList(
    label:    String,
    items:    List<String>,
    onChange: (List<String>) -> Unit
) {
    var newItem by remember { mutableStateOf("") }
    val current = remember(items) { items.toMutableStateList() }

    LaunchedEffect(items) {
        if (current.toList() != items) {
            current.clear()
            current.addAll(items)
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // Chips de ítems existentes
        current.forEach { item ->
            InputChip(
                selected  = false,
                onClick   = {},
                label     = { Text(item) },
                trailingIcon = {
                    IconButton(
                        onClick  = {
                            current.remove(item)
                            onChange(current.toList())
                        },
                        modifier = Modifier.size(18.dp)
                    ) {
                        Icon(
                            Icons.Filled.Close,
                            contentDescription = "Eliminar",
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            )
        }

        // Campo para añadir nuevo ítem
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment     = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value         = newItem,
                onValueChange = { newItem = it },
                label         = { Text("Agregar $label") },
                singleLine    = true,
                modifier      = Modifier.weight(1f)
            )
            IconButton(
                onClick = {
                    val trimmed = newItem.trim()
                    if (trimmed.isNotEmpty() && trimmed !in current) {
                        current.add(trimmed)
                        onChange(current.toList())
                        newItem = ""
                    }
                },
                enabled = newItem.isNotBlank()
            ) {
                Icon(
                    Icons.Filled.AddCircle,
                    contentDescription = "Agregar",
                    tint = if (newItem.isNotBlank()) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Cálculo de IMC
// ─────────────────────────────────────────────────────────────────────────────

private fun calcBmi(weightKg: Float, heightCm: Float): Float {
    if (heightCm <= 0f) return 0f
    val heightM = heightCm / 100f
    return weightKg / (heightM * heightM)
}

private fun bmiCategory(bmi: Float): String = when {
    bmi < 18.5f -> "Bajo peso"
    bmi < 25.0f -> "Normal"
    bmi < 30.0f -> "Sobrepeso"
    else        -> "Obesidad"
}

// ─────────────────────────────────────────────────────────────────────────────
// Modo VISTA (sin cambios respecto al original)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun QuestionnaireContent(
    questionnaire: HealthQuestionnaire,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier            = modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors   = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier            = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Assignment, contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary)
                        Text(
                            text       = "Cuestionario Inicial de Salud",
                            style      = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                    Text(
                        text  = "Completado: ${dateFormat.format(Date(questionnaire.completedAt))}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        item {
            SectionCard(
                title = "Información Básica",
                icon  = Icons.Filled.Person,
                items = listOf(
                    "Rango de edad"  to questionnaire.ageRange,
                    "Género"         to questionnaire.gender,
                    "Peso"           to "${questionnaire.weight} kg",
                    "Altura"         to "${questionnaire.height} cm",
                    "IMC"            to String.format("%.1f", questionnaire.bmi),
                    "Categoría IMC"  to questionnaire.bmiCategory
                )
            )
        }

        item {
            SectionCard(
                title = "Hábitos de Salud",
                icon  = Icons.Filled.LocalActivity,
                items = listOf(
                    "Tabaquismo"         to questionnaire.smokingStatus,
                    "Consumo de alcohol" to questionnaire.alcoholConsumption
                )
            )
        }

        if (questionnaire.preexistingConditions.isNotEmpty()) {
            item {
                ListSectionCard(
                    title = "Condiciones Médicas Preexistentes",
                    icon  = Icons.Filled.MedicalServices,
                    items = questionnaire.preexistingConditions
                )
            }
        }

        if (questionnaire.medications.isNotEmpty()) {
            item {
                ListSectionCard(
                    title = "Medicamentos",
                    icon  = Icons.Filled.Medication,
                    items = questionnaire.medications
                )
            }
        }

        if (questionnaire.recentSurgeries) {
            item {
                SectionCard(
                    title = "Cirugías Recientes",
                    icon  = Icons.Filled.LocalHospital,
                    items = listOf("Detalles" to questionnaire.surgeryDetails)
                )
            }
        }

        if (questionnaire.familyHistory.isNotEmpty()) {
            item {
                ListSectionCard(
                    title = "Historial Familiar",
                    icon  = Icons.Filled.FamilyRestroom,
                    items = questionnaire.familyHistory
                )
            }
        }

        item {
            SectionCard(
                title = "Estado General de Salud",
                icon  = Icons.Filled.HealthAndSafety,
                items = listOf(
                    "Nivel de energía"    to questionnaire.energyLevel,
                    "¿Ha tenido COVID-19?" to questionnaire.hadCovid,
                    "Estado general"      to questionnaire.generalHealthStatus,
                    "Chequeos anuales"    to questionnaire.annualCheckups
                )
            )
        }

        if (questionnaire.covidSymptoms.isNotEmpty()) {
            item {
                ListSectionCard(
                    title = "Síntomas Post-COVID",
                    icon  = Icons.Filled.Coronavirus,
                    items = questionnaire.covidSymptoms
                )
            }
        }

        item {
            SectionCard(
                title = "Indicadores de Salud",
                icon  = Icons.Filled.MonitorHeart,
                items = listOf(
                    "Presión arterial"  to questionnaire.bloodPressure,
                    "Colesterol"        to questionnaire.cholesterolLevel,
                    "Glucosa en sangre" to questionnaire.bloodGlucose
                )
            )
        }

        if (questionnaire.allergies.isNotEmpty()) {
            item {
                ListSectionCard(
                    title = "Alergias",
                    icon  = Icons.Filled.Warning,
                    items = questionnaire.allergies
                )
            }
        }

        if (questionnaire.workInterference.isNotEmpty()) {
            item {
                ListSectionCard(
                    title = "Problemas que Afectan el Trabajo",
                    icon  = Icons.Filled.Work,
                    items = questionnaire.workInterference
                )
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors   = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
                )
            ) {
                Row(
                    modifier              = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Filled.Info, contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary)
                    Text(
                        text  = "Esta información es confidencial y solo es visible para ti. " +
                                "Se utiliza para personalizar tus recomendaciones de salud laboral.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item { Spacer(modifier = Modifier.height(32.dp)) }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Tarjetas de visualización (sin cambios)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun SectionCard(
    title: String,
    icon:  androidx.compose.ui.graphics.vector.ImageVector,
    items: List<Pair<String, String>>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier            = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(text = title, style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold)
            }
            HorizontalDivider()
            items.forEach { (label, value) ->
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = label, style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f))
                    Text(text = value, style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun ListSectionCard(
    title: String,
    icon:  androidx.compose.ui.graphics.vector.ImageVector,
    items: List<String>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier            = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(text = title, style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold)
            }
            HorizontalDivider()
            items.forEach { item ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier              = Modifier.padding(start = 8.dp)
                ) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary)
                    Text(text = item, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}