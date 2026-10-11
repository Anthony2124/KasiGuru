package com.kasiguru.ui.navigation

import androidx.lifecycle.ViewModel
import com.kasiguru.data.repository.CommunityUpdatesRepository
import com.kasiguru.domain.contribute.CommunityEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

/**
 * The in-app pop-up for news about the learner's words and the dictionary, one at a time. Lives at
 * the navigation shell beside the celebrations, which decides when it is calm enough to show.
 */
@HiltViewModel
class CommunityUpdateViewModel @Inject constructor(
    private val repository: CommunityUpdatesRepository
) : ViewModel() {

    val pending: StateFlow<List<CommunityEvent>> = repository.pending

    fun dismiss(event: CommunityEvent) = repository.dismiss(event)
}
