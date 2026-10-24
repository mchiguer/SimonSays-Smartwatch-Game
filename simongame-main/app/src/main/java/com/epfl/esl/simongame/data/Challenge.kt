package com.epfl.esl.simongame.data

import org.json.JSONArray
import org.json.JSONObject


data class Challenge(
    val id: String,
    val challengerId: String,
    val challengerName: String,
    val challengerScore: Int,
    val challengedId: String,
    val challengedName: String,
    val challengedScore: Int? = null,
    val status: ChallengeStatus = ChallengeStatus.PENDING,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("challengerId", challengerId)
        put("challengerName", challengerName)
        put("challengerScore", challengerScore)
        put("challengedId", challengedId)
        put("challengedName", challengedName)
        put("challengedScore", challengedScore ?: JSONObject.NULL)
        put("status", status.name)
        put("createdAt", createdAt)
    }

    companion object {
        fun fromJson(json: JSONObject): Challenge = Challenge(
            id = json.getString("id"),
            challengerId = json.getString("challengerId"),
            challengerName = json.getString("challengerName"),
            challengerScore = json.getInt("challengerScore"),
            challengedId = json.getString("challengedId"),
            challengedName = json.getString("challengedName"),
            challengedScore = if (json.isNull("challengedScore")) null else json.getInt("challengedScore"),
            status = ChallengeStatus.valueOf(json.getString("status")),
            createdAt = json.getLong("createdAt")
        )

        fun listToJson(challenges: List<Challenge>): String {
            val array = JSONArray()
            challenges.forEach { array.put(it.toJson()) }
            return array.toString()
        }

        fun listFromJson(jsonString: String): List<Challenge> {
            if (jsonString.isEmpty()) return emptyList()
            return try {
                val array = JSONArray(jsonString)
                (0 until array.length()).mapNotNull { index ->
                    try {
                        fromJson(array.getJSONObject(index))
                    } catch (e: Exception) {
                        null // Skip invalid challenge entries
                    }
                }
            } catch (e: Exception) {
                emptyList() // Return empty list if JSON is corrupted
            }
        }
    }
}

enum class ChallengeStatus {
    PENDING,      // Waiting for challenged player to accept
    IN_PROGRESS,  // Challenged player accepted, playing
    COMPLETED,    // Both played, results available
    DECLINED      // Challenged player declined
}

data class ChallengeResult(
    val challengeId: String,
    val winnerId: String?,  // null if tie
    val winnerName: String?,
    val challengerScore: Int,
    val challengedScore: Int
) {
    val isTie: Boolean get() = winnerId == null
}