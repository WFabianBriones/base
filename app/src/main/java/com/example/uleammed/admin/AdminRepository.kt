package com.example.uleammed.admin

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

/**
 * Repository para gestión de administradores y estadísticas
 */
class AdminRepository {
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    companion object {
        private const val TAG = "AdminRepository"
    }

    // ===== VERIFICACIÓN DE ROLES =====

    /**
     * Verifica si el usuario actual es administrador o super usuario
     */
    suspend fun isCurrentUserAdmin(): Boolean {
        val userId = auth.currentUser?.uid ?: return false
        return try {
            val userDoc = firestore.collection("users")
                .document(userId)
                .get()
                .await()

            val role = userDoc.getString("role") ?: "USER"
            role == "ADMIN" || role == "SUPERUSER"
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error verificando rol de admin", e)
            false
        }
    }

    /**
     * Verifica si el usuario actual es super usuario
     */
    suspend fun isCurrentUserSuperUser(): Boolean {
        val userId = auth.currentUser?.uid ?: return false
        return try {
            val userDoc = firestore.collection("users")
                .document(userId)
                .get()
                .await()

            val role = userDoc.getString("role") ?: "USER"
            role == "SUPERUSER"
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error verificando rol de super usuario", e)
            false
        }
    }

    // ===== GESTIÓN DE USUARIOS =====

    /**
     * Obtener todos los usuarios (solo para admins)
     */
    suspend fun getAllUsers(): Result<List<UserWithRole>> {
        return try {
            if (!isCurrentUserAdmin()) {
                return Result.failure(SecurityException("No tienes permisos de administrador"))
            }

            val snapshot = firestore.collection("users")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()

            val users = snapshot.documents.mapNotNull { doc ->
                UserWithRole(
                    uid = doc.id,
                    email = doc.getString("email") ?: "",
                    displayName = doc.getString("displayName") ?: "",
                    photoUrl = doc.getString("photoUrl") ?: "",
                    role = doc.getString("role") ?: "USER",
                    createdAt = doc.getLong("createdAt") ?: 0L,
                    lastLogin = doc.getLong("lastLogin") ?: 0L,
                    hasCompletedQuestionnaire = doc.getBoolean("hasCompletedQuestionnaire") ?: false
                )
            }

            android.util.Log.d(TAG, "✅ ${users.size} usuarios obtenidos")
            Result.success(users)
        } catch (e: Exception) {
            android.util.Log.e(TAG, "❌ Error obteniendo usuarios", e)
            Result.failure(e)
        }
    }

    /**
     * Crear un nuevo administrador (solo super usuario)
     */
    suspend fun createAdmin(email: String, password: String, displayName: String): Result<String> {
        return try {
            if (!isCurrentUserSuperUser()) {
                return Result.failure(SecurityException("Solo el super usuario puede crear administradores"))
            }

            // Crear usuario en Firebase Auth
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val newUserId = result.user?.uid ?: throw Exception("Error al crear usuario")

            // Crear documento en Firestore con rol ADMIN
            val adminUser = mapOf(
                "uid" to newUserId,
                "email" to email,
                "displayName" to displayName,
                "photoUrl" to "",
                "role" to "ADMIN",
                "createdAt" to System.currentTimeMillis(),
                "lastLogin" to 0L,
                "hasCompletedQuestionnaire" to false
            )

            firestore.collection("users")
                .document(newUserId)
                .set(adminUser)
                .await()

            android.util.Log.d(TAG, "✅ Administrador creado: $email")
            Result.success(newUserId)
        } catch (e: Exception) {
            android.util.Log.e(TAG, "❌ Error creando administrador", e)
            Result.failure(e)
        }
    }

    /**
     * Cambiar el rol de un usuario (solo super usuario)
     */
    suspend fun changeUserRole(userId: String, newRole: UserRole): Result<Unit> {
        return try {
            if (!isCurrentUserSuperUser()) {
                return Result.failure(SecurityException("Solo el super usuario puede cambiar roles"))
            }

            firestore.collection("users")
                .document(userId)
                .update("role", newRole.name)
                .await()

            android.util.Log.d(TAG, "✅ Rol actualizado a ${newRole.displayName}")
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e(TAG, "❌ Error cambiando rol", e)
            Result.failure(e)
        }
    }

    /**
     * Eliminar usuario (solo super usuario)
     */
    suspend fun deleteUser(userId: String): Result<Unit> {
        return try {
            if (!isCurrentUserSuperUser()) {
                return Result.failure(SecurityException("Solo el super usuario puede eliminar usuarios"))
            }

            // Eliminar documento de Firestore
            firestore.collection("users")
                .document(userId)
                .delete()
                .await()

            android.util.Log.d(TAG, "✅ Usuario eliminado")
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e(TAG, "❌ Error eliminando usuario", e)
            Result.failure(e)
        }
    }

