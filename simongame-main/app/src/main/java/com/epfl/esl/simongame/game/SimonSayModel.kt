package com.epfl.esl.simongame.game

enum class Move {
    JUMP,
    RAISE_ARMS,
    SPIN_AROUND,
    CLAP,
    SAY_HI,
    RUN_IN_PLACE,
    IDLE
}

data class Instruction(
    val text: String,
    val isSimonSays: Boolean,
    val expectedMove: Move
)

data class GameConfig(
    val maxMistakes: Int = 3,
    val moveTexts: Map<Move, String> = mapOf(
        Move.JUMP to "JUMP",
        Move.RAISE_ARMS to "RAISE ARMS",
        Move.SPIN_AROUND to "SPIN AROUND",
        Move.SAY_HI to "SAY HI",
        Move.RUN_IN_PLACE to "RUN IN PLACE",
    )
)

data class GameState(
    val score: Int = 0,
    val mistakes: Int = 0,
    val currentInstruction: Instruction? = null,
    val isGameOver: Boolean = false,
    val round: Int = 0
)

data class GameResult(
    val finalScore: Int,
    val maxScore: Int,
    val totalRounds: Int,
    val mistakes: Int
)