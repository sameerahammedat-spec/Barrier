package com.rushgames.barrier.ui.screens.sessions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rushgames.barrier.data.model.*
import com.rushgames.barrier.data.repository.SessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SessionsUiState(
    val activeSessions: List<Session> = emptyList(),
    val upcomingSessions: List<Session> = emptyList(),
    val history: List<Session> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class SessionsViewModel @Inject constructor(
    private val sessionRepository: SessionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SessionsUiState())
    val uiState: StateFlow<SessionsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                sessionRepository.getActiveSession().map { listOfNotNull(it) },
                sessionRepository.getUpcomingSessions().map { list ->
                    list.filter { it.status == SessionStatus.IDLE }
                },
                sessionRepository.getSessionHistory()
            ) { active, upcoming, history ->
                SessionsUiState(
                    activeSessions = active,
                    upcomingSessions = upcoming,
                    history = history,
                    isLoading = false
                )
            }.collect { _uiState.value = it }
        }
    }

    fun cancelSession(session: Session) = viewModelScope.launch {
        if (!session.isLocked) sessionRepository.cancelSession(session.id)
    }

    fun deleteSession(session: Session) = viewModelScope.launch {
        if (session.status != SessionStatus.ACTIVE) sessionRepository.deleteSession(session)
    }
}
