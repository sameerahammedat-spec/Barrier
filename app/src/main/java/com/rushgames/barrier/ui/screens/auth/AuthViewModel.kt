package com.rushgames.barrier.ui.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rushgames.barrier.data.model.User
import com.rushgames.barrier.data.repository.AuthRepository
import com.rushgames.barrier.data.repository.CloudSyncRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthUiState(
    val user: User? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isSuccess: Boolean = false,
    val syncMessage: String? = null
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val cloudSyncRepository: CloudSyncRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            authRepository.currentUser.collect { user ->
                _uiState.update { it.copy(user = user) }
            }
        }
    }

    fun loginWithEmail(email: String, pass: String) {
        when {
            !android.util.Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches() ->
                showAuthError("Enter a valid email address.")
            pass.isBlank() -> showAuthError("Enter your password.")
            else -> publishAuthResult(authRepository.loginWithEmail())
        }
    }

    fun signUpWithEmail(email: String, pass: String, name: String) {
        when {
            name.isBlank() -> showAuthError("Enter your name.")
            !android.util.Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches() ->
                showAuthError("Enter a valid email address.")
            pass.length < 6 -> showAuthError("Password must be at least 6 characters.")
            else -> publishAuthResult(authRepository.signUpWithEmail())
        }
    }

    fun signInWithGoogle() {
        publishAuthResult(authRepository.signInWithGoogle())
    }

    private fun publishAuthResult(result: Result<User>) {
        result.fold(
            onSuccess = { user ->
                _uiState.update { it.copy(user = user, isSuccess = true, isLoading = false, errorMessage = null) }
            },
            onFailure = { error -> showAuthError(error.message ?: "Authentication failed.") }
        )
    }

    private fun showAuthError(message: String) {
        _uiState.update { it.copy(isLoading = false, isSuccess = false, errorMessage = message) }
    }

    fun continueAsGuest() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = authRepository.continueAsGuest()
            result.fold(
                onSuccess = { u ->
                    _uiState.update { it.copy(isLoading = false, user = u, isSuccess = true) }
                },
                onFailure = { err ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = err.message) }
                }
            )
        }
    }

    fun syncCloudData() {
        val u = _uiState.value.user ?: return
        viewModelScope.launch {
            val syncRes = cloudSyncRepository.syncUserDataToCloud(u.id)
            _uiState.update { it.copy(syncMessage = syncRes.message) }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            authRepository.signOut()
            _uiState.update { AuthUiState() }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
