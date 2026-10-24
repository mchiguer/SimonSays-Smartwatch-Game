package com.epfl.esl.simongame.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import com.epfl.esl.simongame.R
import com.epfl.esl.simongame.data.Challenge
import com.epfl.esl.simongame.data.UserProfile
import com.epfl.esl.simongame.friends.FriendsListUiState

@Composable
private fun ChallengeScoreDialog(
    friendName: String,
    myBestScore: Int,
    onDismiss: () -> Unit,
    onConfirm: (score: Int) -> Unit
) {
    val gold = Color(0xFFE8C46A)
    val cyan = Color(0xFF5FE3FF)
    val deep = Color(0xFF0A0833)

    var showCustomInput by remember { mutableStateOf(false) }
    var customScore by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(deep, RoundedCornerShape(20.dp))
                .border(2.dp, gold, RoundedCornerShape(20.dp))
                .padding(20.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "CHALLENGE",
                    fontFamily = PressStart2P,
                    fontSize = 14.sp,
                    color = gold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = friendName.uppercase(),
                    fontFamily = PressStart2P,
                    fontSize = 12.sp,
                    color = cyan
                )

                Spacer(Modifier.height(20.dp))

                Text(
                    text = "CHOOSE SCORE TO BEAT:",
                    fontFamily = PressStart2P,
                    fontSize = 9.sp,
                    color = Color(0xFFB9B4FF)
                )

                Spacer(Modifier.height(16.dp))

                if (!showCustomInput) {
                    // Best Score button
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(gold.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
                            .border(2.dp, gold, RoundedCornerShape(10.dp))
                            .clickable { onConfirm(myBestScore) }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "BEST SCORE ($myBestScore)",
                            fontFamily = PressStart2P,
                            fontSize = 10.sp,
                            color = gold
                        )
                    }

                    Spacer(Modifier.height(10.dp))

                    val halfScore = myBestScore / 2
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(cyan.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
                            .border(2.dp, cyan, RoundedCornerShape(10.dp))
                            .clickable { onConfirm(halfScore) }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "50% ($halfScore)",
                            fontFamily = PressStart2P,
                            fontSize = 10.sp,
                            color = cyan
                        )
                    }

                    Spacer(Modifier.height(10.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFFF9500).copy(alpha = 0.2f), RoundedCornerShape(10.dp))
                            .border(2.dp, Color(0xFFFF9500), RoundedCornerShape(10.dp))
                            .clickable { showCustomInput = true }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "CUSTOM...",
                            fontFamily = PressStart2P,
                            fontSize = 10.sp,
                            color = Color(0xFFFF9500)
                        )
                    }
                } else {
                    OutlinedTextField(
                        value = customScore,
                        onValueChange = { customScore = it.filter { c -> c.isDigit() } },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        label = {
                            Text(
                                "Enter score",
                                fontFamily = PressStart2P,
                                fontSize = 8.sp,
                                color = gold
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = gold,
                            unfocusedBorderColor = gold,
                            focusedTextColor = gold,
                            unfocusedTextColor = gold,
                            cursorColor = gold,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent
                        ),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(Color.Gray.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
                                .border(2.dp, Color.Gray, RoundedCornerShape(10.dp))
                                .clickable { showCustomInput = false }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "BACK",
                                fontFamily = PressStart2P,
                                fontSize = 9.sp,
                                color = Color.Gray
                            )
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(gold.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
                                .border(2.dp, gold, RoundedCornerShape(10.dp))
                                .clickable {
                                    val score = customScore.toIntOrNull() ?: 0
                                    if (score > 0) onConfirm(score)
                                }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "SEND",
                                fontFamily = PressStart2P,
                                fontSize = 9.sp,
                                color = gold
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                Text(
                    text = "CANCEL",
                    fontFamily = PressStart2P,
                    fontSize = 10.sp,
                    color = Color(0xFFEF4444),
                    modifier = Modifier.clickable { onDismiss() }
                )
            }
        }
    }
}

