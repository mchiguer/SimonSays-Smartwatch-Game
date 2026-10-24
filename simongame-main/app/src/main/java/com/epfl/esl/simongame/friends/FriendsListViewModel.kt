package com.epfl.esl.simongame.friends

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.epfl.esl.simongame.data.Challenge
import com.epfl.esl.simongame.data.UserProfile
import com.epfl.esl.simongame.data.UserRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class FriendsListUiState(
    val isLoading: Boolean = true,
    val friends: List<UserProfile> = emptyList(),
    val pendingChallenges: List<Challenge> = emptyList(),
    val sentChallenges: List<Challenge> = emptyList(),
    val myBestScore: Int = 0
)

class FriendsListViewModel(
    private val repo: UserRepository
) : ViewModel() {

    val uiState: StateFlow<FriendsListUiState> =
        combine(
            repo.currentUser,
            repo.directoryUsers,
            repo.pendingChallenges,
            repo.sentChallenges
        ) { me, directory, pending, sent ->
            val friendIds = me?.friends?.toSet() ?: emptySet()
            val friends = directory.filter { it.id in friendIds }
            val uniquePending = pending.distinctBy { it.id }
            val uniqueSent = sent.distinctBy { it.id }
            FriendsListUiState(
                isLoading = false,
                friends = friends,
                pendingChallenges = uniquePending,
                sentChallenges = uniqueSent,
                myBestScore = me?.bestScore ?: 0
            )
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            FriendsListUiState()
        )

    fun sendChallenge(friendId: String, friendName: String, myScore: Int) {
        viewModelScope.launch {
            repo.sendChallenge(friendId, friendName, myScore)
        }
    }

    fun acceptChallenge(challengeId: String) {
        viewModelScope.launch {
            repo.acceptChallenge(challengeId)
        }
    }

    fun declineChallenge(challengeId: String) {
        viewModelScope.launch {
            repo.declineChallenge(challengeId)
        }
    }

    fun removeFriend(friendId: String) {
        viewModelScope.launch {
            repo.removeFriend(friendId)
        }
    }
}