package com.kasiguru.ui.screens.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kasiguru.data.local.entity.NotificationEntity
import com.kasiguru.data.repository.NotificationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NotificationInboxUiState(
    val notifications: List<NotificationEntity> = emptyList(),
    val unreadCount: Int = 0,
    val selectedFilter: String = "All", // "All", "Streak", "WordOfDay", "Leaderboard", "Achievement"
    val isLoading: Boolean = false
)

@HiltViewModel
class NotificationInboxViewModel @Inject constructor(
    private val repository: NotificationRepository
) : ViewModel() {

    private val _selectedFilter = MutableStateFlow("All")

    val uiState: StateFlow<NotificationInboxUiState> = combine(
        repository.allNotifications,
        repository.unreadCount,
        _selectedFilter
    ) { notifications, unreadCount, filter ->
        val filteredList = if (filter == "All") {
            notifications
        } else {
            notifications.filter { it.category.equals(filter, ignoreCase = true) }
        }
        NotificationInboxUiState(
            notifications = filteredList,
            unreadCount = unreadCount,
            selectedFilter = filter,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = NotificationInboxUiState(isLoading = true)
    )

    fun setFilter(filter: String) {
        _selectedFilter.value = filter
    }

    fun markAsRead(id: Int) {
        viewModelScope.launch {
            repository.markAsRead(id)
        }
    }

    fun markAllAsRead() {
        viewModelScope.launch {
            repository.markAllAsRead()
        }
    }

    fun clearAll() {
        viewModelScope.launch {
            repository.clearAll()
        }
    }
}
