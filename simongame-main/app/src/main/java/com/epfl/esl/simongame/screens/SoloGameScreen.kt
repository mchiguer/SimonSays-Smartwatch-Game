package com.epfl.esl.simongame.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.epfl.esl.simongame.R
import com.epfl.esl.simongame.data.getMoveName
import com.epfl.esl.simongame.game.Move
import com.epfl.esl.simongame.solo_mode.SoloGamePhase
import com.epfl.esl.simongame.solo_mode.SoloGameUiState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

fun getMultiplierColor(multiplier: Int): Color = when (multiplier) {
    2 -> Color(0xFF4ADE80)
    3 -> Color(0xFF60A5FA)
    4 -> Color(0xFFA855F7)
    5 -> Color(0xFFF472B6)
    else -> Color(0xFFFF6B6B)
}

@Composable
fun ShakeableContainer(
    shake: Boolean,
    onShakeComplete: () -> Unit,
    content: @Composable () -> Unit
) {
    var offsetX by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(shake) {
        if (shake) {
            val shakePattern = listOf(25f, -25f, 20f, -20f, 15f, -15f, 10f, -10f, 5f, -5f, 0f)
            for (offset in shakePattern) {
                offsetX = offset
                delay(30)
            }
            onShakeComplete()
        }
    }

    Box(
        modifier = Modifier.graphicsLayer { translationX = offsetX }
    ) {
        content()
    }
}

@Composable
fun AnimatedMultiStat(
    multiplier: Int,
    modifier: Modifier = Modifier
) {
    val gold = Color(0xFFE8C46A)
    val color = if (multiplier > 1) getMultiplierColor(multiplier) else gold

    var lastMultiplier by remember { mutableIntStateOf(multiplier) }
    var triggerPulse by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (triggerPulse) 1.5f else 1f,
        animationSpec = spring(dampingRatio = 0.3f, stiffness = 200f),
        label = "multiScale"
    )

    LaunchedEffect(multiplier) {
        if (multiplier != lastMultiplier && multiplier > 1) {
            triggerPulse = true
            delay(400)
            triggerPulse = false
            lastMultiplier = multiplier
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Text(
            text = "MULTI",
            fontFamily = PressStart2P,
            fontSize = 9.sp,
            color = color
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "x$multiplier",
            fontFamily = PressStart2P,
            fontSize = 12.sp,
            color = color,
            modifier = Modifier.scale(scale)
        )
    }
}

