package com.example.uleammed.perfil

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.example.uleammed.auth.AuthRepository
import com.example.uleammed.HealthQuestionnaire
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * Pantalla para ver el cuestionario inicial completado
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewQuestionnaireScreen(
    onBack: () -> Unit
) {
    var questionnaire by remember { mutableStateOf<HealthQuestionnaire?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    val scope = rememberCoroutineScope()
    val repository = remember { AuthRepository() }
    val userId = FirebaseAuth.getInstance().currentUser?.uid

    LaunchedEffect(Unit) {
        scope.launch {
            isLoading = true
            try {
                if (userId != null) {
                    val result = repository.getQuestionnaire(userId)
                    result.onSuccess { q ->
                        questionnaire = q
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
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mi Cuestionario de Salud") },
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
        when {
            isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            error != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ErrorOutline,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = error ?: "Error desconocido",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                        Button(onClick = onBack) {
                            Text("Volver")
                        }
                    }
                }
            }

            questionnaire != null -> {
                QuestionnaireContent(
                    questionnaire = questionnaire!!,
                    modifier = Modifier.padding(paddingValues)
                )
            }
        }
    }
}

@Composable
fun QuestionnaireContent(
    questionnaire: HealthQuestionnaire,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Assignment,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Cuestionario Inicial de Salud",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                    Text(
                        text = "Completado: ${dateFormat.format(Date(questionnaire.completedAt))}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        // Información Básica
        item {
            SectionCard(
                title = "Información Básica",
                icon = Icons.Filled.Person,
                items = listOf(
                    "Rango de edad" to questionnaire.ageRange,
                    "Género" to questionnaire.gender,
                    "Peso" to "${questionnaire.weight} kg",
                    "Altura" to "${questionnaire.height} cm",
                    "IMC" to String.format("%.1f", questionnaire.bmi),
                    "Categoría IMC" to questionnaire.bmiCategory
                )
            )
        }

        // Hábitos
        item {
            SectionCard(
                title = "Hábitos de Salud",
                icon = Icons.Filled.LocalActivity,
                items = listOf(
                    "Tabaquismo" to questionnaire.smokingStatus,
                    "Consumo de alcohol" to questionnaire.alcoholConsumption
                )
            )
        }

        // Condiciones médicas
        if (questionnaire.preexistingConditions.isNotEmpty()) {
            item {
                ListSectionCard(
                    title = "Condiciones Médicas Preexistentes",
                    icon = Icons.Filled.MedicalServices,
                    items = questionnaire.preexistingConditions
                )
            }
        }

        // Medicamentos
        if (questionnaire.medications.isNotEmpty()) {
            item {
                ListSectionCard(
                    title = "Medicamentos",
                    icon = Icons.Filled.Medication,
                    items = questionnaire.medications
                )
            }
        }

        // Cirugías
        if (questionnaire.recentSurgeries) {
            item {
                SectionCard(
                    title = "Cirugías Recientes",
                    icon = Icons.Filled.LocalHospital,
                    items = listOf(
                        "Detalles" to questionnaire.surgeryDetails
                    )
                )
            }
        }

        // Historial familiar
        if (questionnaire.familyHistory.isNotEmpty()) {
            item {
                ListSectionCard(
                    title = "Historial Familiar",
                    icon = Icons.Filled.FamilyRestroom,
                    items = questionnaire.familyHistory
                )
            }
        }

        // Estado general
        item {
            SectionCard(
                title = "Estado General de Salud",
                icon = Icons.Filled.HealthAndSafety,
                items = listOf(
                    "Nivel de energía" to questionnaire.energyLevel,
                    "¿Ha tenido COVID-19?" to questionnaire.hadCovid,
                    "Estado general" to questionnaire.generalHealthStatus,
                    "Chequeos anuales" to questionnaire.annualCheckups
                )
            )
        }

        // Síntomas COVID
        if (questionnaire.covidSymptoms.isNotEmpty()) {
            item {
                ListSectionCard(
                    title = "Síntomas Post-COVID",
                    icon = Icons.Filled.Coronavirus,
                    items = questionnaire.covidSymptoms
                )
            }
        }

        // Indicadores de salud
        item {
            SectionCard(
                title = "Indicadores de Salud",
                icon = Icons.Filled.MonitorHeart,
                items = listOf(
                    "Presión arterial" to questionnaire.bloodPressure,
                    "Colesterol" to questionnaire.cholesterolLevel,
                    "Glucosa en sangre" to questionnaire.bloodGlucose
                )
            )
        }

        // Alergias
        if (questionnaire.allergies.isNotEmpty()) {
            item {
                ListSectionCard(
                    title = "Alergias",
                    icon = Icons.Filled.Warning,
                    items = questionnaire.allergies
                )
            }
        }

        // Interferencia laboral
        if (questionnaire.workInterference.isNotEmpty()) {
            item {
                ListSectionCard(
                    title = "Problemas que Afectan el Trabajo",
                    icon = Icons.Filled.Work,
                    items = questionnaire.workInterference
                )
            }
        }

        // Nota final
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        text = "Esta información es confidencial y solo es visible para ti. Se utiliza para personalizar tus recomendaciones de salud laboral.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun SectionCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    items: List<Pair<String, String>>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            HorizontalDivider()

            items.forEach { (label, value) ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = value,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
fun ListSectionCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    items: List<String>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            HorizontalDivider()

            items.forEach { item ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = item,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}