package com.example.uleammed.auth

import com.example.uleammed.HealthQuestionnaire
import com.example.uleammed.User
import com.example.uleammed.questionnaires.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
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

            // Actualizar lastCompletedDate para sincronizar con notificaciones
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
    private suspend fun getUserScheduleConfig(userId: String): Int {
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
    suspend fun getCompletedQuestionnaires(userId: String): Result<Set<String>> {
        return try {
            val currentTime = System.currentTimeMillis()
            val periodDays = getUserScheduleConfig(userId)
            val validityPeriod = TimeUnit.DAYS.toMillis(periodDays.toLong())
            val cutoffTime = currentTime - validityPeriod

            val questionnairesRef = firestore.collection("users")
                .document(userId)
                .collection("questionnaires")
                .get()
                .await()

            // Filtrar cuestionarios vigentes EXCLUYENDO el cuestionario inicial de salud
            val completedTypes = questionnairesRef.documents
                .filter { doc ->
                    // Excluir el cuestionario inicial "salud_general" del conteo de los 8
                    if (doc.id == "salud_general") {
                        return@filter false
                    }
                    val timestamp = doc.getLong("completedAt") ?: 0L
                    val isValid = timestamp >= cutoffTime

                    // Log de diagnóstico para cada cuestionario
                    android.util.Log.d("AuthRepository",
                        "📋 ${doc.id}: completedAt=$timestamp, vigente=$isValid")

                    isValid
                }
                .map { it.id }
                .toSet()

            android.util.Log.d("AuthRepository",
                "✅ Cuestionarios vigentes (sin inicial): $completedTypes - Total: ${completedTypes.size}/8 (válidos por $periodDays días)")
            Result.success(completedTypes)
        } catch (e: Exception) {
            android.util.Log.e("AuthRepository", "❌ Error obteniendo cuestionarios completados", e)
            Result.failure(e)
        }
    }

    /**
     * Verifica si un cuestionario específico está completado Y vigente
     */
    suspend fun isQuestionnaireCompleted(userId: String, questionnaireType: String): Result<Boolean> {
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

            val periodDays = getUserScheduleConfig(userId)
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
    suspend fun getQuestionnaireStatus(userId: String, questionnaireType: String): Result<QuestionnaireStatus> {
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

            val periodDays = getUserScheduleConfig(userId)
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
                    totalPeriodDays = periodDays
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
     * Método auxiliar para actualizar lastCompletedDate en la configuración de notificaciones
     * ✅ CORREGIDO: configRef declarado fuera de los bloques try-catch
     */
    private suspend fun updateLastCompletedDate(userId: String, questionnaireTypeName: String) {
        // ✅ Declarar configRef aquí para que esté en alcance en todos los bloques
        val configRef = firestore.collection("users")
            .document(userId)
            .collection("settings")
            .document("notifications")

        try {
            // Intentar actualizar el documento existente
            configRef.update(
                "lastCompletedDates.$questionnaireTypeName",
                System.currentTimeMillis()
            ).await()

            android.util.Log.d("AuthRepository",
                "✅ Actualizado lastCompletedDate para $questionnaireTypeName")
        } catch (e: Exception) {
            // Si falla (documento no existe), crear el documento con merge
            try {
                configRef.set(
                    mapOf(
                        "lastCompletedDates" to mapOf(
                            questionnaireTypeName to System.currentTimeMillis()
                        )
                    ),
                    SetOptions.merge()
                ).await()

                android.util.Log.d("AuthRepository",
                    "✅ Creado lastCompletedDate inicial para $questionnaireTypeName")
            } catch (e2: Exception) {
                android.util.Log.e("AuthRepository",
                    "❌ Error actualizando lastCompletedDate", e2)
            }
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