@Composable
fun SoloGameScreen(
    uiState: SoloGameUiState,
    onBack: () -> Unit,
    onStart: () -> Unit,
    onContinue: () -> Unit,
    onRestart: () -> Unit,
    onShakeComplete: () -> Unit = {},
    onDone: () -> Unit = {},
    onTransitionToGameOver: () -> Unit = {},
    challengeMode: Boolean = false,
    challengerName: String = "",
    scoreToBeat: Int = 0,
    language: com.epfl.esl.simongame.data.AppLanguage = com.epfl.esl.simongame.data.AppLanguage.ENGLISH
) {
    val strings = com.epfl.esl.simongame.data.AppStrings.get(language)
    val getMoveName = { move: com.epfl.esl.simongame.game.Move ->
        strings.getMoveName(move.name)
    }
    val gold = Color(0xFFE8C46A)
    val cyan = Color(0xFF5FE3FF)
    val deep = Color(0xFF0A0833)
    val green = Color(0xFF4ADE80)
    val red = Color(0xFFEF4444)
    val panel = RoundedCornerShape(18.dp)

    var showFlyingScore by remember { mutableStateOf(false) }
    var showTrapAnimation by remember { mutableStateOf(false) }
    var lastAnimatedRound by remember { mutableIntStateOf(0) }

    var showCelebrationBg by remember { mutableStateOf(false) }
    var celebrationColor by remember { mutableStateOf(Color.Transparent) }

    var animateScore by remember { mutableStateOf(false) }

    var displayedScore by remember { mutableFloatStateOf(uiState.score) }

    var showHeartBreak by remember { mutableStateOf(false) }
    var lastMistakeCount by remember { mutableIntStateOf(0) }

    var lastTrophyCount by remember { mutableIntStateOf(0) }
    var showTrophyNotification by remember { mutableStateOf(false) }
    var newTrophyCount by remember { mutableIntStateOf(0) }

    var showLivesNotification by remember { mutableStateOf(false) }
    var livesRemaining by remember { mutableIntStateOf(3) }

    val orange = Color(0xFFFF9500)
    LaunchedEffect(uiState.phase, uiState.round, uiState.lastResultCorrect, uiState.fellForTrap) {
        if (uiState.phase == SoloGamePhase.RESULT &&
            uiState.lastResultCorrect == true &&
            uiState.round != lastAnimatedRound
        ) {
            lastAnimatedRound = uiState.round
            showFlyingScore = true
            celebrationColor = green.copy(alpha = 0.25f)
            showCelebrationBg = true
        } else if (uiState.phase == SoloGamePhase.RESULT &&
            uiState.fellForTrap &&
            uiState.round != lastAnimatedRound
        ) {
            lastAnimatedRound = uiState.round
            celebrationColor = orange.copy(alpha = 0.25f)
            showCelebrationBg = true
            showTrapAnimation = true
        } else if (uiState.phase == SoloGamePhase.RESULT &&
            uiState.lastResultCorrect == false &&
            !uiState.fellForTrap &&
            uiState.round != lastAnimatedRound
        ) {
            lastAnimatedRound = uiState.round
            celebrationColor = red.copy(alpha = 0.25f)
            showCelebrationBg = true
        }
    }

    LaunchedEffect(uiState.mistakes) {
        if (uiState.mistakes > lastMistakeCount && uiState.mistakes > 0) {
            showHeartBreak = true
            livesRemaining = 3 - uiState.mistakes
            showLivesNotification = true
        }
        lastMistakeCount = uiState.mistakes
    }

    LaunchedEffect(displayedScore) {
        val currentTrophyCount = (displayedScore / 10).toInt()
        if (currentTrophyCount > lastTrophyCount) {
            newTrophyCount = currentTrophyCount
            showTrophyNotification = true
        }
        lastTrophyCount = currentTrophyCount
    }

    LaunchedEffect(uiState.pendingGameOver) {
        if (uiState.pendingGameOver && uiState.phase == SoloGamePhase.RESULT) {
            delay(2500L)  // Show ML results for 2.5 seconds
            onTransitionToGameOver()
        }
    }

    ShakeableContainer(
        shake = uiState.triggerShake,
        onShakeComplete = onShakeComplete
    ) {
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

            if (showCelebrationBg) {
                Box(
                    Modifier
                        .matchParentSize()
                        .background(celebrationColor)
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val trophyCount = (displayedScore / 10).toInt()
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "🏆",
                            fontSize = 18.sp
                        )
                        Text(
                            text = "+$trophyCount",
                            fontFamily = PressStart2P,
                            fontSize = 14.sp,
                            color = gold
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    Text(
                        text = strings.solo,
                        fontFamily = PressStart2P,
                        fontSize = 12.sp,
                        color = cyan
                    )
                }

                Spacer(Modifier.height(18.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(2.dp, gold, panel)
                        .background(Color(0xFF15104A).copy(alpha = 0.55f), panel)
                        .padding(horizontal = 12.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    StatSimple(strings.round, uiState.round.toString(), gold)

                    AnimatedScoreStat(
                        score = displayedScore,
                        isAnimating = animateScore,
                        color = gold,
                        strings = strings
                    )

                    ComboStat(multiplier = uiState.multiplier, strings = strings)

                    LivesStat(lives = 3 - uiState.mistakes, strings = strings)
                }

                Spacer(Modifier.height(20.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .border(2.dp, cyan, panel)
                        .background(deep.copy(alpha = 0.55f), panel),
                    contentAlignment = Alignment.Center
                ) {
                    when (uiState.phase) {
                        SoloGamePhase.SHOWING_MOVE -> {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                if (uiState.isSimonSays) {
                                    Text(
                                        text = strings.simonSays,
                                        fontFamily = PressStart2P,
                                        fontSize = 14.sp,
                                        color = cyan
                                    )
                                    Spacer(Modifier.height(12.dp))
                                } else {
                                    Text(
                                        text = strings.doThisMove,
                                        fontFamily = PressStart2P,
                                        fontSize = 12.sp,
                                        color = gold.copy(alpha = 0.6f)
                                    )
                                    Spacer(Modifier.height(12.dp))
                                }
                                Box {
                                    Text(
                                        text = getMoveName(uiState.expectedMove),
                                        fontFamily = PressStart2P,
                                        fontSize = 20.sp,
                                        color = gold.copy(alpha = 0.3f),
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.scale(1.05f)
                                    )
                                    Text(
                                        text = getMoveName(uiState.expectedMove),
                                        fontFamily = PressStart2P,
                                        fontSize = 20.sp,
                                        color = gold,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }

                        SoloGamePhase.COUNTDOWN -> {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                if (uiState.isSimonSays) {
                                    Text(
                                        text = strings.simonSays,
                                        fontFamily = PressStart2P,
                                        fontSize = 12.sp,
                                        color = cyan
                                    )
                                    Spacer(Modifier.height(4.dp))
                                }
                                Text(
                                    text = getMoveName(uiState.expectedMove),
                                    fontFamily = PressStart2P,
                                    fontSize = 18.sp,
                                    color = gold,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(Modifier.height(8.dp))
                                val infiniteTransition = rememberInfiniteTransition(label = "go")
                                val goScale by infiniteTransition.animateFloat(
                                    initialValue = 1f,
                                    targetValue = 1.2f,
                                    animationSpec = infiniteRepeatable(
                                        animation = tween(300),
                                        repeatMode = RepeatMode.Reverse
                                    ),
                                    label = "goScale"
                                )
                                Text(
                                    text = strings.go,
                                    fontFamily = PressStart2P,
                                    fontSize = 16.sp,
                                    color = cyan,
                                    modifier = Modifier.scale(goScale)
                                )
                            }
                        }

                        SoloGamePhase.EVALUATING -> {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                val infiniteTransition = rememberInfiniteTransition(label = "eval")
                                val evalAlpha by infiniteTransition.animateFloat(
                                    initialValue = 0.5f,
                                    targetValue = 1f,
                                    animationSpec = infiniteRepeatable(
                                        animation = tween(500),
                                        repeatMode = RepeatMode.Reverse
                                    ),
                                    label = "evalAlpha"
                                )
                                Text(
                                    text = strings.analyzing,
                                    fontFamily = PressStart2P,
                                    fontSize = 16.sp,
                                    color = cyan.copy(alpha = evalAlpha)
                                )
                            }
                        }

                        SoloGamePhase.RETRY -> {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = strings.movementUnclear,
                                    fontFamily = PressStart2P,
                                    fontSize = 16.sp,
                                    color = Color(0xFFFFB74D)
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = strings.skipping,
                                    fontFamily = PressStart2P,
                                    fontSize = 10.sp,
                                    color = Color.White.copy(alpha = 0.7f)
                                )
                            }
                        }

                        SoloGamePhase.RESULT -> {
                            val orange = Color(0xFFFF9500)
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                if (uiState.lastResultCorrect == true) {
                                    // Pulsing BRAVO
                                    val infiniteTransition = rememberInfiniteTransition(label = "bravo")
                                    val bravoScale by infiniteTransition.animateFloat(
                                        initialValue = 1f,
                                        targetValue = 1.1f,
                                        animationSpec = infiniteRepeatable(
                                            animation = tween(400),
                                            repeatMode = RepeatMode.Reverse
                                        ),
                                        label = "bravoScale"
                                    )
                                    if (uiState.stayedIdle) {
                                        Text(
                                            text = "IDLE",
                                            fontFamily = PressStart2P,
                                            fontSize = 28.sp,
                                            color = green,
                                            modifier = Modifier.scale(bravoScale)
                                        )
                                        Spacer(Modifier.height(4.dp))
                                        Text(
                                            text = strings.goodJob,
                                            fontFamily = PressStart2P,
                                            fontSize = 12.sp,
                                            color = cyan
                                        )
                                    } else {
                                        Text(
                                            text = strings.great,
                                            fontFamily = PressStart2P,
                                            fontSize = 28.sp,
                                            color = green,
                                            modifier = Modifier.scale(bravoScale)
                                        )
                                    }
                                    Spacer(Modifier.height(4.dp))
                                    val timeSeconds = minOf(uiState.elapsedTimeMs / 1000f, 5.0f)
                                    val speedBonusPercent = (uiState.speedBonus * 100).toInt()
                                    Text(
                                        text = "%.1fs | +%d%% SPEED".format(timeSeconds, speedBonusPercent),
                                        fontFamily = PressStart2P,
                                        fontSize = 10.sp,
                                        color = cyan
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = "+${"%.1f".format(uiState.pointsEarned)} pts",
                                        fontFamily = PressStart2P,
                                        fontSize = 14.sp,
                                        color = gold
                                    )
                                } else if (uiState.fellForTrap) {
                                    val infiniteTransition = rememberInfiniteTransition(label = "trap")
                                    val trapScale by infiniteTransition.animateFloat(
                                        initialValue = 1f,
                                        targetValue = 1.15f,
                                        animationSpec = infiniteRepeatable(
                                            animation = tween(300),
                                            repeatMode = RepeatMode.Reverse
                                        ),
                                        label = "trapScale"
                                    )
                                    Text(
                                        text = strings.trap,
                                        fontFamily = PressStart2P,
                                        fontSize = 28.sp,
                                        color = orange,
                                        modifier = Modifier.scale(trapScale)
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        text = strings.iDidntSay,
                                        fontFamily = PressStart2P,
                                        fontSize = 10.sp,
                                        color = Color.White.copy(alpha = 0.8f)
                                    )
                                    Text(
                                        text = strings.simonSaysExclaim,
                                        fontFamily = PressStart2P,
                                        fontSize = 12.sp,
                                        color = cyan
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = strings.minusPoint,
                                        fontFamily = PressStart2P,
                                        fontSize = 10.sp,
                                        color = red
                                    )
                                } else if (uiState.lastResultCorrect == null) {
                                    Text(
                                        text = strings.movementUnclear,
                                        fontFamily = PressStart2P,
                                        fontSize = 16.sp,
                                        color = Color(0xFFFFB74D)
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        text = strings.skipping,
                                        fontFamily = PressStart2P,
                                        fontSize = 10.sp,
                                        color = Color.White.copy(alpha = 0.7f)
                                    )
                                } else {
                                    if (uiState.stayedIdle) {
                                        Text(
                                            text = "IDLE",
                                            fontFamily = PressStart2P,
                                            fontSize = 28.sp,
                                            color = red
                                        )
                                        Spacer(Modifier.height(4.dp))
                                        Text(
                                            text = strings.youDidntMove,
                                            fontFamily = PressStart2P,
                                            fontSize = 10.sp,
                                            color = Color.White.copy(alpha = 0.8f)
                                        )
                                    } else {
                                        Text(
                                            text = strings.missed,
                                            fontFamily = PressStart2P,
                                            fontSize = 28.sp,
                                            color = red
                                        )
                                    }
                                    if (uiState.pendingGameOver) {
                                        Spacer(Modifier.height(8.dp))
                                        Text(
                                            text = strings.gameOver,
                                            fontFamily = PressStart2P,
                                            fontSize = 12.sp,
                                            color = red.copy(alpha = 0.8f)
                                        )
                                    }
                                }
                            }
                        }

                        SoloGamePhase.GAME_OVER -> {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                val infiniteTransition = rememberInfiniteTransition(label = "gameover")
                                val gameOverScale by infiniteTransition.animateFloat(
                                    initialValue = 1f,
                                    targetValue = 1.05f,
                                    animationSpec = infiniteRepeatable(
                                        animation = tween(800),
                                        repeatMode = RepeatMode.Reverse
                                    ),
                                    label = "gameOverScale"
                                )
                                Text(
                                    text = strings.gameOver,
                                    fontFamily = PressStart2P,
                                    fontSize = 20.sp,
                                    color = red,
                                    modifier = Modifier.scale(gameOverScale)
                                )
                                Spacer(Modifier.height(12.dp))
                                Text(
                                    text = "${strings.score}: ${uiState.score}",
                                    fontFamily = PressStart2P,
                                    fontSize = 16.sp,
                                    color = gold
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    if (uiState.phase == SoloGamePhase.COUNTDOWN) {
                        CountdownRing(
                            timeLeftSec = uiState.timeLeftSec,
                            totalSec = 5,
                            ringSize = 160.dp
                        )
                    }

                    if (uiState.phase == SoloGamePhase.RESULT &&
                        uiState.allScores.isNotEmpty() &&
                        !showFlyingScore &&
                        !showTrapAnimation &&
                        !animateScore) {
                        DebugProbabilitiesPanel(
                            allScores = uiState.allScores,
                            predictedMove = uiState.predictedMove,
                            expectedMove = uiState.expectedMove,
                            confidence = uiState.predictionConfidence
                        )
                    }
                }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    when (uiState.phase) {
                        SoloGamePhase.SHOWING_MOVE -> {
                            FigmaMenuButton(
                                label = strings.start,
                                font = PressStart2P,
                                onClick = onStart,
                                modifier = Modifier.height(72.dp)
                            )
                        }

                        SoloGamePhase.COUNTDOWN -> {
                            FigmaMenuButton(
                                label = "DONE!",
                                font = PressStart2P,
                                onClick = onDone,
                                modifier = Modifier.height(72.dp)
                            )
                        }

                        SoloGamePhase.EVALUATING -> {
                        }

                        SoloGamePhase.RETRY -> {
                            FigmaMenuButton(
                                label = strings.retry,
                                font = PressStart2P,
                                onClick = onStart,
                                modifier = Modifier.height(72.dp)
                            )
                        }

                        SoloGamePhase.RESULT -> {
                            if (!showFlyingScore && !showTrapAnimation) {
                                FigmaMenuButton(
                                    label = strings.continue_,
                                    font = PressStart2P,
                                    onClick = onContinue,
                                    modifier = Modifier.height(72.dp)
                                )
                            }
                        }

                        SoloGamePhase.GAME_OVER -> {
                            FigmaMenuButton(
                                label = strings.playAgain,
                                font = PressStart2P,
                                onClick = onRestart,
                                modifier = Modifier.height(72.dp)
                            )
                        }
                    }

                    if (uiState.phase != SoloGamePhase.COUNTDOWN && uiState.phase != SoloGamePhase.EVALUATING) {
                        FigmaMenuButton(
                            label = strings.quit,
                            font = PressStart2P,
                            onClick = onBack,
                            modifier = Modifier.height(56.dp)
                        )
                    }
                }
            }

            if (showFlyingScore && uiState.lastResultCorrect == true) {
                SuccessScoreAnimation(
                    show = true,
                    multiplier = uiState.multiplier,
                    pointsEarned = uiState.pointsEarned,
                    isNewMultiplier = uiState.newMultiplierReached,
                    onComplete = {
                        showFlyingScore = false
                        animateScore = true
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }

            if (showTrapAnimation && uiState.fellForTrap) {
                TrapScoreAnimation(
                    trigger = true,
                    pointsLost = uiState.pointsEarned,
                    onAnimationComplete = {
                        showTrapAnimation = false
                        animateScore = true
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }

            if (showHeartBreak) {
                HeartBreakAnimation(
                    onComplete = {
                        showHeartBreak = false
                        showCelebrationBg = false
                    }
                )
            }

            if (showTrophyNotification) {
                TrophyNotification(
                    trophyCount = newTrophyCount,
                    onComplete = { showTrophyNotification = false },
                    strings = strings
                )
            }

            if (showLivesNotification) {
                LivesRemainingNotification(
                    livesRemaining = livesRemaining,
                    onComplete = { showLivesNotification = false },
                    strings = strings
                )
            }
        }
    }

    LaunchedEffect(animateScore) {
        if (animateScore) {
            delay(200)
            displayedScore = uiState.score
            delay(600)
            animateScore = false
            showCelebrationBg = false
        }
    }
}

@Composable
private fun StatSimple(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontFamily = PressStart2P, fontSize = 9.sp, color = color)
        Spacer(Modifier.height(4.dp))
        Text(value, fontFamily = PressStart2P, fontSize = 12.sp, color = color)
    }
}

@Composable
fun AnimatedScoreStat(
    score: Float,
    isAnimating: Boolean,
    color: Color,
    strings: com.epfl.esl.simongame.data.Strings,
    modifier: Modifier = Modifier
) {
    val sparkleGold = Color(0xFFFFD700)
    val sparkleWhite = Color(0xFFFFFFFF)
    val sparkleOrange = Color(0xFFFF6B35)

    val scale by animateFloatAsState(
        targetValue = if (isAnimating) 2.5f else 1f,
        animationSpec = spring(dampingRatio = 0.4f, stiffness = 200f),
        label = "scoreScale"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "sparkles")

    val sparkle1Offset by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 60f,
        animationSpec = infiniteRepeatable(tween(400), RepeatMode.Restart),
        label = "s1"
    )
    val sparkle2Offset by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 50f,
        animationSpec = infiniteRepeatable(tween(350, delayMillis = 100), RepeatMode.Restart),
        label = "s2"
    )
    val sparkle3Offset by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 55f,
        animationSpec = infiniteRepeatable(tween(380, delayMillis = 50), RepeatMode.Restart),
        label = "s3"
    )
    val sparkleAlpha by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(400), RepeatMode.Restart),
        label = "sAlpha"
    )

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        if (isAnimating) {
            Canvas(modifier = Modifier.size(120.dp)) {
                val cx = size.width / 2
                val cy = size.height / 2
                drawCircle(sparkleGold, 6f * sparkleAlpha, Offset(cx + sparkle1Offset, cy - sparkle1Offset))
                drawCircle(sparkleWhite, 5f * sparkleAlpha, Offset(cx - sparkle2Offset, cy - sparkle2Offset * 0.8f))
                drawCircle(sparkleOrange, 4f * sparkleAlpha, Offset(cx + sparkle3Offset, cy))
                drawCircle(sparkleGold, 5f * sparkleAlpha, Offset(cx - sparkle1Offset, cy + sparkle1Offset * 0.3f))
                drawCircle(sparkleWhite, 4f * sparkleAlpha, Offset(cx + sparkle2Offset * 0.7f, cy + sparkle2Offset))
                drawCircle(sparkleOrange, 5f * sparkleAlpha, Offset(cx, cy - sparkle3Offset))
                drawCircle(sparkleGold, 4f * sparkleAlpha, Offset(cx - sparkle3Offset * 0.6f, cy + sparkle3Offset * 0.8f))
                drawCircle(sparkleWhite, 3f * sparkleAlpha, Offset(cx + sparkle1Offset * 0.5f, cy - sparkle1Offset * 1.2f))
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (!isAnimating) {
                Text(
                    text = strings.score,
                    fontFamily = PressStart2P,
                    fontSize = 9.sp,
                    color = color
                )
                Spacer(Modifier.height(4.dp))
            }
            Text(
                text = "%.1f".format(score),
                fontFamily = PressStart2P,
                fontSize = 12.sp,
                color = if (isAnimating) sparkleGold else color,
                modifier = Modifier.scale(scale)
            )
        }
    }
}

