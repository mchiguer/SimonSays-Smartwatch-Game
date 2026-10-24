package com.epfl.esl.simongame.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.epfl.esl.simongame.R
import kotlinx.coroutines.delay

@Composable
fun SoloResultScreen(
    score: Int,
    maxScore: Int = score,
    rounds: Int,
    mistakes: Int,
    bestStreak: Int = 0,
    isChallenge: Boolean = false,
    challengerName: String = "",
    scoreToBeat: Int = 0,
    onPlayAgain: () -> Unit,
    onBackHome: () -> Unit
) {
    val challengeWon = isChallenge && score > scoreToBeat
    val challengeTied = isChallenge && score == scoreToBeat
    val gold = Color(0xFFE8C46A)
    val cyan = Color(0xFF5FE3FF)
    val deep = Color(0xFF0A0833)
    val green = Color(0xFF4ADE80)
    val red = Color(0xFFEF4444)
    val panel = RoundedCornerShape(18.dp)

    var showConfetti by remember { mutableStateOf(false) }
    var showContent by remember { mutableStateOf(false) }

    val contentScale by animateFloatAsState(
        targetValue = if (showContent) 1f else 0.8f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "contentScale"
    )

    val contentAlpha by animateFloatAsState(
        targetValue = if (showContent) 1f else 0f,
        animationSpec = tween(500),
        label = "contentAlpha"
    )

    LaunchedEffect(Unit) {
        delay(200)
        showContent = true
        delay(500)
        showConfetti = true
    }

    val rating = when {
        mistakes == 0 && score > 5 -> "LEGENDARY"
        score >= 30 -> "AWESOME"
        score >= 20 -> "GREAT"
        score >= 10 -> "GOOD"
        else -> "KEEP TRYING"
    }
    val ratingColor = when {
        mistakes == 0 && score > 5 -> Color(0xFFFFD700)
        score >= 30 -> green
        score >= 20 -> cyan
        score >= 10 -> gold
        else -> gold.copy(alpha = 0.7f)
    }

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
                .background(Color.Black.copy(alpha = 0.5f))
        )

        FloatingParticles(
            modifier = Modifier.fillMaxSize(),
            color = cyan.copy(alpha = 0.5f)
        )

        ConfettiExplosion(
            trigger = showConfetti,
            modifier = Modifier.fillMaxSize()
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .scale(contentScale)
                .graphicsLayer { alpha = contentAlpha },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (isChallenge) {
                ChallengeResultBanner(
                    won = challengeWon,
                    tied = challengeTied,
                    challengerName = challengerName,
                    myScore = score,
                    theirScore = scoreToBeat
                )
                Spacer(Modifier.height(20.dp))
            } else {
                PulsingTitle(text = "GAME OVER", color = cyan)
                Spacer(Modifier.height(24.dp))
                RatingBadge(rating = rating, color = ratingColor)
                Spacer(Modifier.height(32.dp))
            }

            ScoreCard(
                score = score,
                maxScore = maxScore,
                rounds = rounds,
                mistakes = mistakes,
                bestStreak = bestStreak
            )

            Spacer(Modifier.height(40.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                FigmaMenuButton(
                    label = if (isChallenge) "REMATCH" else "PLAY AGAIN",
                    font = PressStart2P,
                    onClick = onPlayAgain,
                    modifier = Modifier.height(72.dp)
                )

                FigmaMenuButton(
                    label = "HOME",
                    font = PressStart2P,
                    onClick = onBackHome,
                    modifier = Modifier.height(56.dp)
                )
            }
        }
    }
}

