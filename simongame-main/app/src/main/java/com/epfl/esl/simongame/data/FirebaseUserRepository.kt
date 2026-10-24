package com.epfl.esl.simongame.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.tasks.await
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
class FirebaseUserRepository : UserRepository {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()

    private val usersCollection = firestore.collection("users")
    private val challengesCollection = firestore.collection("challenges")

    private val _lastRounds = MutableStateFlow<Int?>(null)
    private val _lastMistakes = MutableStateFlow<Int?>(null)

    private val _dismissedSuggestions = MutableStateFlow<Set<String>>(emptySet())

    override val currentUser: Flow<UserProfile?> = callbackFlow {
        val authListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            val firebaseUser = firebaseAuth.currentUser
            if (firebaseUser == null) {
                trySend(null)
            }
        }
        auth.addAuthStateListener(authListener)

        var userDocListener: ListenerRegistration? = null

        val authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            userDocListener?.remove()
            val firebaseUser = firebaseAuth.currentUser
            // Reset stats when user changes
            _lastRounds.value = null
            _lastMistakes.value = null
            if (firebaseUser != null) {
                userDocListener = usersCollection.document(firebaseUser.uid)
                    .addSnapshotListener { snapshot, _ ->
                        if (snapshot != null && snapshot.exists()) {
                            val profile = snapshot.toUserProfile()
                            val lastRounds = snapshot.getLong("lastRounds")?.toInt()
                            val lastMistakes = snapshot.getLong("lastMistakes")?.toInt()
                            _lastRounds.value = lastRounds
                            _lastMistakes.value = lastMistakes
                            @Suppress("UNCHECKED_CAST")
                            val dismissed = (snapshot.get("dismissedSuggestions") as? List<String>)?.toSet() ?: emptySet()
                            _dismissedSuggestions.value = dismissed
                            trySend(profile)
                        } else {
                            trySend(null)
                        }
                    }
            } else {
                trySend(null)
            }
        }

        auth.addAuthStateListener(authStateListener)

        awaitClose {
            auth.removeAuthStateListener(authListener)
            auth.removeAuthStateListener(authStateListener)
            userDocListener?.remove()
        }
    }

    override val directoryUsers: Flow<List<UserProfile>> = callbackFlow {
        val listener = usersCollection
            .addSnapshotListener { snapshot, error ->
                val users = snapshot?.documents?.mapNotNull { it.toUserProfile() }
                    ?.sortedBy { it.username.lowercase() } ?: emptyList()
                trySend(users)
            }
        awaitClose { listener.remove() }
    }

    override val pendingChallenges: Flow<List<Challenge>> = currentUser.flatMapLatest { user ->
        if (user == null) flowOf(emptyList())
        else callbackFlow {
            val listener = challengesCollection
                .whereEqualTo("challengedId", user.id)
                .whereEqualTo("status", ChallengeStatus.PENDING.name)
                .addSnapshotListener { snapshot, _ ->
                    val challenges = snapshot?.documents?.mapNotNull { it.toChallenge() } ?: emptyList()
                    trySend(challenges)
                }
            awaitClose { listener.remove() }
        }
    }

    override val sentChallenges: Flow<List<Challenge>> = currentUser.flatMapLatest { user ->
        if (user == null) flowOf(emptyList())
        else callbackFlow {
            val listener = challengesCollection
                .whereEqualTo("challengerId", user.id)
                .addSnapshotListener { snapshot, _ ->
                    val challenges = snapshot?.documents?.mapNotNull { it.toChallenge() } ?: emptyList()
                    trySend(challenges)
                }
            awaitClose { listener.remove() }
        }
    }

    override val completedChallenges: Flow<List<Challenge>> = currentUser.flatMapLatest { user ->
        if (user == null) flowOf(emptyList())
        else callbackFlow {
            val challengerListener = challengesCollection
                .whereEqualTo("challengerId", user.id)
                .whereEqualTo("status", ChallengeStatus.COMPLETED.name)
                .addSnapshotListener { snapshot, _ ->
                }

            val listener = challengesCollection
                .whereEqualTo("status", ChallengeStatus.COMPLETED.name)
                .addSnapshotListener { snapshot, _ ->
                    val allCompleted = snapshot?.documents?.mapNotNull { it.toChallenge() } ?: emptyList()
                    val userChallenges = allCompleted.filter {
                        it.challengerId == user.id || it.challengedId == user.id
                    }
                    trySend(userChallenges)
                }
            awaitClose { listener.remove() }
        }
    }

    override suspend fun register(username: String, email: String) {
        val firebaseUser = auth.currentUser ?: return
        val userId = firebaseUser.uid

        val existingDoc = usersCollection.document(userId).get().await()
        if (!existingDoc.exists()) {
            val userProfile = hashMapOf(
                "id" to userId,
                "username" to username.trim(),
                "email" to email.trim(),
                "friends" to emptyList<String>(),
                "bestScore" to 0,
                "totalTrophies" to 0
            )
            usersCollection.document(userId).set(userProfile).await()
        }
    }

    suspend fun signUp(email: String, password: String, username: String): Result<Unit> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            result.user?.let {
                register(username, email)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signIn(email: String, password: String): Result<Unit> {
        return try {
            auth.signInWithEmailAndPassword(email, password).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun logout() {
        auth.signOut()
    }

    override suspend fun clearSession() {
    }

    override suspend fun cleanupDuplicateChallenges() {
    }

    override suspend fun addFriend(friendId: String) {
        val userId = auth.currentUser?.uid ?: return
        val userDoc = usersCollection.document(userId)

        firestore.runTransaction { transaction ->
            val snapshot = transaction.get(userDoc)
            @Suppress("UNCHECKED_CAST")
            val currentFriends = snapshot.get("friends") as? List<String> ?: emptyList()
            if (friendId !in currentFriends) {
                transaction.update(userDoc, "friends", currentFriends + friendId)
            }
        }.await()
    }

    override suspend fun removeFriend(friendId: String) {
        val userId = auth.currentUser?.uid ?: return
        val userDoc = usersCollection.document(userId)

        firestore.runTransaction { transaction ->
            val snapshot = transaction.get(userDoc)
            @Suppress("UNCHECKED_CAST")
            val currentFriends = snapshot.get("friends") as? List<String> ?: emptyList()
            transaction.update(userDoc, "friends", currentFriends - friendId)
        }.await()
    }

    override fun getUsersWhoAddedMe(): Flow<List<UserProfile>> = currentUser.flatMapLatest { me ->
        if (me == null) flowOf(emptyList())
        else callbackFlow {
            val listener = usersCollection
                .whereArrayContains("friends", me.id)
                .addSnapshotListener { snapshot, _ ->
                    val usersWhoAddedMe = snapshot?.documents?.mapNotNull { it.toUserProfile() } ?: emptyList()
                    val myFriends = me.friends.toSet()
                    val notAddedBack = usersWhoAddedMe.filter { it.id !in myFriends && it.id != me.id }
                    trySend(notAddedBack)
                }
            awaitClose { listener.remove() }
        }
    }

    override suspend fun updateGameStats(score: Int, trophiesEarned: Int, rounds: Int, mistakes: Int) {
        val userId = auth.currentUser?.uid ?: return
        val userDoc = usersCollection.document(userId)

        firestore.runTransaction { transaction ->
            val snapshot = transaction.get(userDoc)
            val currentBest = snapshot.getLong("bestScore")?.toInt() ?: 0
            val currentTrophies = snapshot.getLong("totalTrophies")?.toInt() ?: 0

            val updates = hashMapOf<String, Any>(
                "totalTrophies" to (currentTrophies + trophiesEarned),
                "lastRounds" to rounds,
                "lastMistakes" to mistakes
            )
            if (score > currentBest) {
                updates["bestScore"] = score
            }
            transaction.update(userDoc, updates)
        }.await()

        _lastRounds.value = rounds
        _lastMistakes.value = mistakes
    }

    override fun getLastGameStats(): Flow<Pair<Int?, Int?>> = combine(_lastRounds, _lastMistakes) { rounds, mistakes ->
        Pair(rounds, mistakes)
    }

    override suspend fun sendChallenge(friendId: String, friendName: String, myScore: Int) {
        val userId = auth.currentUser?.uid ?: return
        val userDoc = usersCollection.document(userId).get().await()
        val myName = userDoc.getString("username") ?: return

        val challengeId = UUID.randomUUID().toString()
        val challenge = hashMapOf(
            "id" to challengeId,
            "challengerId" to userId,
            "challengerName" to myName,
            "challengerScore" to myScore,
            "challengedId" to friendId,
            "challengedName" to friendName,
            "challengedScore" to null,
            "status" to ChallengeStatus.PENDING.name,
            "createdAt" to System.currentTimeMillis()
        )
        challengesCollection.document(challengeId).set(challenge).await()
    }

    override suspend fun acceptChallenge(challengeId: String) {
        challengesCollection.document(challengeId)
            .update("status", ChallengeStatus.IN_PROGRESS.name)
            .await()
    }

    override suspend fun declineChallenge(challengeId: String) {
        challengesCollection.document(challengeId)
            .update("status", ChallengeStatus.DECLINED.name)
            .await()
    }

    override suspend fun completeChallenge(challengeId: String, myScore: Int) {
        challengesCollection.document(challengeId)
            .update(
                mapOf(
                    "challengedScore" to myScore,
                    "status" to ChallengeStatus.COMPLETED.name
                )
            )
            .await()
    }

    override suspend fun getChallengeById(challengeId: String): Challenge? {
        val doc = challengesCollection.document(challengeId).get().await()
        return doc.toChallenge()
    }

    override suspend fun addSampleChallengesIfNeeded() {
        initializeDefaultUsers()
        addBotsAsFriends()
    }

    private suspend fun addBotsAsFriends() {
        val userId = auth.currentUser?.uid ?: return

        val botsToAddUser = listOf("bot_alex", "bot_emma", "bot_nora").shuffled().take(2)

        for (botId in botsToAddUser) {
            val botDoc = usersCollection.document(botId)
            try {
                firestore.runTransaction { transaction ->
                    val snapshot = transaction.get(botDoc)
                    if (snapshot.exists()) {
                        @Suppress("UNCHECKED_CAST")
                        val currentFriends = snapshot.get("friends") as? List<String> ?: emptyList()
                        if (userId !in currentFriends) {
                            transaction.update(botDoc, "friends", currentFriends + userId)
                        }
                    }
                }.await()
            } catch (e: Exception) {
            }
        }
    }

    private suspend fun initializeDefaultUsers() {
        val defaultUsers = listOf(
            mapOf("id" to "bot_alex", "username" to "Alex", "email" to "alex@simonbot.com", "friends" to emptyList<String>(), "bestScore" to 42, "totalTrophies" to 4, "isBot" to true),
            mapOf("id" to "bot_sam", "username" to "Sam", "email" to "sam@simonbot.com", "friends" to emptyList<String>(), "bestScore" to 28, "totalTrophies" to 2, "isBot" to true),
            mapOf("id" to "bot_nora", "username" to "Nora", "email" to "nora@simonbot.com", "friends" to emptyList<String>(), "bestScore" to 65, "totalTrophies" to 6, "isBot" to true),
            mapOf("id" to "bot_yuki", "username" to "Yuki", "email" to "yuki@simonbot.com", "friends" to emptyList<String>(), "bestScore" to 15, "totalTrophies" to 1, "isBot" to true),
            mapOf("id" to "bot_marcus", "username" to "Marcus", "email" to "marcus@simonbot.com", "friends" to emptyList<String>(), "bestScore" to 88, "totalTrophies" to 8, "isBot" to true),
            mapOf("id" to "bot_luna", "username" to "Luna", "email" to "luna@simonbot.com", "friends" to emptyList<String>(), "bestScore" to 53, "totalTrophies" to 5, "isBot" to true),
            mapOf("id" to "bot_kai", "username" to "Kai", "email" to "kai@simonbot.com", "friends" to emptyList<String>(), "bestScore" to 37, "totalTrophies" to 3, "isBot" to true),
            mapOf("id" to "bot_emma", "username" to "Emma", "email" to "emma@simonbot.com", "friends" to emptyList<String>(), "bestScore" to 71, "totalTrophies" to 7, "isBot" to true)
        )

        for (user in defaultUsers) {
            val id = user["id"] as String
            try {
                val doc = usersCollection.document(id).get().await()
                if (!doc.exists()) {
                    usersCollection.document(id).set(user).await()
                }
            } catch (e: Exception) {
            }
        }
    }

    override suspend fun uploadProfilePicture(imageBytes: ByteArray): Result<String> {
        val userId = auth.currentUser?.uid ?: return Result.failure(Exception("Not authenticated"))
        return try {
            val compressedBytes = compressImage(imageBytes, maxSize = 200, quality = 70)
            val base64String = android.util.Base64.encodeToString(compressedBytes, android.util.Base64.NO_WRAP)
            val dataUri = "data:image/jpeg;base64,$base64String"

            usersCollection.document(userId)
                .update("profilePictureUrl", dataUri)
                .await()

            Result.success(dataUri)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun compressImage(imageBytes: ByteArray, maxSize: Int, quality: Int): ByteArray {
        val options = android.graphics.BitmapFactory.Options()
        options.inJustDecodeBounds = true
        android.graphics.BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size, options)

        val width = options.outWidth
        val height = options.outHeight
        var inSampleSize = 1
        while (width / inSampleSize > maxSize || height / inSampleSize > maxSize) {
            inSampleSize *= 2
        }

        options.inJustDecodeBounds = false
        options.inSampleSize = inSampleSize
        val bitmap = android.graphics.BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size, options)
            ?: return imageBytes

        val scale = minOf(maxSize.toFloat() / bitmap.width, maxSize.toFloat() / bitmap.height)
        val newWidth = (bitmap.width * scale).toInt()
        val newHeight = (bitmap.height * scale).toInt()
        val resized = android.graphics.Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)

        val outputStream = java.io.ByteArrayOutputStream()
        resized.compress(android.graphics.Bitmap.CompressFormat.JPEG, quality, outputStream)

        if (resized != bitmap) resized.recycle()
        bitmap.recycle()

        return outputStream.toByteArray()
    }

    override suspend fun removeProfilePicture(): Result<Unit> {
        val userId = auth.currentUser?.uid ?: return Result.failure(Exception("Not authenticated"))
        return try {
            usersCollection.document(userId)
                .update("profilePictureUrl", null)
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun dismissSuggestion(userId: String) {
        val currentUserId = auth.currentUser?.uid ?: return
        val userDoc = usersCollection.document(currentUserId)

        firestore.runTransaction { transaction ->
            val snapshot = transaction.get(userDoc)
            @Suppress("UNCHECKED_CAST")
            val currentDismissed = (snapshot.get("dismissedSuggestions") as? List<String>) ?: emptyList()
            if (userId !in currentDismissed) {
                transaction.update(userDoc, "dismissedSuggestions", currentDismissed + userId)
            }
        }.await()

        _dismissedSuggestions.value = _dismissedSuggestions.value + userId
    }

    override fun getDismissedSuggestions(): Flow<Set<String>> = _dismissedSuggestions

    override suspend fun deleteAccount(): Result<Unit> {
        val user = auth.currentUser ?: return Result.failure(Exception("Not authenticated"))
        val userId = user.uid

        return try {
            usersCollection.document(userId).delete().await()

            try {
                val challengesAsChallenger = challengesCollection
                    .whereEqualTo("challengerId", userId)
                    .get().await()
                challengesAsChallenger.documents.forEach { it.reference.delete().await() }

                val challengesAsChallenged = challengesCollection
                    .whereEqualTo("challengedId", userId)
                    .get().await()
                challengesAsChallenged.documents.forEach { it.reference.delete().await() }
            } catch (e: Exception) {
            }

            try {
                val usersWithThisFriend = usersCollection
                    .whereArrayContains("friends", userId)
                    .get().await()
                usersWithThisFriend.documents.forEach { doc ->
                    val currentFriends = (doc.get("friends") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                    doc.reference.update("friends", currentFriends.filter { it != userId }).await()
                }
            } catch (e: Exception) {
            }

            user.delete().await()

            _dismissedSuggestions.value = emptySet()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun com.google.firebase.firestore.DocumentSnapshot.toUserProfile(): UserProfile? {
        return try {
            UserProfile(
                id = getString("id") ?: return null,
                username = getString("username") ?: return null,
                email = getString("email") ?: "",
                friends = (get("friends") as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                bestScore = getLong("bestScore")?.toInt() ?: 0,
                totalTrophies = getLong("totalTrophies")?.toInt() ?: 0,
                profilePictureUrl = getString("profilePictureUrl")
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun com.google.firebase.firestore.DocumentSnapshot.toChallenge(): Challenge? {
        return try {
            Challenge(
                id = getString("id") ?: return null,
                challengerId = getString("challengerId") ?: return null,
                challengerName = getString("challengerName") ?: return null,
                challengerScore = getLong("challengerScore")?.toInt() ?: return null,
                challengedId = getString("challengedId") ?: return null,
                challengedName = getString("challengedName") ?: return null,
                challengedScore = getLong("challengedScore")?.toInt(),
                status = ChallengeStatus.valueOf(getString("status") ?: ChallengeStatus.PENDING.name),
                createdAt = getLong("createdAt") ?: System.currentTimeMillis()
            )
        } catch (e: Exception) {
            null
        }
    }
}