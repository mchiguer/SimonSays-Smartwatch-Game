package com.epfl.esl.simongame.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.epfl.esl.simongame.data.UserRepository
import com.epfl.esl.simongame.game.GameResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeUiState(
    val bestSoloScore: Int = 0,
    val lastSoloRounds: Int? = null,
    val lastSoloMistakes: Int? = null
)

class HomeViewModel(
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState

    init {
        viewModelScope.launch {
            userRepository.currentUser.collect { user ->
                if (user != null) {
                    _uiState.update {
                        it.copy(bestSoloScore = user.bestScore)
                    }
                } else {
                    _uiState.value = HomeUiState()
                }
            }
        }
        viewModelScope.launch {
            userRepository.getLastGameStats().collect { (rounds, mistakes) ->
                _uiState.update {
                    it.copy(
                        lastSoloRounds = rounds,
                        lastSoloMistakes = mistakes
                    )
                }
            }
        }
    }

    fun onSoloGameFinished(result: GameResult) {
        _uiState.update { current ->
            val newBest = maxOf(current.bestSoloScore, result.finalScore)
            current.copy(
                bestSoloScore = newBest,
                lastSoloRounds = result.totalRounds,
                lastSoloMistakes = result.mistakes
            )
        }

        val trophiesEarned = result.finalScore / 10
        viewModelScope.launch {
            userRepository.updateGameStats(
                result.finalScore,
                trophiesEarned,
                result.totalRounds,
                result.mistakes
            )
        }
    }
}