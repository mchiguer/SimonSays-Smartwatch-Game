package com.epfl.esl.simongame.solo_mode

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.epfl.esl.simongame.game.GameConfig
import com.epfl.esl.simongame.game.GameResult
import com.epfl.esl.simongame.game.Move
import com.epfl.esl.simongame.wear.GameState
import com.epfl.esl.simongame.wear.MotionWindow
import com.epfl.esl.simongame.wear.MotionWindowRepository
import com.epfl.esl.simongame.wear.PhoneWearClient
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class SoloGamePhase {
    SHOWING_MOVE,
    COUNTDOWN,
    EVALUATING,
    RESULT,
    RETRY,
    GAME_OVER
}

data class SoloGameUiState(
    val phase: SoloGamePhase = SoloGamePhase.SHOWING_MOVE,
    val currentMove: String = "",
    val expectedMove: Move = Move.IDLE,
    val isSimonSays: Boolean = true,
    val round: Int = 0,
    val score: Float = 0f,
    val maxScore: Float = 0f,
    val mistakes: Int = 0,
    val timeLeftSec: Int = 5,
    val lastResultCorrect: Boolean? = null,
    val fellForTrap: Boolean = false,
    val stayedIdle: Boolean = false,
    val isGameOver: Boolean = false,
    val pendingGameOver: Boolean = false,
    val gameResult: GameResult? = null,
    val predictedMove: Move? = null,
    val predictionConfidence: Float = 0f,
    val allScores: Map<Move, Float> = emptyMap(),
    val accelSampleCount: Int = 0,
    val gyroSampleCount: Int = 0,
    val streak: Int = 0,
    val multiplier: Int = 1,
    val pointsEarned: Float = 0f,
    val speedBonus: Float = 0f,
    val showMultiplierPopup: Boolean = false,
    val newMultiplierReached: Boolean = false,
    val bestStreak: Int = 0,
    val triggerShake: Boolean = false,
    val elapsedTimeMs: Long = 0,
    val retryCount: Int = 0,
)

