package com.epfl.esl.simongame.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.epfl.esl.simongame.R
import com.epfl.esl.simongame.data.UserProfile


data class LeaderboardEntry(
    val rank: Int,
    val username: String,
    val trophies: Int,
    val bestScore: Int,
    val isCurrentUser: Boolean = false
)

@Composable
fun MultiGameScreen(
    onBack: () -> Unit,
    currentUser: UserProfile? = null,
    friends: List<UserProfile> = emptyList()
) {
    val gold = Color(0xFFE8C46A)
    val cyan = Color(0xFF5FE3FF)
    val deep = Color(0xFF0A0833)
    val panel = RoundedCornerShape(18.dp)

    val leaderboardEntries = buildLeaderboard(currentUser, friends)

    Box(modifier = Modifier.fillMaxSize()) {

        Image(
            painter = painterResource(R.drawable.background),
            contentDescription = null,
            modifier = Modifier.matchParentSize(),
            contentScale = ContentScale.Crop
        )
        Box(
            Modifier
                .matchParentSize()
                .background(Color.Black.copy(alpha = 0.35f))
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
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
                    text = "LEADERBOARD",
                    fontFamily = PressStart2P,
                    fontSize = 12.sp,
                    color = cyan
                )
            }

            Spacer(Modifier.height(20.dp))

            Box(
                modifier = Modifier
                    .background(deep.copy(alpha = 0.7f), panel)
                    .border(2.dp, gold, panel)
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(text = "🏆", fontSize = 24.sp)
                    Text(
                        text = "FRIENDS RANKING",
                        fontFamily = PressStart2P,
                        fontSize = 14.sp,
                        color = gold
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            if (leaderboardEntries.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(deep.copy(alpha = 0.55f), panel)
                        .border(2.dp, gold, panel)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "NO FRIENDS YET",
                            fontFamily = PressStart2P,
                            fontSize = 14.sp,
                            color = gold
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "Add friends to see",
                            fontFamily = PressStart2P,
                            fontSize = 10.sp,
                            color = Color(0xFFB9B4FF)
                        )
                        Text(
                            text = "the leaderboard!",
                            fontFamily = PressStart2P,
                            fontSize = 10.sp,
                            color = Color(0xFFB9B4FF)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(leaderboardEntries) { index, entry ->
                        LeaderboardRow(entry = entry)
                    }
                }
            }

        }
    }
}

@Composable
private fun LeaderboardRow(entry: LeaderboardEntry) {
    val gold = Color(0xFFE8C46A)
    val cyan = Color(0xFF5FE3FF)
    val deep = Color(0xFF0A0833)
    val panel = RoundedCornerShape(14.dp)

    val rankColor = when (entry.rank) {
        1 -> Color(0xFFFFD700)
        2 -> Color(0xFFC0C0C0)
        3 -> Color(0xFFCD7F32)
        else -> gold
    }

    val borderColor = if (entry.isCurrentUser) cyan else gold.copy(alpha = 0.6f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(deep.copy(alpha = 0.7f), panel)
            .border(2.dp, borderColor, panel)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(rankColor.copy(alpha = 0.2f))
                .border(2.dp, rankColor, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "#${entry.rank}",
                fontFamily = PressStart2P,
                fontSize = 10.sp,
                color = rankColor
            )
        }

        Spacer(Modifier.width(12.dp))

        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.3f))
                .border(2.dp, if (entry.isCurrentUser) cyan else gold.copy(alpha = 0.5f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = entry.username.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                fontFamily = PressStart2P,
                fontSize = 14.sp,
                color = if (entry.isCurrentUser) cyan else gold
            )
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = entry.username,
                    fontFamily = PressStart2P,
                    fontSize = 11.sp,
                    color = if (entry.isCurrentUser) cyan else gold
                )
                if (entry.isCurrentUser) {
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "(YOU)",
                        fontFamily = PressStart2P,
                        fontSize = 8.sp,
                        color = cyan.copy(alpha = 0.7f)
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = "BEST: ${entry.bestScore}",
                fontFamily = PressStart2P,
                fontSize = 8.sp,
                color = Color(0xFFB9B4FF)
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(text = "🏆", fontSize = 18.sp)
            Text(
                text = "${entry.trophies}",
                fontFamily = PressStart2P,
                fontSize = 14.sp,
                color = gold
            )
        }
    }
}

private fun buildLeaderboard(
    currentUser: UserProfile?,
    friends: List<UserProfile>
): List<LeaderboardEntry> {
    val allUsers = mutableListOf<LeaderboardEntry>()

    currentUser?.let {
        allUsers.add(
            LeaderboardEntry(
                rank = 0,
                username = it.username,
                trophies = it.totalTrophies,
                bestScore = it.bestScore,
                isCurrentUser = true
            )
        )
    }

    friends.forEach { friend ->
        allUsers.add(
            LeaderboardEntry(
                rank = 0,
                username = friend.username,
                trophies = friend.totalTrophies,
                bestScore = friend.bestScore,
                isCurrentUser = false
            )
        )
    }

    val sorted = allUsers.sortedWith(
        compareByDescending<LeaderboardEntry> { it.trophies }
            .thenByDescending { it.bestScore }
    )

    return sorted.mapIndexed { index, entry ->
        entry.copy(rank = index + 1)
    }
}