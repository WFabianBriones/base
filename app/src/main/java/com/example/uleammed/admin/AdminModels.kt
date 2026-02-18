package com.example.uleammed.admin

/**
 * Roles de usuario en el sistema
 */
enum class UserRole(val displayName: String) {
    USER("Usuario"),
    ADMIN("Administrador"),
    SUPERUSER("Super Usuario");

    companion object {
        fun fromString(value: String?): UserRole {
            return when (value?.uppercase()) {
                "ADMIN" -> ADMIN
                "SUPERUSER" -> SUPERUSER
                else -> USER
            }
        }
    }
}

/**
 * Extensión del modelo User con información de rol
 */
data class UserWithRole(
    val uid: String = "",
    val email: String = "",
    val displayName: String = "",
    val photoUrl: String = "",
    val role: String = "USER", // USER, ADMIN, SUPERUSER
    val createdAt: Long = 0L,
    val lastLogin: Long = 0L,
    val hasCompletedQuestionnaire: Boolean = false
) {
    fun getUserRole(): UserRole = UserRole.fromString(role)
    
    fun isAdmin(): Boolean = role == "ADMIN" || role == "SUPERUSER"
    
    fun isSuperUser(): Boolean = role == "SUPERUSER"
}

/**
 * Modelos de datos para estadísticas
 */
data class AppStatistics(
    val totalUsers: Int,
    val activeUsers: Int,
    val totalQuestionnaires: Int,
    val totalHealthScores: Int,
    val registrationsByMonth: Map<String, Int>,
    val questionnairesByType: Map<String, Int>
)

data class QuestionnaireStatistics(
    val completionByType: Map<String, Int>,
    val totalUsers: Int
) {
    fun getCompletionRate(type: String): Float {
        val completed = completionByType[type] ?: 0
        return if (totalUsers > 0) completed.toFloat() / totalUsers else 0f
    }
}
