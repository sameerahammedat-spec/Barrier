package com.rushgames.barrier.ui.screens.protection

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.view.accessibility.AccessibilityManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rushgames.barrier.data.model.Session
import com.rushgames.barrier.data.datastore.BarrierPreferences
import com.rushgames.barrier.data.repository.SessionRepository
import com.rushgames.barrier.service.BarrierAccessibilityService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class ProtectionCenterUiState(
    val activeSession: Session? = null,
    val accessibilityEnabled: Boolean = false,
    val accessibilityServiceConnected: Boolean = false,
    val adultProtectionEnabled: Boolean = false,
    val storageRecoveryNotice: String? = null
) {
    val sessionHasRules: Boolean
        get() = activeSession?.let { session ->
            session.allowedAppPackages.isNotEmpty() ||
                session.blocklistIds.isNotEmpty() ||
                session.blockedWebsites.isNotEmpty() ||
                session.allowedWebsites.isNotEmpty() ||
                session.blockAdultContent ||
                session.websiteFilterMode == com.rushgames.barrier.data.model.WebsiteFilterMode.ADULT_PROTECTION
        } == true

    val sessionRulesEnforced: Boolean
        get() = activeSession?.let { it.endTime > System.currentTimeMillis() } == true &&
            accessibilityEnabled && accessibilityServiceConnected && sessionHasRules &&
            storageRecoveryNotice == null

    val globalAdultFilteringEnforced: Boolean
        get() = adultProtectionEnabled && accessibilityEnabled && accessibilityServiceConnected
}

@HiltViewModel
class ProtectionCenterViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    sessionRepository: SessionRepository,
    preferences: BarrierPreferences
) : ViewModel() {
    private val accessibilityEnabled = MutableStateFlow(false)
    private val sessionClock = flow {
        while (true) {
            emit(Unit)
            delay(15_000)
        }
    }

    val uiState: StateFlow<ProtectionCenterUiState> =
        combine(
            sessionRepository.getActiveSession(),
            accessibilityEnabled,
            BarrierAccessibilityService.connectionStatus,
            combine(preferences.adultProtection, preferences.storageRecoveryNotice) { adult, notice ->
                adult to notice
            },
            sessionClock
        ) { session, isAccessibilityEnabled, isServiceConnected, protectionState, _ ->
            ProtectionCenterUiState(
                activeSession = session?.takeIf { it.endTime > System.currentTimeMillis() },
                accessibilityEnabled = isAccessibilityEnabled,
                accessibilityServiceConnected = isServiceConnected,
                adultProtectionEnabled = protectionState.first,
                storageRecoveryNotice = protectionState.second
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ProtectionCenterUiState()
        )

    fun refreshAccessibilityStatus() {
        val manager = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager
        accessibilityEnabled.value = manager
            ?.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
            .orEmpty()
            .any { service ->
                service.resolveInfo.serviceInfo.packageName == context.packageName &&
                    service.resolveInfo.serviceInfo.name == BarrierAccessibilityService::class.java.name
            }
    }
}
