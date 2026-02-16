package com.example.uleammed.auth

import com.example.uleammed.HealthQuestionnaire
import com.example.uleammed.User
import com.example.uleammed.questionnaires.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit

class AuthRepository {
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    val currentUser: FirebaseUser?
        get() = auth.currentUser

    // ===== MÉTODOS DE AUTENTICACIÓN =====

    suspend fun registerWithEmail(email: String, password: String, displayName: String): Result<User> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val firebaseUser = result.user ?: throw Exception("Error al crear usuario")

            val user = User(
                uid = firebaseUser.uid,
                email = email,
                displayName = displayName,
                hasCompletedQuestionnaire = false
            )

            firestore.collection("users")
                .document(firebaseUser.uid)
                .set(user)
                .await()

            // ✅ NUEVO: Inicializar configuración de notificaciones
            initializeNotificationSettings(firebaseUser.uid)

            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun loginWithEmail(email: String, password: String): Result<User> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            val firebaseUser = result.user ?: throw Exception("Error al iniciar sesión")

            val userDoc = firestore.collection("users")
                .document(firebaseUser.uid)
                .get()
                .await()

            val user = userDoc.toObject(User::class.java) ?: throw Exception("Usuario no encontrado")

            // ✅ NUEVO: Verificar que settings/notifications exista (para usuarios antiguos)
            initializeNotificationSettings(firebaseUser.uid)

            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signInWithGoogle(idToken: String): Result<User> {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val result = auth.signInWithCredential(credential).await()
            val firebaseUser = result.user ?: throw Exception("Error al iniciar sesión con Google")

            val userDoc = firestore.collection("users")
                .document(firebaseUser.uid)
                .get()
                .await()

            val user = if (userDoc.exists()) {
                userDoc.toObject(User::class.java) ?: throw Exception("Error al cargar usuario")
            } else {
                val newUser = User(
                    uid = firebaseUser.uid,
                    email = firebaseUser.email ?: "",
                    displayName = firebaseUser.displayName ?: "",
                    photoUrl = firebaseUser.photoUrl?.toString() ?: "",
                    hasCompletedQuestionnaire = false
                )
                firestore.collection("users")
                    .document(firebaseUser.uid)
                    .set(newUser)
                    .await()
                newUser
            }

            // ✅ NUEVO: Inicializar configuración de notificaciones
            initializeNotificationSettings(firebaseUser.uid)

            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUserData(userId: String): Result<User> {
        return try {
            val doc = firestore.collection("users")
                .document(userId)
                .get()
                .await()

            val user = doc.toObject(User::class.java) ?: throw Exception("Usuario no encontrado")
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateUserProfile(userId: String, updates: Map<String, Any>): Result<Unit> {
        return try {
            firestore.collection("users")
                .document(userId)
                .update(updates)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun signOut() {
        auth.signOut()
    }

    suspend fun needsQuestionnaire(): Boolean {
        val userId = currentUser?.uid ?: return false
        return try {
            val userDoc = firestore.collection("users")
                .document(userId)
                .get()
                .await()

            val user = userDoc.toObject(User::class.java)
            !(user?.hasCompletedQuestionnaire ?: false)
        } catch (e: Exception) {
            false
        }
    }

    // ===== CUESTIONARIO INICIAL DE SALUD =====

    suspend fun saveQuestionnaire(questionnaire: HealthQuestionnaire): Result<Unit> {
        return try {
            val questionnaireWithTimestamp = questionnaire.copy(
                completedAt = System.currentTimeMillis()
            )

            firestore.collection("users")
                .document(questionnaire.userId)
                .collection("questionnaires")
                .document("salud_general")
                .set(questionnaireWithTimestamp)
                .await()

            firestore.collection("users")
                .document(questionnaire.userId)
                .update("hasCompletedQuestionnaire", true)
                .await()

            // ✅ AGREGAR ESTA LÍNEA:
            updateLastCompletedDate(questionnaire.userId, "SALUD_GENERAL")

            android.util.Log.d("AuthRepository", "✅ Cuestionario inicial guardado con timestamp")
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("AuthRepository", "❌ Error guardando cuestionario inicial", e)
            Result.failure(e)
        }
    }

    suspend fun getQuestionnaire(userId: String): Result<HealthQuestionnaire?> {
        return try {
            val doc = firestore.collection("users")
                .document(userId)
                .collection("questionnaires")
                .document("salud_general")
                .get()
                .await()

            val questionnaire = doc.toObject(HealthQuestionnaire::class.java)
            Result.success(questionnaire)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ===== GESTIÓN DE ESTADO DE CUESTIONARIOS =====

    /**
     * Obtiene la configuración de periodicidad del usuario
     */
    suspend fun getUserScheduleConfig(userId: String): Int {
        return try {
            val doc = firestore.collection("users")
                .document(userId)
                .collection("settings")
                .document("notifications")
                .get()
                .await()

            val periodDays = doc.getLong("periodDays")?.toInt() ?: 7
            android.util.Log.d("AuthRepository", "✅ Período de usuario: $periodDays días")
            periodDays
        } catch (e: Exception) {
            android.util.Log.w("AuthRepository", "⚠️ No se pudo obtener configuración, usando 7 días por defecto", e)
            7
        }
    }

    /**
     * Obtiene el conjunto de tipos de cuestionarios que están completados Y vigentes
     * Usa la configuración personalizada del usuario (periodDays)
     */
    suspend fun getCompletedQuestionnaires(
        userId: String,
        periodDays: Int  // ✅ Nuevo parámetro
    ): Result<Set<String>> {
        return try {
            val currentTime = System.currentTimeMillis()
            val validityPeriod = TimeUnit.DAYS.toMillis(periodDays.toLong())
            val cutoffTime = currentTime - validityPeriod

            val questionnairesRef = firestore.collection("users")
                .document(userId)
                .collection("questionnaires")
                .get()
                .await()

            val completedTypes = questionnairesRef.documents
                .filter { doc ->
                    val timestamp = doc.getLong("completedAt") ?: 0L
                    timestamp >= cutoffTime
                }
                .map { it.id }
                .toSet()

            android.util.Log.d("AuthRepository",
                "✅ Cuestionarios vigentes: $completedTypes (válidos por $periodDays días)")
            Result.success(completedTypes)
        } catch (e: Exception) {
            android.util.Log.e("AuthRepository", "❌ Error obteniendo cuestionarios completados", e)
            Result.failure(e)
        }
    }

    /**
     * Verifica si un cuestionario específico está completado Y vigente
     */
    suspend fun isQuestionnaireCompleted(
        userId: String,
        questionnaireType: String,
        periodDays: Int  // ✅ Nuevo parámetro
    ): Result<Boolean> {
        return try {
            val doc = firestore.collection("users")
                .document(userId)
                .collection("questionnaires")
                .document(questionnaireType)
                .get()
                .await()

            if (!doc.exists()) {
                return Result.success(false)
            }

            val timestamp = doc.getLong("completedAt") ?: 0L
            val currentTime = System.currentTimeMillis()
            val validityPeriod = TimeUnit.DAYS.toMillis(periodDays.toLong())
            val cutoffTime = currentTime - validityPeriod

            val isValid = timestamp >= cutoffTime

            if (!isValid) {
                android.util.Log.d("AuthRepository",
                    "⏰ Cuestionario $questionnaireType expirado (completado hace más de $periodDays días)")
            }

            Result.success(isValid)
        } catch (e: Exception) {
            android.util.Log.e("AuthRepository", "❌ Error verificando cuestionario $questionnaireType", e)
            Result.failure(e)
        }
    }

    /**
     * Obtiene información detallada sobre el estado de los cuestionarios
     */
    suspend fun getQuestionnaireStatus(
        userId: String,
        questionnaireType: String,
        periodDays: Int  // ✅ Nuevo parámetro
    ): Result<QuestionnaireStatus> {
        return try {
            val doc = firestore.collection("users")
                .document(userId)
                .collection("questionnaires")
                .document(questionnaireType)
                .get()
                .await()

            if (!doc.exists()) {
                return Result.success(QuestionnaireStatus.NotCompleted)
            }

            val timestamp = doc.getLong("completedAt") ?: 0L
            val currentTime = System.currentTimeMillis()
            val validityPeriod = TimeUnit.DAYS.toMillis(periodDays.toLong())
            val cutoffTime = currentTime - validityPeriod

            if (timestamp >= cutoffTime) {
                val daysRemaining = TimeUnit.MILLISECONDS.toDays(
                    (timestamp + validityPeriod) - currentTime
                ).toInt()

                Result.success(QuestionnaireStatus.Completed(
                    completedAt = timestamp,
                    daysRemaining = daysRemaining,
                    totalPeriodDays = periodDays  // ✅ Usa el parámetro
                ))
            } else {
                Result.success(QuestionnaireStatus.Expired(
                    lastCompletedAt = timestamp
                ))
            }
        } catch (e: Exception) {
            android.util.Log.e("AuthRepository", "❌ Error obteniendo estado del cuestionario", e)
            Result.failure(e)
        }
    }

    /**
     * ✅ NUEVO: Inicializa el documento settings/notifications para un usuario
     * Llamar esto cuando se crea una cuenta nueva o en el primer inicio de sesión
     */
    private suspend fun initializeNotificationSettings(userId: String) {
        try {
            val settingsRef = firestore.collection("users")
                .document(userId)
                .collection("settings")
                .document("notifications")

            // Verificar si ya existe
            val doc = settingsRef.get().await()

            if (!doc.exists()) {
                // Crear documento inicial
                val initialConfig = mapOf(
                    "periodDays" to 7,  // Período por defecto: 7 días
                    "preferredHour" to 9, // Hora por defecto: 9 AM
                    "preferredMinute" to 0,
                    "showRemindersInApp" to true,
                    "lastCompletedDates" to mapOf<String, Long>(), // Mapa vacío
                    "enabledQuestionnaires" to listOf(
                        "ERGONOMIA",
                        "SINTOMAS_MUSCULARES",
                        "SINTOMAS_VISUALES",
                        "CARGA_TRABAJO",
                        "ESTRES_SALUD_MENTAL",
                        "HABITOS_SUENO",
                        "ACTIVIDAD_FISICA",
                        "BALANCE_VIDA_TRABAJO"
                    ),
                    "createdAt" to FieldValue.serverTimestamp()
                )

                settingsRef.set(initialConfig).await()

                android.util.Log.d("AuthRepository",
                    "✅ Documento settings/notifications creado para usuario: $userId")
            } else {
                android.util.Log.d("AuthRepository",
                    "✓ settings/notifications ya existe para usuario: $userId")
            }
        } catch (e: Exception) {
            android.util.Log.e("AuthRepository",
                "❌ Error inicializando settings/notifications", e)
            // No lanzar excepción para no bloquear el login/registro
        }
    }

    /**
     * ✅ MEJORADO: Actualizar lastCompletedDate usando set con merge
     * Esto crea el documento si no existe y solo actualiza el campo específico
     */
    private suspend fun updateLastCompletedDate(userId: String, questionnaireTypeName: String) {
        val configRef = firestore.collection("users")
            .document(userId)
            .collection("settings")
            .document("notifications")

        try {
            // ✅ SIEMPRE usar set con merge (es más seguro que update)
            configRef.set(
                mapOf(
                    "lastCompletedDates" to mapOf(
                        questionnaireTypeName to System.currentTimeMillis()
                    )
                ),
                SetOptions.merge() // ← Esto crea el documento si no existe
            ).await()

            android.util.Log.d("AuthRepository",
                "✅ Actualizado/Creado lastCompletedDate para $questionnaireTypeName")
        } catch (e: Exception) {
            android.util.Log.e("AuthRepository",
                "❌ Error actualizando lastCompletedDate para $questionnaireTypeName", e)
            throw e
        }
    }

    // ===== CUESTIONARIOS ESPECÍFICOS CON TIMESTAMP =====

    suspend fun saveErgonomiaQuestionnaire(questionnaire: ErgonomiaQuestionnaire): Result<Unit> {
        return try {
            val questionnaireWithTimestamp = questionnaire.copy(
                completedAt = System.currentTimeMillis()
            )

            firestore.collection("users")
                .document(questionnaire.userId)
                .collection("questionnaires")
                .document("ergonomia")
                .set(questionnaireWithTimestamp)
                .await()

            updateLastCompletedDate(questionnaire.userId, "ERGONOMIA")

            android.util.Log.d("AuthRepository", "✅ Ergonomía guardado con timestamp")
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("AuthRepository", "❌ Error guardando ergonomía", e)
            Result.failure(e)
        }
    }

    suspend fun saveSintomasMuscularesQuestionnaire(questionnaire: SintomasMuscularesQuestionnaire): Result<Unit> {
        return try {
            val questionnaireWithTimestamp = questionnaire.copy(
                completedAt = System.currentTimeMillis()
            )

            firestore.collection("users")
                .document(questionnaire.userId)
                .collection("questionnaires")
                .document("sintomas_musculares")
                .set(questionnaireWithTimestamp)
                .await()

            updateLastCompletedDate(questionnaire.userId, "SINTOMAS_MUSCULARES")

            android.util.Log.d("AuthRepository", "✅ Síntomas musculares guardado con timestamp")
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("AuthRepository", "❌ Error guardando síntomas musculares", e)
            Result.failure(e)
        }
    }

    suspend fun saveSintomasVisualesQuestionnaire(questionnaire: SintomasVisualesQuestionnaire): Result<Unit> {
        return try {
            val questionnaireWithTimestamp = questionnaire.copy(
                completedAt = System.currentTimeMillis()
            )

            firestore.collection("users")
                .document(questionnaire.userId)
                .collection("questionnaires")
                .document("sintomas_visuales")
                .set(questionnaireWithTimestamp)
                .await()

            updateLastCompletedDate(questionnaire.userId, "SINTOMAS_VISUALES")

            android.util.Log.d("AuthRepository", "✅ Síntomas visuales guardado con timestamp")
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("AuthRepository", "❌ Error guardando síntomas visuales", e)
            Result.failure(e)
        }
    }

    suspend fun saveCargaTrabajoQuestionnaire(questionnaire: CargaTrabajoQuestionnaire): Result<Unit> {
        return try {
            val questionnaireWithTimestamp = questionnaire.copy(
                completedAt = System.currentTimeMillis()
            )

            firestore.collection("users")
                .document(questionnaire.userId)
                .collection("questionnaires")
                .document("carga_trabajo")
                .set(questionnaireWithTimestamp)
                .await()

            updateLastCompletedDate(questionnaire.userId, "CARGA_TRABAJO")

            android.util.Log.d("AuthRepository", "✅ Carga de trabajo guardado con timestamp")
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("AuthRepository", "❌ Error guardando carga trabajo", e)
            Result.failure(e)
        }
    }

    suspend fun saveEstresSaludMentalQuestionnaire(questionnaire: EstresSaludMentalQuestionnaire): Result<Unit> {
        return try {
            val questionnaireWithTimestamp = questionnaire.copy(
                completedAt = System.currentTimeMillis()
            )

            firestore.collection("users")
                .document(questionnaire.userId)
                .collection("questionnaires")
                .document("estres_salud_mental")
                .set(questionnaireWithTimestamp)
                .await()

            updateLastCompletedDate(questionnaire.userId, "ESTRES_SALUD_MENTAL")

            android.util.Log.d("AuthRepository", "✅ Estrés y salud mental guardado con timestamp")
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("AuthRepository", "❌ Error guardando estrés", e)
            Result.failure(e)
        }
    }

    suspend fun saveHabitosSuenoQuestionnaire(questionnaire: HabitosSuenoQuestionnaire): Result<Unit> {
        return try {
            val questionnaireWithTimestamp = questionnaire.copy(
                completedAt = System.currentTimeMillis()
            )

            firestore.collection("users")
                .document(questionnaire.userId)
                .collection("questionnaires")
                .document("habitos_sueno")
                .set(questionnaireWithTimestamp)
                .await()

            updateLastCompletedDate(questionnaire.userId, "HABITOS_SUENO")

            android.util.Log.d("AuthRepository", "✅ Hábitos de sueño guardado con timestamp")
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("AuthRepository", "❌ Error guardando hábitos sueño", e)
            Result.failure(e)
        }
    }

    suspend fun saveActividadFisicaQuestionnaire(questionnaire: ActividadFisicaQuestionnaire): Result<Unit> {
        return try {
            val questionnaireWithTimestamp = questionnaire.copy(
                completedAt = System.currentTimeMillis()
            )

            firestore.collection("users")
                .document(questionnaire.userId)
                .collection("questionnaires")
                .document("actividad_fisica")
                .set(questionnaireWithTimestamp)
                .await()

            updateLastCompletedDate(questionnaire.userId, "ACTIVIDAD_FISICA")

            android.util.Log.d("AuthRepository", "✅ Actividad física guardado con timestamp")
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("AuthRepository", "❌ Error guardando actividad física", e)
            Result.failure(e)
        }
    }

    suspend fun saveBalanceVidaTrabajoQuestionnaire(questionnaire: BalanceVidaTrabajoQuestionnaire): Result<Unit> {
        return try {
            val questionnaireWithTimestamp = questionnaire.copy(
                completedAt = System.currentTimeMillis()
            )

            firestore.collection("users")
                .document(questionnaire.userId)
                .collection("questionnaires")
                .document("balance_vida_trabajo")
                .set(questionnaireWithTimestamp)
                .await()

            updateLastCompletedDate(questionnaire.userId, "BALANCE_VIDA_TRABAJO")

            android.util.Log.d("AuthRepository", "✅ Balance vida-trabajo guardado con timestamp")
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("AuthRepository", "❌ Error guardando balance", e)
            Result.failure(e)
        }
    }
    /**
     * Eliminar un cuestionario completado (permite volver a completarlo)
     */
    suspend fun deleteQuestionnaire(userId: String, questionnaireType: String): Result<Unit> {
        return try {
            // Eliminar documento del cuestionario
            firestore.collection("users")
                .document(userId)
                .collection("questionnaires")
                .document(questionnaireType)
                .delete()
                .await()

            // Eliminar lastCompletedDate de settings/notifications
            val configRef = firestore.collection("users")
                .document(userId)
                .collection("settings")
                .document("notifications")

            try {
                val configDoc = configRef.get().await()
                if (configDoc.exists()) {
                    val lastCompletedDates = configDoc.get("lastCompletedDates") as? Map<String, Long> ?: emptyMap()
                    val questionnaireTypeName = questionnaireType.uppercase().replace("_", "_")

                    val updatedDates = lastCompletedDates.toMutableMap()
                    updatedDates.remove(questionnaireTypeName)

                    configRef.update("lastCompletedDates", updatedDates).await()
                }
            } catch (e: Exception) {
                android.util.Log.w("AuthRepository", "⚠️ No se pudo actualizar lastCompletedDates", e)
            }

            android.util.Log.d("AuthRepository", "✅ Cuestionario $questionnaireType eliminado")
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("AuthRepository", "❌ Error eliminando cuestionario $questionnaireType", e)
            Result.failure(e)
        }
    }
}

/**
 * Sealed class para representar el estado de un cuestionario
 */
sealed class QuestionnaireStatus {
    object NotCompleted : QuestionnaireStatus()

    data class Completed(
        val completedAt: Long,
        val daysRemaining: Int,
        val totalPeriodDays: Int
    ) : QuestionnaireStatus()

    data class Expired(
        val lastCompletedAt: Long
    ) : QuestionnaireStatus()
}