@Composable
private fun FriendListTitle(modifier: Modifier = Modifier) {
    val bungee = FontFamily(androidx.compose.ui.text.font.Font(R.font.bungee_simon))

    @Composable
    fun OutlinedTitle(
        text: String,
        fill: Color,
        outline: Color = Color(0xFF14105A),
        shadowColor: Color = Color(0xFF0A0833),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = text,
                fontFamily = bungee,
                fontSize = 44.sp,
                color = outline,
                style = TextStyle(drawStyle = Stroke(width = 10f))
            )
            Text(
                text = text,
                fontFamily = bungee,
                fontSize = 44.sp,
                color = fill,
                style = TextStyle(
                    shadow = Shadow(
                        color = shadowColor,
                        offset = Offset(6f, 6f),
                        blurRadius = 0f
                    )
                )
            )
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        OutlinedTitle("FRIENDS", fill = Color(0xFFE8C46A))
        Spacer(Modifier.height(4.dp))
        OutlinedTitle("LIST", fill = Color(0xFF5FE3FF))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeToDeleteFriendCard(
    user: UserProfile,
    onChallenge: () -> Unit,
    onRemove: () -> Unit,
    hasPendingChallenge: Boolean = false,
    modifier: Modifier = Modifier
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onRemove()
                true
            } else {
                false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        modifier = modifier,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFFF6B6B), RoundedCornerShape(18.dp))
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Remove",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        },
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = true
    ) {
        FriendCardWithChallenge(
            user = user,
            onChallenge = onChallenge,
            hasPendingChallenge = hasPendingChallenge
        )
    }
}

