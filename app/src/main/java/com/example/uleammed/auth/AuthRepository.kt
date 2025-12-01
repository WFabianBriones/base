package com.example.uleammed.auth

import com.example.uleammed.HealthQuestionnaire
import com.example.uleammed.User
import com.example.uleammed.questionnaires.ActividadFisicaQuestionnaire
import com.example.uleammed.questionnaires.BalanceVidaTrabajoQuestionnaire
import com.example.uleammed.questionnaires.CargaTrabajoQuestionnaire
import com.example.uleammed.questionnaires.ErgonomiaQuestionnaire
import com.example.uleammed.questionnaires.EstresSaludMentalQuestionnaire
import com.example.uleammed.questionnaires.HabitosSuenoQuestionnaire
import com.example.uleammed.questionnaires.SintomasMuscularesQuestionnaire
import com.example.uleammed.questionnaires.SintomasVisualesQuestionnaire
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class AuthRepository {
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    val currentUser: FirebaseUser?
        get() = auth.currentUser

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

    suspend fun saveQuestionnaire(questionnaire: HealthQuestionnaire): Result<Unit> {
        return try {
            // ✅ Guardar en ruta correcta para scoring
            firestore.collection("questionnaires")
                .document(questionnaire.userId)
                .set(questionnaire)
                .await()

            firestore.collection("users")
                .document(questionnaire.userId)
                .update("hasCompletedQuestionnaire", true)
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getQuestionnaire(userId: String): Result<HealthQuestionnaire?> {
        return try {
            val doc = firestore.collection("questionnaires")
                .document(userId)
                .get()
                .await()

            val questionnaire = doc.toObject(HealthQuestionnaire::class.java)
            Result.success(questionnaire)
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

    // ===== CUESTIONARIOS ESPECÍFICOS =====
    // ✅ ESTRUCTURA CORREGIDA: users/{userId}/questionnaires/{tipo}

    suspend fun saveErgonomiaQuestionnaire(questionnaire: ErgonomiaQuestionnaire): Result<Unit> {
        return try {
            firestore.collection("users")
                .document(questionnaire.userId)
                .collection("questionnaires")
                .document("ergonomia")
                .set(questionnaire)
                .await()

            android.util.Log.d("AuthRepository", "✅ Ergonomía guardado en: users/${questionnaire.userId}/questionnaires/ergonomia")
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("AuthRepository", "❌ Error guardando ergonomía", e)
            Result.failure(e)
        }
    }

    suspend fun saveSintomasMuscularesQuestionnaire(questionnaire: SintomasMuscularesQuestionnaire): Result<Unit> {
        return try {
            firestore.collection("users")
                .document(questionnaire.userId)
                .collection("questionnaires")
                .document("sintomas_musculares")
                .set(questionnaire)
                .await()

            android.util.Log.d("AuthRepository", "✅ Síntomas musculares guardado")
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("AuthRepository", "❌ Error guardando síntomas musculares", e)
            Result.failure(e)
        }
    }

    suspend fun saveSintomasVisualesQuestionnaire(questionnaire: SintomasVisualesQuestionnaire): Result<Unit> {
        return try {
            firestore.collection("users")
                .document(questionnaire.userId)
                .collection("questionnaires")
                .document("sintomas_visuales")
                .set(questionnaire)
                .await()

            android.util.Log.d("AuthRepository", "✅ Síntomas visuales guardado")
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("AuthRepository", "❌ Error guardando síntomas visuales", e)
            Result.failure(e)
        }
    }

    suspend fun saveCargaTrabajoQuestionnaire(questionnaire: CargaTrabajoQuestionnaire): Result<Unit> {
        return try {
            firestore.collection("users")
                .document(questionnaire.userId)
                .collection("questionnaires")
                .document("carga_trabajo")
                .set(questionnaire)
                .await()

            android.util.Log.d("AuthRepository", "✅ Carga de trabajo guardado")
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("AuthRepository", "❌ Error guardando carga trabajo", e)
            Result.failure(e)
        }
    }

    suspend fun saveEstresSaludMentalQuestionnaire(questionnaire: EstresSaludMentalQuestionnaire): Result<Unit> {
        return try {
            firestore.collection("users")
                .document(questionnaire.userId)
                .collection("questionnaires")
                .document("estres_salud_mental")
                .set(questionnaire)
                .await()

            android.util.Log.d("AuthRepository", "✅ Estrés y salud mental guardado")
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("AuthRepository", "❌ Error guardando estrés", e)
            Result.failure(e)
        }
    }

    suspend fun saveHabitosSuenoQuestionnaire(questionnaire: HabitosSuenoQuestionnaire): Result<Unit> {
        return try {
            firestore.collection("users")
                .document(questionnaire.userId)
                .collection("questionnaires")
                .document("habitos_sueno")
                .set(questionnaire)
                .await()

            android.util.Log.d("AuthRepository", "✅ Hábitos de sueño guardado")
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("AuthRepository", "❌ Error guardando hábitos sueño", e)
            Result.failure(e)
        }
    }

    suspend fun saveActividadFisicaQuestionnaire(questionnaire: ActividadFisicaQuestionnaire): Result<Unit> {
        return try {
            firestore.collection("users")
                .document(questionnaire.userId)
                .collection("questionnaires")
                .document("actividad_fisica")
                .set(questionnaire)
                .await()

            android.util.Log.d("AuthRepository", "✅ Actividad física guardado")
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("AuthRepository", "❌ Error guardando actividad física", e)
            Result.failure(e)
        }
    }

    suspend fun saveBalanceVidaTrabajoQuestionnaire(questionnaire: BalanceVidaTrabajoQuestionnaire): Result<Unit> {
        return try {
            firestore.collection("users")
                .document(questionnaire.userId)
                .collection("questionnaires")
                .document("balance_vida_trabajo")
                .set(questionnaire)
                .await()

            android.util.Log.d("AuthRepository", "✅ Balance vida-trabajo guardado")
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("AuthRepository", "❌ Error guardando balance", e)
            Result.failure(e)
        }
    }
}