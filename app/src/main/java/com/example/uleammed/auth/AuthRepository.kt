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

    // Guardar cuestionarios específicos
    suspend fun saveErgonomiaQuestionnaire(questionnaire: ErgonomiaQuestionnaire): Result<Unit> {
        return try {
            firestore.collection("ergonomia_questionnaires")
                .document(questionnaire.userId)
                .collection("responses")
                .document(questionnaire.responseId)
                .set(questionnaire)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun saveSintomasMuscularesQuestionnaire(questionnaire: SintomasMuscularesQuestionnaire): Result<Unit> {
        return try {
            firestore.collection("sintomas_musculares_questionnaires")
                .document(questionnaire.userId)
                .collection("responses")
                .document(questionnaire.responseId)
                .set(questionnaire)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun saveSintomasVisualesQuestionnaire(questionnaire: SintomasVisualesQuestionnaire): Result<Unit> {
        return try {
            firestore.collection("sintomas_visuales_questionnaires")
                .document(questionnaire.userId)
                .collection("responses")
                .document(questionnaire.responseId)
                .set(questionnaire)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun saveCargaTrabajoQuestionnaire(questionnaire: CargaTrabajoQuestionnaire): Result<Unit> {
        return try {
            firestore.collection("carga_trabajo_questionnaires")
                .document(questionnaire.userId)
                .collection("responses")
                .document(questionnaire.responseId)
                .set(questionnaire)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun saveEstresSaludMentalQuestionnaire(questionnaire: EstresSaludMentalQuestionnaire): Result<Unit> {
        return try {
            firestore.collection("estres_salud_mental_questionnaires")
                .document(questionnaire.userId)
                .collection("responses")
                .document(questionnaire.responseId)
                .set(questionnaire)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun saveHabitosSuenoQuestionnaire(questionnaire: HabitosSuenoQuestionnaire): Result<Unit> {
        return try {
            firestore.collection("habitos_sueno_questionnaires")
                .document(questionnaire.userId)
                .collection("responses")
                .document(questionnaire.responseId)
                .set(questionnaire)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun saveActividadFisicaQuestionnaire(questionnaire: ActividadFisicaQuestionnaire): Result<Unit> {
        return try {
            firestore.collection("actividad_fisica_questionnaires")
                .document(questionnaire.userId)
                .collection("responses")
                .document(questionnaire.responseId)
                .set(questionnaire)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun saveBalanceVidaTrabajoQuestionnaire(questionnaire: BalanceVidaTrabajoQuestionnaire): Result<Unit> {
        return try {
            firestore.collection("balance_vida_trabajo_questionnaires")
                .document(questionnaire.userId)
                .collection("responses")
                .document(questionnaire.responseId)
                .set(questionnaire)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}