@Composable
private fun ChallengeResultBanner(
    won: Boolean,
    tied: Boolean,
    challengerName: String,
    myScore: Int,
    theirScore: Int
) {
    val green = Color(0xFF4ADE80)
    val red = Color(0xFFEF4444)
    val gold = Color(0xFFE8C46A)
    val cyan = Color(0xFF5FE3FF)
    val deep = Color(0xFF0A0833)

    val resultColor = when {
        won -> green
        tied -> gold
        else -> red
    }

    val resultText = when {
        won -> "YOU WIN!"
        tied -> "TIE!"
        else -> "YOU LOSE"
    }

    val resultEmoji = when {
        won -> "🏆"
        tied -> "🤝"
        else -> "😢"
    }

    val infiniteTransition = rememberInfiniteTransition(label = "challenge")
    val bannerScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (won) 1.05f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bannerScale"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.scale(bannerScale)
    ) {
        Text(
            text = "⚔️ CHALLENGE VS ${challengerName.uppercase()} ⚔️",
            fontFamily = PressStart2P,
            fontSize = 10.sp,
            color = cyan,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .background(resultColor.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                .border(3.dp, resultColor, RoundedCornerShape(16.dp))
                .padding(horizontal = 32.dp, vertical = 20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(text = resultEmoji, fontSize = 32.sp)
                Spacer(Modifier.width(16.dp))
                Text(
                    text = resultText,
                    fontFamily = PressStart2P,
                    fontSize = 24.sp,
                    color = resultColor
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .background(deep.copy(alpha = 0.8f), RoundedCornerShape(12.dp))
                .border(2.dp, gold.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                .padding(horizontal = 24.dp, vertical = 12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "YOU",
                        fontFamily = PressStart2P,
                        fontSize = 8.sp,
                        color = cyan
                    )
                    Text(
                        text = myScore.toString(),
                        fontFamily = PressStart2P,
                        fontSize = 20.sp,
                        color = if (won || tied) green else gold
                    )
                }

                Text(
                    text = "VS",
                    fontFamily = PressStart2P,
                    fontSize = 12.sp,
                    color = gold.copy(alpha = 0.7f)
                )

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = challengerName.uppercase().take(6),
                        fontFamily = PressStart2P,
                        fontSize = 8.sp,
                        color = Color(0xFFFF9500)
                    )
                    Text(
                        text = theirScore.toString(),
                        fontFamily = PressStart2P,
                        fontSize = 20.sp,
                        color = if (!won && !tied) green else gold
                    )
                }
            }
        }
    }
}

@Composable
private fun PulsingTitle(text: String, color: Color) {
    val infiniteTransition = rememberInfiniteTransition(label = "title")

    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "titleScale"
    )

    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    Box(
        modifier = Modifier.scale(scale),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontFamily = PressStart2P,
            fontSize = 28.sp,
            color = color.copy(alpha = glowAlpha),
            modifier = Modifier
                .offset(x = 2.dp, y = 2.dp)
                .scale(1.1f)
        )
        Text(
            text = text,
            fontFamily = PressStart2P,
            fontSize = 28.sp,
            color = color
        )
    }
}

@Composable
private fun RatingBadge(rating: String, color: Color) {
    val infiniteTransition = rememberInfiniteTransition(label = "rating")

    val badgeScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "badgeScale"
    )

    Box(
        modifier = Modifier
            .scale(badgeScale)
            .background(color.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
            .border(2.dp, color, RoundedCornerShape(12.dp))
            .padding(horizontal = 24.dp, vertical = 12.dp)
    ) {
        Text(
            text = rating,
            fontFamily = PressStart2P,
            fontSize = 16.sp,
            color = color
        )
    }
}

@Composable
private fun ScoreCard(
    score: Int,
    maxScore: Int,
    rounds: Int,
    mistakes: Int,
    bestStreak: Int
) {
    val gold = Color(0xFFE8C46A)
    val cyan = Color(0xFF5FE3FF)
    val deep = Color(0xFF0A0833)
    val green = Color(0xFF4ADE80)
    val red = Color(0xFFEF4444)
    val orange = Color(0xFFFF6B35)
    val panel = RoundedCornerShape(18.dp)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(3.dp, gold, panel)
            .background(deep.copy(alpha = 0.85f), panel)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "FINAL SCORE",
            fontFamily = PressStart2P,
            fontSize = 12.sp,
            color = cyan
        )

        Spacer(Modifier.height(12.dp))

        AnimatedScoreCounter(
            targetScore = score,
            fontSize = 56.sp,
            textColor = gold,
            durationMs = 2000
        )

        Spacer(Modifier.height(24.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .height(2.dp)
                .background(gold.copy(alpha = 0.3f))
        )

        Spacer(Modifier.height(20.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatItem(label = "ROUNDS", value = rounds.toString(), color = cyan)
            StatItem(label = "TROPHIES", value = "🏆 ${maxScore / 10}", color = gold)
        }

        if (bestStreak > 1) {
            Spacer(Modifier.height(16.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                val infiniteTransition = rememberInfiniteTransition(label = "fire")
                val fireScale by infiniteTransition.animateFloat(
                    initialValue = 0.9f,
                    targetValue = 1.1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(300),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "fireScale"
                )

                Box(modifier = Modifier.scale(fireScale)) {
                    Text(
                        text = "BEST STREAK: $bestStreak",
                        fontFamily = PressStart2P,
                        fontSize = 12.sp,
                        color = orange
                    )
                }
            }
        }
    }
}

@Composable
private fun StatItem(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            fontFamily = PressStart2P,
            fontSize = 8.sp,
            color = color.copy(alpha = 0.7f)
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = value,
            fontFamily = PressStart2P,
            fontSize = 18.sp,
            color = color
        )
    }
}