class SoloGameViewModel(
    private val predictor: MotionToMovePredictor,
    private val motionWindowRepository: MotionWindowRepository,
    private val phoneWearClient: PhoneWearClient,
    private val appContext: Context
) : ViewModel() {

    companion object {
        const val MAX_RETRIES = 2
    }

    private val config = GameConfig()
    private val playableMoves = config.moveTexts.keys.toList()
    private val maxMistakes = config.maxMistakes

    private val _uiState = MutableStateFlow(SoloGameUiState())
    val uiState: StateFlow<SoloGameUiState> = _uiState

    private var countdownJob: Job? = null
    private var motionCollectionJob: Job? = null
    private var watchSyncJob: Job? = null
    private var lastMotionWindow: MotionWindow? = null

    private var recordingStartTimeMs: Long = 0
    private val maxRecordingTimeMs: Long = 5000

    private fun calculateMultiplier(streak: Int): Int {
        return when {
            streak < 2 -> 1
            streak < 4 -> 2
            streak < 6 -> 3
            streak < 8 -> 4
            streak < 10 -> 5
            else -> 6
        }
    }

    init {
        startNewGame()
        collectMotionData()
        syncWithWatch()
    }

    private fun syncWithWatch() {
        watchSyncJob = viewModelScope.launch {
            uiState.collect { state ->
                sendStateToWatch(state)
            }
        }
    }

    private suspend fun sendStateToWatch(state: SoloGameUiState) {
        val watchState = GameState(
            phase = state.phase.name,
            moveName = state.currentMove,
            round = state.round,
            score = state.score.toInt(),
            timeLeftSec = state.timeLeftSec,
            isRecording = state.phase == SoloGamePhase.COUNTDOWN,
            lastResultCorrect = state.lastResultCorrect,
            retryCount = state.retryCount
        )
        try {
            phoneWearClient.sendGameState(watchState)
        } catch (e: Exception) {
        }
    }

    private fun collectMotionData() {
        motionCollectionJob = viewModelScope.launch {
            motionWindowRepository.windows.collect { window ->
                lastMotionWindow = window
            }
        }
    }

    fun startNewGame() {
        countdownJob?.cancel()
        _uiState.value = SoloGameUiState()
        generateNextMove()
    }

    private fun generateNextMove() {
        val move = playableMoves.random()
        val baseMoveText = config.moveTexts.getValue(move)

        val isSimonSays = Math.random() < 0.7
        val displayText = if (isSimonSays) "SIMON SAYS\n$baseMoveText" else baseMoveText

        _uiState.update {
            it.copy(
                phase = SoloGamePhase.SHOWING_MOVE,
                currentMove = displayText,
                expectedMove = move,
                isSimonSays = isSimonSays,
                round = it.round + 1,
                timeLeftSec = 5,
                lastResultCorrect = null,
                fellForTrap = false,
                stayedIdle = false,
                pendingGameOver = false,
                predictedMove = null,
                predictionConfidence = 0f,
                retryCount = 0,
                showMultiplierPopup = false,
                newMultiplierReached = false,
                pointsEarned = 0f,
                triggerShake = false
            )
        }
    }

    fun onStartPressed() {
        val currentPhase = _uiState.value.phase
        if (currentPhase != SoloGamePhase.SHOWING_MOVE && currentPhase != SoloGamePhase.RETRY) return

        lastMotionWindow = null
        recordingStartTimeMs = System.currentTimeMillis()

        _uiState.update { it.copy(phase = SoloGamePhase.COUNTDOWN, timeLeftSec = 5, elapsedTimeMs = 0) }

        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            phoneWearClient.sendStartRecording(_uiState.value.currentMove)

            for (t in 5 downTo 1) {
                _uiState.update {
                    it.copy(
                        timeLeftSec = t,
                        elapsedTimeMs = System.currentTimeMillis() - recordingStartTimeMs
                    )
                }
                delay(1000L)
            }
            _uiState.update { it.copy(timeLeftSec = 0) }

            stopRecordingAndEvaluate()
        }
    }

    fun onDonePressed() {
        val currentPhase = _uiState.value.phase
        if (currentPhase != SoloGamePhase.COUNTDOWN) return

        countdownJob?.cancel()
        viewModelScope.launch {
            stopRecordingAndEvaluate()
        }
    }

    private suspend fun stopRecordingAndEvaluate() {
        val elapsedMs = minOf(System.currentTimeMillis() - recordingStartTimeMs, maxRecordingTimeMs)
        _uiState.update { it.copy(elapsedTimeMs = elapsedMs) }

        phoneWearClient.sendStopRecording()

        delay(2000L)
        evaluateWithML()
    }

    private fun evaluateWithML() {
        val motionWindow = lastMotionWindow
        val currentState = _uiState.value

        _uiState.update { it.copy(phase = SoloGamePhase.EVALUATING) }

        if (motionWindow == null) {
            _uiState.update { it.copy(accelSampleCount = 0, gyroSampleCount = 0) }
            handleNoData(currentState)
            return
        }

        val accelCount = motionWindow.accel.size
        val gyroCount = motionWindow.gyro.size
        _uiState.update { it.copy(accelSampleCount = accelCount, gyroSampleCount = gyroCount) }

        val prediction = predictor.predictWithConfidence(motionWindow)

        evaluatePrediction(prediction, _uiState.value)
    }

    private fun handleNoData(currentState: SoloGameUiState) {
        _uiState.update {
            it.copy(
                phase = SoloGamePhase.RESULT,
                lastResultCorrect = null,
                pointsEarned = 0f,
                triggerShake = false
            )
        }
    }

    private fun evaluatePrediction(prediction: PredictionResult, currentState: SoloGameUiState) {
        val didExpectedMove = prediction.move == currentState.expectedMove

        if (currentState.isSimonSays) {
            if (didExpectedMove) {
                handleCorrectMove(currentState, prediction)
            } else {
                handleWrongMove(currentState, prediction.move, prediction.confidence, prediction.allScores)
            }
        } else {
            if (didExpectedMove) {
                handleFellForTrap(currentState, prediction)
            } else {
                handleAvoidedTrap(currentState, prediction)
            }
        }
    }

    private fun handleCorrectMove(currentState: SoloGameUiState, prediction: PredictionResult) {
        val newStreak = currentState.streak + 1
        val oldMultiplier = currentState.multiplier
        val newMultiplier = calculateMultiplier(newStreak)
        val reachedNewMultiplier = newMultiplier > oldMultiplier

        val speedBonus = maxOf(0f, (maxRecordingTimeMs - currentState.elapsedTimeMs).toFloat() / maxRecordingTimeMs)

        val basePoints = 1f
        val pointsThisTurn = basePoints * newMultiplier * (1f + speedBonus)

        val newScore = currentState.score + pointsThisTurn
        val newMaxScore = maxOf(currentState.maxScore, newScore)
        val newBestStreak = maxOf(currentState.bestStreak, newStreak)

        _uiState.update {
            it.copy(
                phase = SoloGamePhase.RESULT,
                score = newScore,
                maxScore = newMaxScore,
                lastResultCorrect = true,
                predictedMove = prediction.move,
                predictionConfidence = prediction.confidence,
                allScores = prediction.allScores,
                streak = newStreak,
                multiplier = newMultiplier,
                pointsEarned = pointsThisTurn,
                speedBonus = speedBonus,
                showMultiplierPopup = newMultiplier > 1,
                newMultiplierReached = reachedNewMultiplier,
                bestStreak = newBestStreak,
                triggerShake = false
            )
        }
    }

    private fun handleWrongMove(currentState: SoloGameUiState, predictedMove: Move?, confidence: Float, allScores: Map<Move, Float>) {
        val newMistakes = currentState.mistakes + 1
        val isGameOver = newMistakes >= maxMistakes
        val playerStayedIdle = predictedMove == Move.IDLE

        _uiState.update {
            it.copy(
                phase = SoloGamePhase.RESULT,
                mistakes = newMistakes,
                lastResultCorrect = false,
                predictedMove = predictedMove,
                predictionConfidence = confidence,
                allScores = allScores,
                streak = 0,
                multiplier = 1,
                pointsEarned = 0f,
                speedBonus = 0f,
                showMultiplierPopup = false,
                newMultiplierReached = false,
                stayedIdle = playerStayedIdle,
                isGameOver = isGameOver,
                pendingGameOver = isGameOver,
                gameResult = if (isGameOver) GameResult(it.score.toInt(), it.maxScore.toInt(), it.round, newMistakes) else null,
                triggerShake = true
            )
        }
    }

    private fun handleFellForTrap(currentState: SoloGameUiState, prediction: PredictionResult) {
        val newScore = maxOf(0f, currentState.score - 1f)
        val actualPenalty = newScore - currentState.score

        _uiState.update {
            it.copy(
                phase = SoloGamePhase.RESULT,
                score = newScore,
                lastResultCorrect = false,
                fellForTrap = true,
                predictedMove = prediction.move,
                predictionConfidence = prediction.confidence,
                allScores = prediction.allScores,
                streak = 0,
                multiplier = 1,
                pointsEarned = actualPenalty,
                speedBonus = 0f,
                showMultiplierPopup = false,
                newMultiplierReached = false,
                triggerShake = true
            )
        }
    }

    private fun handleAvoidedTrap(currentState: SoloGameUiState, prediction: PredictionResult) {
        val newStreak = currentState.streak + 1
        val newMultiplier = calculateMultiplier(newStreak)
        val reachedNewMultiplier = newMultiplier > currentState.multiplier
        val playerStayedIdle = prediction.move == Move.IDLE

        val speedBonus = maxOf(0f, (maxRecordingTimeMs - currentState.elapsedTimeMs).toFloat() / maxRecordingTimeMs)

        val basePoints = 1f
        val pointsThisTurn = basePoints * newMultiplier * (1f + speedBonus)

        val newScore = currentState.score + pointsThisTurn
        val newMaxScore = maxOf(currentState.maxScore, newScore)
        val newBestStreak = maxOf(currentState.bestStreak, newStreak)

        _uiState.update {
            it.copy(
                phase = SoloGamePhase.RESULT,
                score = newScore,
                maxScore = newMaxScore,
                lastResultCorrect = true,
                stayedIdle = playerStayedIdle,
                predictedMove = prediction.move,
                predictionConfidence = prediction.confidence,
                allScores = prediction.allScores,
                streak = newStreak,
                multiplier = newMultiplier,
                pointsEarned = pointsThisTurn,
                speedBonus = speedBonus,
                showMultiplierPopup = newMultiplier > 1,
                newMultiplierReached = reachedNewMultiplier,
                bestStreak = newBestStreak,
                triggerShake = false
            )
        }
    }

    fun onContinuePressed() {
        if (_uiState.value.phase != SoloGamePhase.RESULT) return
        generateNextMove()
    }

    fun onTransitionToGameOver() {
        if (!_uiState.value.pendingGameOver) return
        _uiState.update {
            it.copy(
                phase = SoloGamePhase.GAME_OVER,
                pendingGameOver = false
            )
        }
    }

    fun onRestartPressed() {
        startNewGame()
    }

    fun onShakeComplete() {
        _uiState.update { it.copy(triggerShake = false) }
    }

    override fun onCleared() {
        super.onCleared()
        countdownJob?.cancel()
        motionCollectionJob?.cancel()
        watchSyncJob?.cancel()
    }
}