@Composable
fun ComboStat(
    multiplier: Int,
    strings: com.epfl.esl.simongame.data.Strings,
    modifier: Modifier = Modifier
) {
    val gray = Color(0xFF888888)

    var lastMultiplier by remember { mutableIntStateOf(multiplier) }
    var triggerPulse by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (triggerPulse) 1.5f else 1f,
        animationSpec = spring(dampingRatio = 0.3f, stiffness = 200f),
        label = "comboScale"
    )

    LaunchedEffect(multiplier) {
        if (multiplier != lastMultiplier && multiplier > 1) {
            triggerPulse = true
            delay(400)
            triggerPulse = false
            lastMultiplier = multiplier
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        if (multiplier > 1) {
            val comboColor = getMultiplierColor(multiplier)
            Text(
                text = strings.combo,
                fontFamily = PressStart2P,
                fontSize = 9.sp,
                color = comboColor
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "x$multiplier",
                fontFamily = PressStart2P,
                fontSize = 12.sp,
                color = comboColor,
                modifier = Modifier.scale(scale)
            )
        } else {
            Text(
                text = strings.noCombo,
                fontFamily = PressStart2P,
                fontSize = 9.sp,
                color = gray
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = strings.combo,
                fontFamily = PressStart2P,
                fontSize = 9.sp,
                color = gray
            )
        }
    }
}

