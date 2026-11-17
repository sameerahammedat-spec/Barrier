package com.rushgames.barrier.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rushgames.barrier.data.datastore.BarrierPreferences
import com.rushgames.barrier.data.model.User
import com.rushgames.barrier.data.repository.AuthRepository
import com.rushgames.barrier.data.repository.CloudSyncRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val theme: String = "system",
    val strictModeGlobal: Boolean = false,
    val adultProtection: Boolean = false,
    val notificationsEnabled: Boolean = true,
    val currentUser: User? = null,
    val syncMessage: String? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val prefs: BarrierPreferences,
    private val authRepository: AuthRepository,
    private val cloudSyncRepository: CloudSyncRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                prefs.theme,
                prefs.strictModeGlobal,
                prefs.adultProtection,
                authRepository.currentUser
            ) { theme, strict, adult, user ->
                SettingsUiState(
                    theme = theme,
                    strictModeGlobal = strict,
                    adultProtection = adult,
                    currentUser = user
                )
            }.collect { _uiState.value = it }
        }
    }

    fun setTheme(theme: String) = viewModelScope.launch { prefs.setTheme(theme) }
    fun toggleStrictMode() = viewModelScope.launch { prefs.setStrictModeGlobal(!_uiState.value.strictModeGlobal) }
    fun toggleAdultProtection() = viewModelScope.launch { prefs.setAdultProtection(!_uiState.value.adultProtection) }

    fun syncCloudData() {
        val user = _uiState.value.currentUser ?: return
        viewModelScope.launch {
            val res = cloudSyncRepository.syncUserDataToCloud(user.id)
            _uiState.update { it.copy(syncMessage = res.message) }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            authRepository.signOut()
        }
    }
}
