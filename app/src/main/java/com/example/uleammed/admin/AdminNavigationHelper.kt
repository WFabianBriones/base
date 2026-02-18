package com.example.uleammed.admin

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Helper para manejar la navegación automática según el rol del usuario
 * 
 * USO:
 * En tu ViewModel de login, después de un login exitoso:
 * 
 * val destination = AdminNavigationHelper.getPostLoginDestination()
 * // destination será "admin_dashboard" para admins/superuser
 * // o "home" para usuarios normales
 */
object AdminNavigationHelper {
    
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    
    /**
     * Determina a dónde debe navegar el usuario después del login
     * basándose en su rol
     * 
     * @return "admin_dashboard" si es ADMIN o SUPERUSER, "home" si es USER
     */
    suspend fun getPostLoginDestination(): String {
        return try {
            val userId = auth.currentUser?.uid ?: return "home"
            
            val userDoc = firestore.collection("users")
                .document(userId)
                .get()
                .await()
            
            val role = userDoc.getString("role") ?: "USER"
            
            when (role) {
                "ADMIN", "SUPERUSER" -> "admin_dashboard"
                else -> "home"
            }
        } catch (e: Exception) {
            android.util.Log.e("AdminNavHelper", "Error obteniendo destino", e)
            "home" // Por defecto va al home si hay error
        }
    }
    
    /**
     * Verifica si el usuario actual es admin o superuser
     */
    suspend fun isCurrentUserAdmin(): Boolean {
        return try {
            val userId = auth.currentUser?.uid ?: return false
            
            val userDoc = firestore.collection("users")
                .document(userId)
                .get()
                .await()
            
            val role = userDoc.getString("role") ?: "USER"
            role == "ADMIN" || role == "SUPERUSER"
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Verifica si el usuario actual es superuser
     */
    suspend fun isCurrentUserSuperUser(): Boolean {
        return try {
            val userId = auth.currentUser?.uid ?: return false
            
            val userDoc = firestore.collection("users")
                .document(userId)
                .get()
                .await()
            
            val role = userDoc.getString("role") ?: "USER"
            role == "SUPERUSER"
        } catch (e: Exception) {
            false
        }
    }
}