@Composable
fun LivesStat(
    lives: Int,
    strings: com.epfl.esl.simongame.data.Strings,
    modifier: Modifier = Modifier
) {
    val red = Color(0xFFEF4444)
    val gold = Color(0xFFE8C46A)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Text(
            text = strings.lives,
            fontFamily = PressStart2P,
            fontSize = 9.sp,
            color = gold
        )
        Spacer(Modifier.height(4.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            repeat(lives.coerceAtLeast(0)) {
                Text(text = "♥", fontSize = 14.sp, color = red)
            }
        }
    }
}

@Composable
fun CountdownRing(
    timeLeftSec: Int,
    totalSec: Int,
    modifier: Modifier = Modifier,
    ringSize: Dp = 150.dp,
    strokeWidth: Dp = 12.dp,
    trackColor: Color = Color(0xFF0A0833).copy(alpha = 0.5f),
    progressColor: Color = Color(0xFF5FE3FF),
    textColor: Color = Color(0xFFE8C46A),
) {
    val rawProgress = (timeLeftSec.coerceIn(0, totalSec)).toFloat() / totalSec.toFloat()
    val progress = animateFloatAsState(targetValue = rawProgress, label = "ringProgress").value

    val pulseScale = if (timeLeftSec <= 2) {
        val infiniteTransition = rememberInfiniteTransition(label = "pulse")
        infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = 1.15f,
            animationSpec = infiniteRepeatable(
                animation = tween(150),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulseScale"
        ).value
    } else 1f

    Box(
        modifier = modifier
            .size(ringSize)
            .scale(pulseScale),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val stroke = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
            val diameter = size.minDimension
            val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
            val arcSize = Size(diameter, diameter)

            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = stroke
            )

            val ringColor = if (timeLeftSec <= 2) Color(0xFFFF6B6B) else progressColor

            drawArc(
                color = ringColor,
                startAngle = -90f,
                sweepAngle = 360f * progress,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = stroke
            )
        }

        Text(
            text = timeLeftSec.toString(),
            fontFamily = PressStart2P,
            fontSize = 32.sp,
            color = if (timeLeftSec <= 2) Color(0xFFFF6B6B) else textColor
        )
    }
}

