package com.example.uleammed.resources

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.uleammed.QuestionnaireType

/**
 * Tipos de recursos disponibles
 */
enum class ResourceType(
    val displayName: String,
    val icon: ImageVector
) {
    ARTICLE("Artículo", Icons.Filled.Article),
    GUIDE("Guía", Icons.Filled.MenuBook),
    VIDEO("Video", Icons.Filled.VideoLibrary),
    INFOGRAPHIC("Infografía", Icons.Filled.Image),
    EXERCISE("Ejercicio", Icons.Filled.FitnessCenter),
    RESEARCH("Investigación", Icons.Filled.Science),
    TOOL("Herramienta", Icons.Filled.Build),
    FAQ("FAQ", Icons.Filled.QuestionAnswer)
}

/**
 * Nivel de dificultad del contenido
 */
enum class ResourceDifficulty(
    val displayName: String,
    val icon: ImageVector
) {
    BASIC("Básico", Icons.Filled.School),
    INTERMEDIATE("Intermedio", Icons.Filled.TrendingUp),
    ADVANCED("Avanzado", Icons.Filled.EmojiEvents)
}

/**
 * Categoría temática (alineada con cuestionarios)
 */
enum class ResourceCategory(
    val displayName: String,
    val icon: ImageVector,
    val color: Long // Color en formato hex
) {
    ERGONOMICS("Ergonomía", Icons.Filled.Computer, 0xFF4CAF50),
    MUSCULOSKELETAL("Músculo-Esquelético", Icons.Filled.MonitorHeart, 0xFFF44336),
    VISUAL("Salud Visual", Icons.Filled.RemoveRedEye, 0xFF2196F3),
    WORKLOAD("Carga Laboral", Icons.Filled.Work, 0xFFFF9800),
    MENTAL_HEALTH("Salud Mental", Icons.Filled.Psychology, 0xFF9C27B0),
    SLEEP("Sueño", Icons.Filled.NightlightRound, 0xFF3F51B5),
    PHYSICAL_ACTIVITY("Actividad Física", Icons.Filled.DirectionsRun, 0xFF00BCD4),
    WORK_LIFE_BALANCE("Balance Vida-Trabajo", Icons.Filled.Balance, 0xFF8BC34A),
    GENERAL("General", Icons.Filled.HealthAndSafety, 0xFF607D8B);

    companion object {
        fun fromQuestionnaireType(type: QuestionnaireType): ResourceCategory {
            return when (type) {
                QuestionnaireType.ERGONOMIA -> ERGONOMICS
                QuestionnaireType.SINTOMAS_MUSCULARES -> MUSCULOSKELETAL
                QuestionnaireType.SINTOMAS_VISUALES -> VISUAL
                QuestionnaireType.CARGA_TRABAJO -> WORKLOAD
                QuestionnaireType.ESTRES_SALUD_MENTAL -> MENTAL_HEALTH
                QuestionnaireType.HABITOS_SUENO -> SLEEP
                QuestionnaireType.ACTIVIDAD_FISICA -> PHYSICAL_ACTIVITY
                QuestionnaireType.BALANCE_VIDA_TRABAJO -> WORK_LIFE_BALANCE
            }
        }
    }
}

/**
 * Recurso de salud laboral
 */
data class ResourceItem(
    val id: String,
    val type: ResourceType,
    val category: ResourceCategory,
    val title: String,
    val summary: String,
    val content: String, // Markdown o texto enriquecido
    val author: String? = null,
    val source: String, // "OMS", "NIOSH", "Mayo Clinic", etc.
    val sourceUrl: String? = null,
    val publishDate: Long = System.currentTimeMillis(),
    val readTime: String, // "5 min", "10 min"
    val difficulty: ResourceDifficulty = ResourceDifficulty.BASIC,
    val tags: List<String> = emptyList(),
    val isPeerReviewed: Boolean = false,
    val imageUrl: String? = null,
    val videoUrl: String? = null, // YouTube ID o URL
    val pdfUrl: String? = null,
    val relatedResourceIds: List<String> = emptyList(),
    val keyPoints: List<String> = emptyList(), // Puntos clave para vista rápida
    val references: List<String> = emptyList(), // Referencias científicas
    val views: Int = 0,
    val saves: Int = 0,
    val isFavorite: Boolean = false,
    val lastUpdated: Long = System.currentTimeMillis()
)

/**
 * Ejercicio práctico
 */
data class ExerciseResource(
    val id: String,
    val name: String,
    val description: String,
    val category: ResourceCategory,
    val duration: Int, // segundos
    val repetitions: Int,
    val sets: Int = 1,
    val instructions: List<String>,
    val benefits: List<String>,
    val warnings: List<String> = emptyList(),
    val animationUrl: String? = null,
    val thumbnailUrl: String? = null,
    val difficulty: ResourceDifficulty = ResourceDifficulty.BASIC,
    val equipment: List<String> = emptyList() // "Ninguno", "Silla", "Mesa"
)

/**
 * Pregunta frecuente
 */
data class FAQItem(
    val id: String,
    val question: String,
    val answer: String,
    val category: ResourceCategory,
    val relatedResourceIds: List<String> = emptyList(),
    val sources: List<String> = emptyList(),
    val isExpanded: Boolean = false
)

/**
 * Filtros de búsqueda
 */
data class ResourceFilter(
    val types: Set<ResourceType> = emptySet(),
    val categories: Set<ResourceCategory> = emptySet(),
    val difficulties: Set<ResourceDifficulty> = emptySet(),
    val peerReviewedOnly: Boolean = false,
    val favoritesOnly: Boolean = false,
    val hasVideo: Boolean = false,
    val hasDownload: Boolean = false,
    val searchQuery: String = ""
) {
    fun isActive(): Boolean {
        return types.isNotEmpty() ||
                categories.isNotEmpty() ||
                difficulties.isNotEmpty() ||
                peerReviewedOnly ||
                favoritesOnly ||
                hasVideo ||
                hasDownload ||
                searchQuery.isNotBlank()
    }
}

/**
 * Estado de la UI de recursos
 */
sealed class ResourceState {
    object Idle : ResourceState()
    object Loading : ResourceState()
    data class Success(val resources: List<ResourceItem>) : ResourceState()
    data class Error(val message: String) : ResourceState()
}

/**
 * Historial de lectura
 */
data class ReadingHistory(
    val resourceId: String,
    val timestamp: Long,
    val progress: Float = 1.0f, // 0.0 - 1.0
    val completedReading: Boolean = false
)

/**
 * Estadísticas de usuario
 */
data class ResourceStats(
    val totalRead: Int = 0,
    val totalSaved: Int = 0,
    val readingStreak: Int = 0,
    val favoriteCategory: ResourceCategory? = null,
    val minutesSpent: Int = 0
)