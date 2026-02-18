package com.example.uleammed.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel para el dashboard administrativo
 */
class AdminDashboardViewModel(
    private val repository: AdminRepository = AdminRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<AdminDashboardState>(AdminDashboardState.Loading)
    val uiState: StateFlow<AdminDashboardState> = _uiState

    private val _userRole = MutableStateFlow<UserRole>(UserRole.USER)
    val userRole: StateFlow<UserRole> = _userRole

    init {
        checkUserRole()
        loadDashboardData()
    }

    private fun checkUserRole() {
        viewModelScope.launch {
            val isSuperUser = repository.isCurrentUserSuperUser()
            val isAdmin = repository.isCurrentUserAdmin()

            _userRole.value = when {
                isSuperUser -> UserRole.SUPERUSER
                isAdmin -> UserRole.ADMIN
                else -> UserRole.USER
            }
        }
    }

    fun loadDashboardData() {
        viewModelScope.launch {
            _uiState.value = AdminDashboardState.Loading

            try {
                // Cargar estadísticas generales
                val statsResult = repository.getAppStatistics()
                val stats = statsResult.getOrNull()

                // Cargar estadísticas de cuestionarios
                val questionnaireResult = repository.getQuestionnaireStatistics()
                val questionnaireStats = questionnaireResult.getOrNull()

                // Cargar distribución de riesgo
                val riskResult = repository.getRiskDistribution()
                val riskDistribution = riskResult.getOrNull()

                if (stats != null && questionnaireStats != null && riskDistribution != null) {
                    _uiState.value = AdminDashboardState.Success(
                        statistics = stats,
                        questionnaireStats = questionnaireStats,
                        riskDistribution = riskDistribution
                    )
                } else {
                    _uiState.value = AdminDashboardState.Error("Error al cargar datos")
                }
            } catch (e: Exception) {
                _uiState.value = AdminDashboardState.Error(e.message ?: "Error desconocido")
            }
        }
    }

    fun loadAllUsers() {
        viewModelScope.launch {
            try {
                val result = repository.getAllUsers()
                result.onSuccess { users ->
                    _uiState.value = AdminDashboardState.UsersList(users)
                }
            } catch (e: Exception) {
                _uiState.value = AdminDashboardState.Error(e.message ?: "Error al cargar usuarios")
            }
        }
    }
}

/**
 * Estados del dashboard administrativo
 */
sealed class AdminDashboardState {
    object Loading : AdminDashboardState()
    
    data class Success(
        val statistics: AppStatistics,
        val questionnaireStats: QuestionnaireStatistics,
        val riskDistribution: Map<String, Int>
    ) : AdminDashboardState()
    
    data class UsersList(
        val users: List<UserWithRole>
    ) : AdminDashboardState()
    
    data class Error(val message: String) : AdminDashboardState()
}
