package com.rushgames.barrier.ui.screens.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rushgames.barrier.data.dao.StatsDao
import com.rushgames.barrier.data.model.Session
import com.rushgames.barrier.data.model.SessionStatus
import com.rushgames.barrier.data.repository.SessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

data class TodayUiState(
    val activeSession: Session? = null,
    val upcomingSessions: List<Session> = emptyList(),
    val todayFocusMinutes: Int = 0,
    val todayBlockedAttempts: Int = 0,
    val isLoading: Boolean = true,
    val hasUsageStatsPermission: Boolean = false,
    val hasAccessibilityPermission: Boolean = false
)

@HiltViewModel
class TodayViewModel @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val statsDao: StatsDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(TodayUiState())
    val uiState: StateFlow<TodayUiState> = _uiState.asStateFlow()

    init {
        observeData()
    }

    private fun observeData() {
        viewModelScope.launch {
            combine(
                sessionRepository.getActiveSession(),
                sessionRepository.getUpcomingSessions()
            ) { active, upcoming ->
                Pair(active, upcoming.filter { it.status == SessionStatus.IDLE }.take(3))
            }.collect { (active, upcoming) ->
                val dateKey = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                val stats = statsDao.getStatsByDate(dateKey)
                val blockedCount = statsDao.getBlockedAttemptCount(
                    System.currentTimeMillis() - 86_400_000L
                )
                _uiState.update { state ->
                    state.copy(
                        activeSession = active,
                        upcomingSessions = upcoming,
                        todayFocusMinutes = stats?.totalFocusMinutes ?: 0,
                        todayBlockedAttempts = blockedCount,
                        isLoading = false
                    )
                }
            }
        }
    }

    fun updatePermissionStatus(usageStats: Boolean, accessibility: Boolean) {
        _uiState.update { it.copy(
            hasUsageStatsPermission = usageStats,
            hasAccessibilityPermission = accessibility
        )}
    }
}
