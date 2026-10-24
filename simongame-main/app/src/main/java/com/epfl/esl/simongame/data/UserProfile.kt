package com.epfl.esl.simongame.data

data class UserProfile(
    val id: String,
    val username: String,
    val email: String,
    val friends: List<String>,
    val bestScore: Int = 0,
    val totalTrophies: Int = 0,
    val profilePictureUrl: String? = null
)
