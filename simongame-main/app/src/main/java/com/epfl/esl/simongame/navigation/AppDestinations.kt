package com.epfl.esl.simongame.navigation

object AppDestinations {
    const val HOME = "home"
    const val SOLO_GAME = "solo_game"
    const val CHALLENGE_GAME = "challenge_game"
    const val SOLO_RESULT = "solo_result"
    const val PROFILE = "profile"
    const val MULTI_GAME = "multi_game"
    const val REGISTER = "register"
    const val FRIEND_SEARCH = "friend_search"
    const val FRIENDS_LIST = "friends_list"
    const val SETTINGS = "settings"

    fun soloResultRoute(score: Int, maxScore: Int, rounds: Int, mistakes: Int) =
        "$SOLO_RESULT/$score/$maxScore/$rounds/$mistakes/0/none/0"

    fun challengeResultRoute(
        score: Int,
        maxScore: Int,
        rounds: Int,
        mistakes: Int,
        challengeId: String,
        challengerName: String,
        scoreToBeat: Int
    ) = "$SOLO_RESULT/$score/$maxScore/$rounds/$mistakes/1/$challengerName/$scoreToBeat"

    fun challengeGameRoute(challengeId: String, challengerName: String, scoreToBeat: Int) =
        "$CHALLENGE_GAME/$challengeId/$challengerName/$scoreToBeat"
}