@Composable
private fun FriendCardWithChallenge(
    user: UserProfile,
    onChallenge: () -> Unit,
    hasPendingChallenge: Boolean = false,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(18.dp)
    val gold = Color(0xFFE8C46A)
    val cyan = Color(0xFF5FE3FF)
    val gray = Color(0xFF888888)

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = shape,
        color = Color.Transparent,
        border = BorderStroke(2.dp, gold)
    ) {
        Box {
            Image(
                painter = painterResource(R.drawable.btn_background),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = user.username,
                        fontFamily = PressStart2P,
                        fontSize = 14.sp,
                        color = gold
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "🏆 ${user.totalTrophies}",
                            fontFamily = PressStart2P,
                            fontSize = 10.sp,
                            color = Color(0xFFB9B4FF)
                        )
                        Text(
                            text = "BEST: ${user.bestScore}",
                            fontFamily = PressStart2P,
                            fontSize = 10.sp,
                            color = Color(0xFFB9B4FF)
                        )
                    }
                }

                if (hasPendingChallenge) {
                    Box(
                        modifier = Modifier
                            .background(gray.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
                            .border(2.dp, gray, RoundedCornerShape(10.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "⏳ SENT",
                            fontFamily = PressStart2P,
                            fontSize = 9.sp,
                            color = gray
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .background(cyan.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
                            .border(2.dp, cyan, RoundedCornerShape(10.dp))
                            .clickable { onChallenge() }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "⚔️ DEFI",
                            fontFamily = PressStart2P,
                            fontSize = 9.sp,
                            color = cyan
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChallengeCard(
    challenge: Challenge,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(18.dp)
    val orange = Color(0xFFFF9500)
    val green = Color(0xFF4ADE80)
    val red = Color(0xFFEF4444)
    val deep = Color(0xFF0A0833)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(deep.copy(alpha = 0.9f), shape)
            .border(2.dp, orange, shape)
            .padding(16.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "⚔️", fontSize = 24.sp)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        text = "CHALLENGE FROM",
                        fontFamily = PressStart2P,
                        fontSize = 8.sp,
                        color = orange
                    )
                    Text(
                        text = challenge.challengerName.uppercase(),
                        fontFamily = PressStart2P,
                        fontSize = 14.sp,
                        color = Color(0xFFE8C46A)
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            Text(
                text = "BEAT MY SCORE: ${challenge.challengerScore}",
                fontFamily = PressStart2P,
                fontSize = 11.sp,
                color = Color.White
            )

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(green.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
                        .border(2.dp, green, RoundedCornerShape(10.dp))
                        .clickable { onAccept() }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "ACCEPT",
                        fontFamily = PressStart2P,
                        fontSize = 10.sp,
                        color = green
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(red.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
                        .border(2.dp, red, RoundedCornerShape(10.dp))
                        .clickable { onDecline() }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "DECLINE",
                        fontFamily = PressStart2P,
                        fontSize = 10.sp,
                        color = red
                    )
                }
            }
        }
    }
}

@Composable
fun FriendsListScreen(
    ui: FriendsListUiState,
    onBack: () -> Unit,
    onChallengeFriend: (friendId: String, friendName: String, myScore: Int) -> Unit = { _, _, _ -> },
    onAcceptChallenge: (challengeId: String) -> Unit = {},
    onDeclineChallenge: (challengeId: String) -> Unit = {},
    onPlayChallenge: (challengeId: String, challengerName: String, scoreToBeat: Int) -> Unit = { _, _, _ -> },
    onRemoveFriend: (friendId: String) -> Unit = {}
) {
    val gold = Color(0xFFE8C46A)
    val cyan = Color(0xFF5FE3FF)
    val orange = Color(0xFFFF9500)


    var showChallengeDialog by remember { mutableStateOf(false) }
    var selectedFriend by remember { mutableStateOf<UserProfile?>(null) }

    if (showChallengeDialog && selectedFriend != null) {
        ChallengeScoreDialog(
            friendName = selectedFriend!!.username,
            myBestScore = ui.myBestScore,
            onDismiss = {
                showChallengeDialog = false
                selectedFriend = null
            },
            onConfirm = { score ->
                onChallengeFriend(selectedFriend!!.id, selectedFriend!!.username, score)
                showChallengeDialog = false
                selectedFriend = null
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {

        Image(
            painter = painterResource(R.drawable.background),
            contentDescription = null,
            modifier = Modifier.matchParentSize(),
            contentScale = ContentScale.Crop
        )

        Box(
            modifier = Modifier
                .matchParentSize()
                .background(Color.Black.copy(alpha = 0.35f))
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 18.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "< BACK",
                    fontFamily = PressStart2P,
                    fontSize = 12.sp,
                    color = gold,
                    modifier = Modifier.clickable { onBack() }
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = "FRIENDS",
                    fontFamily = PressStart2P,
                    fontSize = 12.sp,
                    color = cyan
                )
            }

            Spacer(Modifier.height(20.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                if (ui.pendingChallenges.isNotEmpty()) {
                    item {
                        Text(
                            text = "⚔️ CHALLENGES (${ui.pendingChallenges.size})",
                            fontFamily = PressStart2P,
                            fontSize = 11.sp,
                            color = orange
                        )
                        Spacer(Modifier.height(8.dp))
                    }

                    items(ui.pendingChallenges, key = { "pending_${it.id}" }) { challenge ->
                        ChallengeCard(
                            challenge = challenge,
                            onAccept = {
                                onAcceptChallenge(challenge.id)
                                onPlayChallenge(challenge.id, challenge.challengerName, challenge.challengerScore)
                            },
                            onDecline = { onDeclineChallenge(challenge.id) }
                        )
                    }

                    item {
                        Spacer(Modifier.height(16.dp))
                    }
                }

                item {
                    Text(
                        text = "👥 YOUR FRIENDS (${ui.friends.size})",
                        fontFamily = PressStart2P,
                        fontSize = 11.sp,
                        color = gold
                    )
                    Spacer(Modifier.height(8.dp))
                }

                if (ui.friends.isEmpty()) {
                    item {
                        Text(
                            text = "NO FRIENDS YET\nADD SOME FROM PROFILE!",
                            fontFamily = PressStart2P,
                            fontSize = 10.sp,
                            color = Color(0xFFB9B4FF)
                        )
                    }
                } else {
                    val challengedFriendIds = ui.sentChallenges
                        .filter { it.status == com.epfl.esl.simongame.data.ChallengeStatus.PENDING }
                        .map { it.challengedId }
                        .toSet()

                    items(ui.friends, key = { "friend_${it.id}" }) { friend ->
                        SwipeToDeleteFriendCard(
                            user = friend,
                            onChallenge = {
                                selectedFriend = friend
                                showChallengeDialog = true
                            },
                            hasPendingChallenge = friend.id in challengedFriendIds,
                            onRemove = { onRemoveFriend(friend.id) }
                        )
                    }
                }
            }
        }
    }
}