@Composable
fun HeartBreakAnimation(
    onComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ashGray = Color(0xFF666666)

    val heartScale = remember { Animatable(1f) }
    val heartAlpha = remember { Animatable(1f) }
    val heartOffsetY = remember { Animatable(-200f) }

    data class AshParticle(val x: Float, val y: Float, val size: Float, val speed: Float)
    var ashParticles by remember { mutableStateOf(listOf<AshParticle>()) }
    val ashFallProgress = remember { Animatable(0f) }

    var phase by remember { mutableIntStateOf(0) }

    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        scope.launch { heartScale.animateTo(3f, tween(400, easing = FastOutSlowInEasing)) }
        heartOffsetY.animateTo(0f, tween(400, easing = FastOutSlowInEasing))

        delay(500)

        phase = 1
        delay(400)

        phase = 2

        ashParticles = (0..15).map {
            AshParticle(
                x = (-30..30).random().toFloat(),
                y = (-20..20).random().toFloat(),
                size = (3..8).random().toFloat(),
                speed = 0.8f + kotlin.random.Random.nextFloat() * 0.7f

            )
        }

        scope.launch { heartAlpha.animateTo(0f, tween(300)) }
        ashFallProgress.animateTo(1f, tween(800, easing = FastOutSlowInEasing))

        delay(200)
        onComplete()
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        if (phase == 0 || phase == 1) {
            Text(
                text = "💔",
                fontSize = 44.sp,
                modifier = Modifier
                    .offset(y = heartOffsetY.value.dp)
                    .scale(heartScale.value)
                    .graphicsLayer { alpha = heartAlpha.value }
            )
        }

        if (phase == 2) {
            Canvas(
                modifier = Modifier.size(200.dp)
            ) {
                val cx = size.width / 2
                val cy = size.height / 2

                ashParticles.forEach { particle ->
                    val fallDistance = 400f * ashFallProgress.value * particle.speed
                    val fadeOut = 1f - ashFallProgress.value

                    drawCircle(
                        color = ashGray.copy(alpha = fadeOut * 0.8f),
                        radius = particle.size,
                        center = Offset(
                            cx + particle.x + (particle.x * 0.5f * ashFallProgress.value),
                            cy + particle.y + fallDistance
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun TrophyNotification(
    trophyCount: Int,
    onComplete: () -> Unit,
    strings: com.epfl.esl.simongame.data.Strings,
    modifier: Modifier = Modifier
) {
    val gold = Color(0xFFE8C46A)

    val alpha = remember { Animatable(0f) }
    val scale = remember { Animatable(0.5f) }

    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        scope.launch { alpha.animateTo(1f, tween(200)) }
        scale.animateTo(1.2f, spring(dampingRatio = 0.5f, stiffness = 300f))

        delay(1000)

        scope.launch { alpha.animateTo(0f, tween(300)) }
        scale.animateTo(0.8f, tween(300))

        delay(100)
        onComplete()
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .scale(scale.value)
                .graphicsLayer { this.alpha = alpha.value }
                .background(Color(0xFF15104A).copy(alpha = 0.9f), RoundedCornerShape(16.dp))
                .border(2.dp, gold, RoundedCornerShape(16.dp))
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Text(
                text = "🏆",
                fontSize = 32.sp
            )
            Text(
                text = strings.trophyPlus,
                fontFamily = PressStart2P,
                fontSize = 16.sp,
                color = gold
            )
        }
    }
}

@Composable
fun LivesRemainingNotification(
    livesRemaining: Int,
    onComplete: () -> Unit,
    strings: com.epfl.esl.simongame.data.Strings,
    modifier: Modifier = Modifier
) {
    val red = Color(0xFFEF4444)

    val offsetY = remember { Animatable(-100f) }
    val alpha = remember { Animatable(0f) }

    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        scope.launch { offsetY.animateTo(0f, tween(300, easing = FastOutSlowInEasing)) }
        alpha.animateTo(1f, tween(200))

        delay(1500)

        scope.launch { alpha.animateTo(0f, tween(300)) }
        offsetY.animateTo(-50f, tween(300))

        delay(100)
        onComplete()
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .padding(bottom = 200.dp)
                .offset(y = offsetY.value.dp)
                .graphicsLayer { this.alpha = alpha.value }
                .background(Color(0xFF15104A).copy(alpha = 0.9f), RoundedCornerShape(12.dp))
                .border(2.dp, red, RoundedCornerShape(12.dp))
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Text(
                text = strings.livesRemaining,
                fontFamily = PressStart2P,
                fontSize = 10.sp,
                color = red
            )
            repeat(livesRemaining.coerceAtLeast(0)) {
                Text(
                    text = "♥",
                    fontSize = 16.sp,
                    color = red
                )
            }
        }
    }
}

@Composable
fun DebugProbabilitiesPanel(
    allScores: Map<Move, Float>,
    predictedMove: Move?,
    expectedMove: Move,
    confidence: Float,
    modifier: Modifier = Modifier
) {
    val gold = Color(0xFFE8C46A)
    val cyan = Color(0xFF5FE3FF)
    val green = Color(0xFF4ADE80)
    val red = Color(0xFFEF4444)
    val orange = Color(0xFFFF9500)
    val deep = Color(0xFF0A0833)

    val sortedScores = allScores.entries.sortedByDescending { it.value }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .background(deep.copy(alpha = 0.9f), RoundedCornerShape(12.dp))
            .border(2.dp, cyan.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "DEBUG PROBAS",
            fontFamily = PressStart2P,
            fontSize = 8.sp,
            color = cyan
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Expected: ${expectedMove.name}",
            fontFamily = PressStart2P,
            fontSize = 7.sp,
            color = gold.copy(alpha = 0.7f)
        )

        Spacer(Modifier.height(8.dp))

        sortedScores.forEach { (move, score) ->
            val isExpected = move == expectedMove
            val isPredicted = move == predictedMove
            val barColor = when {
                isExpected && isPredicted -> green
                isPredicted && !isExpected -> red
                isExpected -> orange
                else -> gold.copy(alpha = 0.5f)
            }
            val textColor = when {
                isExpected && isPredicted -> green
                isPredicted -> red
                isExpected -> orange
                else -> gold.copy(alpha = 0.7f)
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val shortName = move.name.take(6).uppercase()
                Text(
                    text = shortName,
                    fontFamily = PressStart2P,
                    fontSize = 6.sp,
                    color = textColor,
                    modifier = Modifier.width(45.dp)
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(12.dp)
                        .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(fraction = score.coerceIn(0f, 1f))
                            .background(barColor, RoundedCornerShape(4.dp))
                    )
                }

                Text(
                    text = "${(score * 100).toInt()}%",
                    fontFamily = PressStart2P,
                    fontSize = 6.sp,
                    color = textColor,
                    modifier = Modifier.width(30.dp),
                    textAlign = TextAlign.End
                )

                if (isPredicted) {
                    Text(text = "←", fontSize = 10.sp, color = if (isExpected) green else red)
                }
            }
        }

        Spacer(Modifier.height(4.dp))

        val isCorrect = predictedMove == expectedMove
        Text(
            text = if (isCorrect) "✓ CORRECT" else "✗ WRONG",
            fontFamily = PressStart2P,
            fontSize = 7.sp,
            color = if (isCorrect) green else red
        )
    }
}