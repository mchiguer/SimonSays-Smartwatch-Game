package com.epfl.esl.simongame.friends

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.epfl.esl.simongame.data.UserProfile
import com.epfl.esl.simongame.data.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class FriendSearchUiState(
    val query: String = "",
    val isLoading: Boolean = true,
    val currentUserId: String? = null,
    val currentFriendIds: Set<String> = emptySet(),
    val results: List<UserProfile> = emptyList(),
    val usersWhoAddedMe: List<UserProfile> = emptyList(),
    val dismissedSuggestions: Set<String> = emptySet(),
    val error: String? = null
)

class FriendSearchViewModel(
    private val repo: UserRepository
) : ViewModel() {

    private val queryFlow = MutableStateFlow("")

    val uiState: StateFlow<FriendSearchUiState> =
        combine(
            repo.currentUser,
            repo.directoryUsers,
            repo.getUsersWhoAddedMe(),
            repo.getDismissedSuggestions(),
            queryFlow
        ) { me, directory, whoAddedMe, dismissed, query ->
            val meId = me?.id
            val myFriendIds = me?.friends?.toSet() ?: emptySet()

            val q = query.trim().lowercase()

            val filteredWhoAddedMe = whoAddedMe.filter { it.id !in dismissed }

            val activeWhoAddedMeIds = filteredWhoAddedMe.map { it.id }.toSet()

            val filtered = directory
                .asSequence()
                .filter { u -> meId == null || u.id != meId }
                .filter { u -> u.id !in myFriendIds }
                .filter { u -> u.id !in activeWhoAddedMeIds }
                .filter { u -> u.id !in dismissed }
                .filter { u ->
                    if (q.isBlank()) true else {
                        u.username.lowercase().contains(q) ||
                                u.email.lowercase().contains(q) ||
                                u.id.lowercase().contains(q)
                    }
                }
                .toList()

            FriendSearchUiState(
                query = query,
                isLoading = false,
                currentUserId = meId,
                currentFriendIds = myFriendIds,
                results = filtered,
                usersWhoAddedMe = filteredWhoAddedMe,
                dismissedSuggestions = dismissed,
                error = null
            )
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            FriendSearchUiState()
        )

    fun onQueryChange(newValue: String) {
        queryFlow.value = newValue
    }

    fun addFriend(friendId: String) {
        viewModelScope.launch {
            repo.addFriend(friendId)
        }
    }

    fun dismissSuggestion(userId: String) {
        viewModelScope.launch {
            repo.dismissSuggestion(userId)
        }
    }
}