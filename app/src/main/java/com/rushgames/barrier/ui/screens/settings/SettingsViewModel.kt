package com.rushgames.barrier.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rushgames.barrier.data.datastore.BarrierPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val theme: String = "system",
    val strictModeGlobal: Boolean = false,
    val adultProtection: Boolean = false,
    val notificationsEnabled: Boolean = true,
    val recoveryPin: String = ""
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val prefs: BarrierPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                prefs.theme,
                prefs.strictModeGlobal,
                prefs.adultProtection,
                prefs.recoveryPin
            ) { theme, strict, adult, pin ->
                SettingsUiState(
                    theme = theme,
                    strictModeGlobal = strict,
                    adultProtection = adult,
                    recoveryPin = pin
                )
            }.collect { _uiState.value = it }
        }
    }

    fun setTheme(theme: String) = viewModelScope.launch { prefs.setTheme(theme) }
    fun toggleStrictMode() = viewModelScope.launch { prefs.setStrictModeGlobal(!_uiState.value.strictModeGlobal) }
    fun toggleAdultProtection() = viewModelScope.launch { prefs.setAdultProtection(!_uiState.value.adultProtection) }
    fun setRecoveryPin(pin: String) = viewModelScope.launch { prefs.setRecoveryPin(pin) }
}
