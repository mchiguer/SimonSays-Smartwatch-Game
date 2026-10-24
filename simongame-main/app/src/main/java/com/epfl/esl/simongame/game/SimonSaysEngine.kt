package com.epfl.esl.simongame.game

import kotlin.random.Random

class SimonSaysEngine(
    private val config: GameConfig = GameConfig(),
    seed: Long? = null
) {
    private val random = seed?.let { Random(it) } ?: Random.Default
    private val playableMoves = config.moveTexts.keys.toList()

    var state: GameState = GameState()
        private set

    fun startNewGame() {
        state = GameState()
        generateNextInstruction()
    }

    fun generateNextInstruction() {
        if (state.isGameOver) return

        val move = playableMoves.random(random)
        val isSimon = random.nextBoolean()
        val actionText = config.moveTexts.getValue(move)

        val text = if (isSimon) "Simon says $actionText" else actionText
        state = state.copy(
            currentInstruction = Instruction(text = text, isSimonSays = isSimon, expectedMove = move),
            round = state.round + 1
        )
    }

    fun answer(predictedMove: Move): Boolean {
        val inst = state.currentInstruction ?: error("No instruction")
        val correct = if (inst.isSimonSays) {
            predictedMove == inst.expectedMove
        } else {
            predictedMove == Move.IDLE
        }

        val newScore = if (correct) state.score + 1 else state.score
        val newMistakes = if (correct) state.mistakes else state.mistakes + 1
        val isGameOver = newMistakes >= config.maxMistakes

        state = state.copy(score = newScore, mistakes = newMistakes, isGameOver = isGameOver)
        return correct
    }

    fun toResult(): GameResult = GameResult(state.score, state.score, state.round, state.mistakes)  // maxScore = score car pas de pénalités ici
    fun endNow() { state = state.copy(isGameOver = true) }
}