    // ===== ESTADÍSTICAS Y ANALYTICS =====

    /**
     * Obtener estadísticas generales de la aplicación
     */
    suspend fun getAppStatistics(): Result<AppStatistics> {
        return try {
            if (!isCurrentUserAdmin()) {
                return Result.failure(SecurityException("No tienes permisos de administrador"))
            }

            // Total de usuarios
            val usersSnapshot = firestore.collection("users").get().await()
            val totalUsers = usersSnapshot.size()

            // Usuarios activos (con cuestionarios completados)
            val activeUsers = usersSnapshot.documents.count { doc ->
                doc.getBoolean("hasCompletedQuestionnaire") == true
            }

            // Total de cuestionarios completados
            var totalQuestionnaires = 0
            for (userDoc in usersSnapshot.documents) {
                val questionnairesSnapshot = firestore.collection("users")
                    .document(userDoc.id)
                    .collection("questionnaires")
                    .get()
                    .await()
                totalQuestionnaires += questionnairesSnapshot.size()
            }

            // Total de puntuaciones de salud
            val scoresSnapshot = firestore.collection("health_scores").get().await()
            val totalScores = scoresSnapshot.size()

            val statistics = AppStatistics(
                totalUsers = totalUsers,
                activeUsers = activeUsers,
                totalQuestionnaires = totalQuestionnaires,
                totalHealthScores = totalScores,
                registrationsByMonth = emptyMap(),
                questionnairesByType = emptyMap()
            )

            android.util.Log.d(TAG, "✅ Estadísticas obtenidas")
            Result.success(statistics)
        } catch (e: Exception) {
            android.util.Log.e(TAG, "❌ Error obteniendo estadísticas", e)
            Result.failure(e)
        }
    }

    /**
     * Obtener estadísticas detalladas de cuestionarios
     */
    suspend fun getQuestionnaireStatistics(): Result<QuestionnaireStatistics> {
        return try {
            if (!isCurrentUserAdmin()) {
                return Result.failure(SecurityException("No tienes permisos de administrador"))
            }

            val questionnaireTypes = listOf(
                "salud_general",
                "ergonomia",
                "sintomas_musculares",
                "sintomas_visuales",
                "carga_trabajo",
                "estres_salud_mental",
                "habitos_sueno",
                "actividad_fisica",
                "balance_vida_trabajo"
            )

            val countByType = mutableMapOf<String, Int>()

            val usersSnapshot = firestore.collection("users").get().await()
            for (userDoc in usersSnapshot.documents) {
                for (type in questionnaireTypes) {
                    val exists = firestore.collection("users")
                        .document(userDoc.id)
                        .collection("questionnaires")
                        .document(type)
                        .get()
                        .await()
                        .exists()

                    if (exists) {
                        countByType[type] = (countByType[type] ?: 0) + 1
                    }
                }
            }

            val statistics = QuestionnaireStatistics(
                completionByType = countByType,
                totalUsers = usersSnapshot.size()
            )

            android.util.Log.d(TAG, "✅ Estadísticas de cuestionarios obtenidas")
            Result.success(statistics)
        } catch (e: Exception) {
            android.util.Log.e(TAG, "❌ Error obteniendo estadísticas de cuestionarios", e)
            Result.failure(e)
        }
    }

    /**
     * Obtener distribución de niveles de riesgo
     */
    suspend fun getRiskDistribution(): Result<Map<String, Int>> {
        return try {
            if (!isCurrentUserAdmin()) {
                return Result.failure(SecurityException("No tienes permisos de administrador"))
            }

            val scoresSnapshot = firestore.collection("health_scores").get().await()
            
            val distribution = mutableMapOf(
                "LOW" to 0,
                "MODERATE" to 0,
                "HIGH" to 0,
                "CRITICAL" to 0
            )

            for (doc in scoresSnapshot.documents) {
                val overallRisk = doc.getString("overallRisk") ?: "LOW"
                distribution[overallRisk] = (distribution[overallRisk] ?: 0) + 1
            }

            android.util.Log.d(TAG, "✅ Distribución de riesgo obtenida")
            Result.success(distribution)
        } catch (e: Exception) {
            android.util.Log.e(TAG, "❌ Error obteniendo distribución de riesgo", e)
            Result.failure(e)
        }
    }
}
