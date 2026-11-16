package com.rushgames.barrier.ui.screens.sessions

import android.content.Context
import android.content.pm.PackageManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rushgames.barrier.data.model.*
import com.rushgames.barrier.data.repository.BlocklistRepository
import com.rushgames.barrier.data.repository.SessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CreateSessionUiState(
    val name: String = "Focus Session",
    val durationHours: Int = 1,
    val durationMinutes: Int = 0,
    val isLocked: Boolean = false,
    val isStrictMode: Boolean = false,
    val blockAdultContent: Boolean = false,
    val websiteFilterMode: WebsiteFilterMode = WebsiteFilterMode.BLOCKLIST,
    val selectedBlocklistIds: List<Long> = emptyList(),
    val allowedApps: List<String> = emptyList(),
    val allowedWebsites: List<String> = emptyList(),
    val blockedWebsites: List<String> = emptyList(),
    val breakSettings: SessionBreakSettings = SessionBreakSettings(),
    val availableBlocklists: List<Blocklist> = emptyList(),
    val installedApps: List<AppInfo> = emptyList(),
    val isSaving: Boolean = false
)

@HiltViewModel
class CreateSessionViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val sessionRepository: SessionRepository,
    private val blocklistRepository: BlocklistRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateSessionUiState())
    val uiState: StateFlow<CreateSessionUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            blocklistRepository.getAllBlocklists().collect { lists ->
                _uiState.update { it.copy(availableBlocklists = lists) }
            }
        }
        loadInstalledApps()
    }

    private fun loadInstalledApps() {
        viewModelScope.launch {
            val pm = context.packageManager
            val intent = android.content.Intent(android.content.Intent.ACTION_MAIN).apply {
                addCategory(android.content.Intent.CATEGORY_LAUNCHER)
            }
            val apps = pm.queryIntentActivities(intent, PackageManager.MATCH_ALL)
                .map { ri ->
                    AppInfo(
                        packageName = ri.activityInfo.packageName,
                        appName = ri.loadLabel(pm).toString(),
                        isSystemApp = (ri.activityInfo.applicationInfo.flags and
                                android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0,
                        isBrowser = KnownBrowsers.ALL_KNOWN_BROWSERS.contains(ri.activityInfo.packageName)
                    )
                }
                .sortedBy { it.appName }
                .distinctBy { it.packageName }
            _uiState.update { it.copy(installedApps = apps) }
        }
    }

    fun setName(name: String) = _uiState.update { it.copy(name = name) }
    fun setDuration(hours: Int, minutes: Int) = _uiState.update { it.copy(durationHours = hours, durationMinutes = minutes) }
    fun toggleLocked() = _uiState.update { it.copy(isLocked = !it.isLocked) }
    fun toggleStrictMode() = _uiState.update { it.copy(isStrictMode = !it.isStrictMode) }
    fun toggleAdultContent() = _uiState.update { it.copy(blockAdultContent = !it.blockAdultContent) }
    fun setWebsiteFilterMode(mode: WebsiteFilterMode) = _uiState.update { it.copy(websiteFilterMode = mode) }

    fun toggleBlocklist(id: Long) = _uiState.update { state ->
        val current = state.selectedBlocklistIds.toMutableList()
        if (current.contains(id)) current.remove(id) else current.add(id)
        state.copy(selectedBlocklistIds = current)
    }

    fun toggleAllowedApp(packageName: String) = _uiState.update { state ->
        val current = state.allowedApps.toMutableList()
        if (current.contains(packageName)) current.remove(packageName) else current.add(packageName)
        state.copy(allowedApps = current)
    }

    fun addAllowedWebsite(domain: String) {
        if (domain.isBlank()) return
        val cleanDomain = com.rushgames.barrier.filter.WebsiteDomainMatcher.normalizeDomain(domain) ?: return
        _uiState.update { state ->
            val current = state.allowedWebsites.toMutableList()
            if (!current.contains(cleanDomain)) current.add(cleanDomain)
            state.copy(allowedWebsites = current)
        }
    }

    fun removeAllowedWebsite(domain: String) = _uiState.update { state ->
        state.copy(allowedWebsites = state.allowedWebsites.filter { it != domain })
    }

    fun setBreakSettings(settings: SessionBreakSettings) = _uiState.update { it.copy(breakSettings = settings) }

    fun createSession(onSuccess: () -> Unit) = viewModelScope.launch {
        _uiState.update { it.copy(isSaving = true) }
        val s = _uiState.value
        val durationMin = s.durationHours * 60 + s.durationMinutes
        val now = System.currentTimeMillis()
        val durationMs = maxOf(1, durationMin) * 60_000L
        val session = Session(
            name = s.name,
            durationMinutes = maxOf(1, durationMin),
            startTime = now,
            endTime = now + durationMs,
            status = SessionStatus.ACTIVE,
            isLocked = s.isLocked,
            isStrictMode = s.isStrictMode,
            websiteFilterMode = s.websiteFilterMode,
            blocklistIds = s.selectedBlocklistIds.map { it.toString() },
            allowedAppPackages = s.allowedApps,
            allowedWebsites = s.allowedWebsites,
            blockedWebsites = s.blockedWebsites,
            breakSettings = s.breakSettings,
            blockAdultContent = s.blockAdultContent
        )
        val sessionId = sessionRepository.createSession(session)

        // Start Foreground Service & Expiration timer
        runCatching {
            val serviceIntent = android.content.Intent(context, com.rushgames.barrier.service.BlockingService::class.java).apply {
                action = com.rushgames.barrier.service.BlockingService.ACTION_START
            }
            context.startForegroundService(serviceIntent)
            com.rushgames.barrier.worker.SessionExpirationWorker.scheduleExpiration(context, sessionId, durationMs)
        }

        _uiState.update { it.copy(isSaving = false) }
        onSuccess()
    }
}
