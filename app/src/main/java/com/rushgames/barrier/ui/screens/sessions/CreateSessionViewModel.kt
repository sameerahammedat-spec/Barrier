package com.rushgames.barrier.ui.screens.sessions

import android.content.Context
import android.content.pm.PackageManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rushgames.barrier.data.datastore.BarrierPreferences
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
    val appFilterMode: AppFilterMode = AppFilterMode.NONE,
    val selectedBlocklistIds: List<Long> = emptyList(),
    val allowedApps: List<String> = emptyList(),
    val blockedApps: List<String> = emptyList(),
    val allowedWebsites: List<String> = emptyList(),
    val blockedWebsites: List<String> = emptyList(),
    val breakSettings: SessionBreakSettings = SessionBreakSettings(),
    val availableBlocklists: List<Blocklist> = emptyList(),
    val installedApps: List<AppInfo> = emptyList(),
    val isSaving: Boolean = false,
    val saveError: String? = null
)

@HiltViewModel
class CreateSessionViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val sessionRepository: SessionRepository,
    private val blocklistRepository: BlocklistRepository,
    private val preferences: BarrierPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateSessionUiState())
    val uiState: StateFlow<CreateSessionUiState> = _uiState.asStateFlow()
    private var strictModeChangedByUser = false

    init {
        viewModelScope.launch {
            val strictByDefault = preferences.strictModeGlobal.first()
            if (!strictModeChangedByUser) {
                _uiState.update { it.copy(isStrictMode = strictByDefault) }
            }
        }
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
            val protectedPackages = setOf(
                context.packageName,
                "android",
                "com.android.systemui",
                "com.android.settings",
                "com.android.permissioncontroller",
                "com.google.android.permissioncontroller",
                "com.android.packageinstaller",
                "com.google.android.packageinstaller",
                "com.android.launcher",
                "com.android.launcher3",
                "com.google.android.apps.nexuslauncher"
            )
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
                .filterNot { it.activityInfo.packageName in protectedPackages }
                .sortedBy { it.appName }
                .distinctBy { it.packageName }
            _uiState.update { it.copy(installedApps = apps) }
        }
    }

    fun setName(name: String) = _uiState.update { it.copy(name = name) }
    fun setDuration(hours: Int, minutes: Int) = _uiState.update { it.copy(durationHours = hours, durationMinutes = minutes) }
    fun toggleLocked() = _uiState.update { it.copy(isLocked = !it.isLocked) }
    fun toggleStrictMode() {
        strictModeChangedByUser = true
        _uiState.update { it.copy(isStrictMode = !it.isStrictMode) }
    }
    fun toggleAdultContent() = _uiState.update { it.copy(blockAdultContent = !it.blockAdultContent) }
    fun setWebsiteFilterMode(mode: WebsiteFilterMode) = _uiState.update { it.copy(websiteFilterMode = mode) }
    fun setAppFilterMode(mode: AppFilterMode) = _uiState.update { it.copy(appFilterMode = mode) }

    fun toggleBlocklist(id: Long) = _uiState.update { state ->
        val current = state.selectedBlocklistIds.toMutableList()
        if (current.contains(id)) current.remove(id) else current.add(id)
        state.copy(selectedBlocklistIds = current)
    }

    fun toggleBlockedApp(packageName: String) = _uiState.update { state ->
        val current = state.blockedApps.toMutableList()
        if (current.contains(packageName)) current.remove(packageName) else current.add(packageName)
        state.copy(blockedApps = current)
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
        _uiState.update { it.copy(isSaving = true, saveError = null) }
        val s = _uiState.value
        if (s.appFilterMode == AppFilterMode.BLOCK_SELECTED && s.blockedApps.isEmpty()) {
            _uiState.update {
                it.copy(isSaving = false, saveError = "Select at least one app to block, or turn app blocking off.")
            }
            return@launch
        }
        if (s.appFilterMode == AppFilterMode.ALLOWLIST_ONLY && s.allowedApps.isEmpty()) {
            _uiState.update {
                it.copy(isSaving = false, saveError = "Select at least one allowed app to use Allowlist Only mode.")
            }
            return@launch
        }

        val durationMin = SessionDurationPolicy.toMinutes(s.durationHours, s.durationMinutes)
        if (!SessionDurationPolicy.isValid(durationMin)) {
            _uiState.update {
                it.copy(
                    isSaving = false,
                    saveError = "Choose a session duration between 5 minutes and 7 days."
                )
            }
            return@launch
        }
        val now = System.currentTimeMillis()
        val durationMs = durationMin * 60_000L
        val session = Session(
            name = s.name,
            durationMinutes = durationMin,
            startTime = now,
            endTime = now + durationMs,
            status = SessionStatus.IDLE,
            isLocked = s.isLocked || s.isStrictMode,
            isStrictMode = s.isStrictMode,
            websiteFilterMode = s.websiteFilterMode,
            appFilterMode = s.appFilterMode,
            blocklistIds = s.selectedBlocklistIds.map { it.toString() },
            allowedAppPackages = s.allowedApps,
            blockedAppPackages = s.blockedApps,
            allowedWebsites = s.allowedWebsites,
            blockedWebsites = s.blockedWebsites,
            breakSettings = s.breakSettings,
            blockAdultContent = s.blockAdultContent
        )

        var createdSessionId: Long? = null
        var rollbackSucceeded = true
        try {
            val sessionId = sessionRepository.createActiveSession(session)
            if (sessionId == null) {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        saveError = "A focus session is already active. Wait for it to finish before starting another."
                    )
                }
                return@launch
            }
            createdSessionId = sessionId
            com.rushgames.barrier.worker.SessionExpirationWorker.scheduleExpiration(
                context,
                sessionId,
                durationMs
            )
            val serviceIntent = android.content.Intent(
                context,
                com.rushgames.barrier.service.BlockingService::class.java
            ).apply {
                action = com.rushgames.barrier.service.BlockingService.ACTION_START
            }
            context.startForegroundService(serviceIntent)
        } catch (cause: Exception) {
            if (cause is kotlinx.coroutines.CancellationException) throw cause

            createdSessionId?.let { sessionId ->
                try {
                    sessionRepository.failSessionStart(sessionId)
                    if (sessionRepository.getActiveSessionOnce() == null) {
                        context.stopService(
                            android.content.Intent(context, com.rushgames.barrier.service.BlockingService::class.java)
                        )
                    }
                } catch (rollbackCause: Exception) {
                    rollbackSucceeded = false
                    cause.addSuppressed(rollbackCause)
                }
            }

            val message = when {
                createdSessionId == null -> "Couldn't save the session. Check available storage and try again."
                rollbackSucceeded -> "Barrier couldn't start monitoring. The session was canceled; check permissions and try again."
                else -> "Barrier couldn't start monitoring or reset the saved session. Check its status before retrying."
            }
            _uiState.update { it.copy(isSaving = false, saveError = message) }
            return@launch
        }

        _uiState.update { it.copy(isSaving = false) }
        onSuccess()
    }
}
