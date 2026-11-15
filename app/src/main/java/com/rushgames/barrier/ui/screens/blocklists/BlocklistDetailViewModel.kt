package com.rushgames.barrier.ui.screens.blocklists

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rushgames.barrier.data.model.AppInfo
import com.rushgames.barrier.data.model.BlockedApp
import com.rushgames.barrier.data.model.BlockedWebsite
import com.rushgames.barrier.data.model.Blocklist
import com.rushgames.barrier.data.repository.AppRepository
import com.rushgames.barrier.data.repository.BlocklistRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BlocklistDetailUiState(
    val blocklist: Blocklist? = null,
    val blockedApps: List<BlockedApp> = emptyList(),
    val blockedWebsites: List<BlockedWebsite> = emptyList(),
    val allInstalledApps: List<AppInfo> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class BlocklistDetailViewModel @Inject constructor(
    private val blocklistRepository: BlocklistRepository,
    private val appRepository: AppRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(BlocklistDetailUiState())
    val uiState: StateFlow<BlocklistDetailUiState> = _uiState.asStateFlow()

    private var currentBlocklistId: Long = -1L

    fun loadBlocklist(blocklistId: Long) {
        currentBlocklistId = blocklistId
        viewModelScope.launch {
            val allApps = appRepository.getInstalledApps()
            _uiState.update { it.copy(allInstalledApps = allApps) }

            combine(
                blocklistRepository.getAllBlocklists(),
                blocklistRepository.getAppsForBlocklist(blocklistId),
                blocklistRepository.getWebsitesForBlocklist(blocklistId)
            ) { lists, apps, sites ->
                val target = lists.find { it.id == blocklistId }
                BlocklistDetailUiState(
                    blocklist = target,
                    blockedApps = apps,
                    blockedWebsites = sites,
                    allInstalledApps = allApps,
                    isLoading = false
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    fun addAppToBlocklist(app: AppInfo) = viewModelScope.launch {
        if (currentBlocklistId != -1L) {
            blocklistRepository.addAppToBlocklist(app.packageName, app.appName, currentBlocklistId)
        }
    }

    fun removeAppFromBlocklist(app: BlockedApp) = viewModelScope.launch {
        blocklistRepository.removeAppFromBlocklist(app)
    }

    fun addWebsiteToBlocklist(domain: String) = viewModelScope.launch {
        if (currentBlocklistId != -1L && domain.isNotBlank()) {
            blocklistRepository.addWebsiteToBlocklist(domain.trim(), currentBlocklistId)
        }
    }

    fun removeWebsiteFromBlocklist(website: BlockedWebsite) = viewModelScope.launch {
        blocklistRepository.removeWebsiteFromBlocklist(website)
    }
}
