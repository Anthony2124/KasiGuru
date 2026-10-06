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
    val selectedFilter: String = "All", // "All", "Streak", "WordOfDay", "News"
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
        val filteredList = when (filter) {
            "All" -> notifications
            // Pushes from the team arrive with whatever category was sent; all of them are news.
            NEWS_FILTER -> notifications.filter { n -> APP_CATEGORIES.none { n.category.equals(it, ignoreCase = true) } }
            else -> notifications.filter { it.category.equals(filter, ignoreCase = true) }
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

internal const val NEWS_FILTER = "News"

/** Categories the app writes itself; anything else came from the team. */
private val APP_CATEGORIES = listOf("Streak", "WordOfDay")

/**
 * When a message arrived, relative to [now]: "Just now", "5 min ago", "3 h ago", "Yesterday",
 * a weekday within the week, then a date.
 *
 * New messages store epoch milliseconds. Older ones stored display text such as "Just now",
 * which is shown as it was written.
 */
internal fun inboxTimestamp(raw: String, now: Long = System.currentTimeMillis()): String {
    val at = raw.toLongOrNull() ?: return raw
    val zone = java.time.ZoneId.systemDefault()
    val minutes = (now - at).coerceAtLeast(0) / 60_000
    val then = java.time.Instant.ofEpochMilli(at).atZone(zone).toLocalDate()
    val today = java.time.Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
    return when {
        minutes < 1 -> "Just now"
        minutes < 60 -> "$minutes min ago"
        then == today -> "${minutes / 60} h ago"
        then == today.minusDays(1) -> "Yesterday"
        then.isAfter(today.minusDays(7)) ->
            then.dayOfWeek.getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.ENGLISH)
        else -> then.format(java.time.format.DateTimeFormatter.ofPattern("d MMM yyyy", java.util.Locale.ENGLISH))
    }
}
