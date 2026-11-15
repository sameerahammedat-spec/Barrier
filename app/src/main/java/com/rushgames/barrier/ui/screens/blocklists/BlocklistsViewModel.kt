package com.rushgames.barrier.ui.screens.blocklists

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rushgames.barrier.data.model.*
import com.rushgames.barrier.data.repository.BlocklistRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BlocklistsUiState(
    val blocklists: List<Blocklist> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class BlocklistsViewModel @Inject constructor(
    private val repository: BlocklistRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(BlocklistsUiState())
    val uiState: StateFlow<BlocklistsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.getAllBlocklists().collect { lists ->
                _uiState.update { it.copy(blocklists = lists, isLoading = false) }
            }
        }
    }

    fun deleteBlocklist(blocklist: Blocklist) = viewModelScope.launch {
        repository.deleteBlocklist(blocklist)
    }

    fun toggleEnabled(blocklist: Blocklist) = viewModelScope.launch {
        repository.updateBlocklist(blocklist.copy(isEnabled = !blocklist.isEnabled))
    }

    fun duplicateBlocklist(blocklist: Blocklist) = viewModelScope.launch {
        repository.createBlocklist("${blocklist.name} (copy)", blocklist.category)
    }
}
