package com.epfl.esl.simongame.data

import kotlinx.coroutines.flow.Flow

interface UserRepository {
    val currentUser: Flow<UserProfile?>
    val directoryUsers: Flow<List<UserProfile>>
    val pendingChallenges: Flow<List<Challenge>>
    val sentChallenges: Flow<List<Challenge>>
    val completedChallenges: Flow<List<Challenge>>

    suspend fun register(username: String, email: String)
    suspend fun logout()
    suspend fun clearSession()
    suspend fun cleanupDuplicateChallenges()
    suspend fun addFriend(friendId: String)
    suspend fun removeFriend(friendId: String)
    fun getUsersWhoAddedMe(): Flow<List<UserProfile>>
    suspend fun updateGameStats(score: Int, trophiesEarned: Int, rounds: Int, mistakes: Int)
    fun getLastGameStats(): Flow<Pair<Int?, Int?>>

    suspend fun sendChallenge(friendId: String, friendName: String, myScore: Int)
    suspend fun acceptChallenge(challengeId: String)
    suspend fun declineChallenge(challengeId: String)
    suspend fun completeChallenge(challengeId: String, myScore: Int)
    suspend fun addSampleChallengesIfNeeded()
    suspend fun getChallengeById(challengeId: String): Challenge?

    suspend fun uploadProfilePicture(imageBytes: ByteArray): Result<String>
    suspend fun removeProfilePicture(): Result<Unit>

    suspend fun dismissSuggestion(userId: String)
    fun getDismissedSuggestions(): Flow<Set<String>>

    suspend fun deleteAccount(): Result<Unit>
}
