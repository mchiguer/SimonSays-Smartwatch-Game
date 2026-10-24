package com.epfl.esl.simongame.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun MegaScoreAnimation(
    trigger: Boolean,
    multiplier: Int,
    pointsEarned: Float,
    onAnimationComplete: () -> Unit = {},
    modifier: Modifier = Modifier
) {

    var showInitialPlus by remember { mutableStateOf(false) }
    var showComboBox by remember { mutableStateOf(false) }
    var showFinalPoints by remember { mutableStateOf(false) }
    var displayedPoints by remember { mutableFloatStateOf(1f) }
    var isFlying by remember { mutableStateOf(false) }

    val initialPlusScale = remember { Animatable(0f) }
    val initialPlusAlpha = remember { Animatable(0f) }

    val comboBoxScale = remember { Animatable(0f) }
    val comboBoxAlpha = remember { Animatable(0f) }
    val comboBoxRotation = remember { Animatable(0f) }

    val finalPointsScale = remember { Animatable(0f) }
    val finalPointsAlpha = remember { Animatable(1f) }
    val finalPointsOffsetY = remember { Animatable(0f) }
    val finalPointsOffsetX = remember { Animatable(0f) }

    val multiplierColor = getMultiplierColor(multiplier)
    val scope = rememberCoroutineScope()

    LaunchedEffect(trigger) {
        if (trigger) {
            displayedPoints = 1f
            showInitialPlus = false
            showComboBox = false
            showFinalPoints = false
            isFlying = false

            initialPlusScale.snapTo(0f)
            initialPlusAlpha.snapTo(0f)
            comboBoxScale.snapTo(0f)
            comboBoxAlpha.snapTo(0f)
            comboBoxRotation.snapTo(-5f)
            finalPointsScale.snapTo(0f)
            finalPointsAlpha.snapTo(1f)
            finalPointsOffsetY.snapTo(0f)
            finalPointsOffsetX.snapTo(0f)

            showInitialPlus = true
            initialPlusAlpha.animateTo(1f, tween(100))
            initialPlusScale.animateTo(1.3f, spring(dampingRatio = 0.4f, stiffness = 300f))
            initialPlusScale.animateTo(1f, tween(100))

            delay(400)

            initialPlusScale.animateTo(0.5f, tween(150))
            initialPlusAlpha.animateTo(0f, tween(150))
            showInitialPlus = false

            delay(100)

            if (multiplier > 1) {
                showComboBox = true
                comboBoxAlpha.animateTo(1f, tween(100))

                scope.launch {
                    comboBoxRotation.animateTo(5f, tween(100))
                    comboBoxRotation.animateTo(-3f, tween(100))
                    comboBoxRotation.animateTo(2f, tween(100))
                    comboBoxRotation.animateTo(0f, tween(100))
                }

                comboBoxScale.animateTo(1.2f, spring(dampingRatio = 0.3f, stiffness = 200f))
                comboBoxScale.animateTo(1f, tween(150))

                repeat(2) {
                    comboBoxScale.animateTo(1.1f, tween(200))
                    comboBoxScale.animateTo(1f, tween(200))
                }

                delay(200)

                comboBoxScale.animateTo(0.8f, tween(150))
                comboBoxAlpha.animateTo(0f, tween(150))
                showComboBox = false

                delay(100)
            }

            showFinalPoints = true
            finalPointsAlpha.snapTo(1f)
            finalPointsScale.animateTo(1f, spring(dampingRatio = 0.5f, stiffness = 300f))

            val steps = 10
            val stepSize = pointsEarned / steps
            for (i in 1..steps) {
                displayedPoints = minOf(stepSize * i, pointsEarned)
                finalPointsScale.animateTo(1.4f, tween(40))
                finalPointsScale.animateTo(1f, tween(40))
                if (i < steps) delay(80)
            }
            displayedPoints = pointsEarned

            delay(300)

            isFlying = true

            scope.launch {
                repeat(8) {
                    finalPointsOffsetX.animateTo(20f, tween(40))
                    finalPointsOffsetX.animateTo(-20f, tween(40))
                }
            }

            scope.launch {
                finalPointsScale.animateTo(0.5f, tween(500))
            }

            scope.launch {
                delay(300)
                finalPointsAlpha.animateTo(0f, tween(200))
            }

            finalPointsOffsetY.animateTo(-400f, tween(500, easing = FastOutSlowInEasing))

            showFinalPoints = false
            onAnimationComplete()
        }
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        if (showInitialPlus) {
            Box(
                modifier = Modifier
                    .scale(initialPlusScale.value)
                    .alpha(initialPlusAlpha.value)
                    .size(140.dp)
                    .background(Color(0xFF0A0833).copy(alpha = 0.9f), CircleShape)
                    .border(4.dp, Color(0xFF4ADE80), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "+1",
                    fontFamily = PressStart2P,
                    fontSize = 36.sp,
                    color = Color(0xFF4ADE80)
                )
            }
        }

        if (showComboBox && multiplier > 1) {
            Box(
                modifier = Modifier
                    .scale(comboBoxScale.value)
                    .alpha(comboBoxAlpha.value)
                    .rotate(comboBoxRotation.value)
                    .background(Color(0xFF0A0833).copy(alpha = 0.95f), RoundedCornerShape(16.dp))
                    .border(4.dp, multiplierColor, RoundedCornerShape(16.dp))
                    .padding(horizontal = 32.dp, vertical = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "COMBO",
                        fontFamily = PressStart2P,
                        fontSize = 14.sp,
                        color = Color(0xFFE8C46A)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "x$multiplier",
                        fontFamily = PressStart2P,
                        fontSize = 40.sp,
                        color = multiplierColor
                    )
                }
            }
        }

        if (showFinalPoints) {
            Text(
                text = "+${"%.1f".format(displayedPoints)}",
                fontFamily = PressStart2P,
                fontSize = 44.sp,
                color = if (multiplier > 1) multiplierColor else Color(0xFF4ADE80),
                modifier = Modifier
                    .offset {
                        IntOffset(
                            finalPointsOffsetX.value.roundToInt(),
                            finalPointsOffsetY.value.roundToInt()
                        )
                    }
                    .scale(finalPointsScale.value)
                    .alpha(finalPointsAlpha.value)
            )
        }
    }
}

@Composable
fun TrapScoreAnimation(
    trigger: Boolean,
    pointsLost: Float,
    onAnimationComplete: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val orange = Color(0xFFFF9500)
    val red = Color(0xFFEF4444)

    var showMinusOne by remember { mutableStateOf(false) }

    val minusScale = remember { Animatable(0f) }
    val minusAlpha = remember { Animatable(0f) }
    val minusOffsetY = remember { Animatable(0f) }
    val minusOffsetX = remember { Animatable(0f) }
    val minusRotation = remember { Animatable(0f) }

    val scope = rememberCoroutineScope()

    LaunchedEffect(trigger) {
        if (trigger) {
            showMinusOne = false
            minusScale.snapTo(0f)
            minusAlpha.snapTo(0f)
            minusOffsetY.snapTo(0f)
            minusOffsetX.snapTo(0f)
            minusRotation.snapTo(0f)

            if (pointsLost < 0) {
                showMinusOne = true
                minusAlpha.animateTo(1f, tween(100))
                minusScale.animateTo(1.5f, spring(dampingRatio = 0.3f, stiffness = 400f))

                scope.launch {
                    repeat(4) {
                        minusOffsetX.animateTo(15f, tween(40))
                        minusOffsetX.animateTo(-15f, tween(40))
                    }
                    minusOffsetX.animateTo(0f, tween(40))
                }
                scope.launch {
                    minusRotation.animateTo(5f, tween(50))
                    minusRotation.animateTo(-5f, tween(50))
                    minusRotation.animateTo(3f, tween(50))
                    minusRotation.animateTo(-3f, tween(50))
                    minusRotation.animateTo(0f, tween(50))
                }

                delay(600)

                scope.launch {
                    minusScale.animateTo(0.6f, tween(400))
                }
                scope.launch {
                    delay(200)
                    minusAlpha.animateTo(0f, tween(200))
                }

                minusOffsetY.animateTo(-350f, tween(400, easing = FastOutSlowInEasing))

                showMinusOne = false
            }

            onAnimationComplete()
        }
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        if (showMinusOne) {
            Text(
                text = "-1",
                fontFamily = PressStart2P,
                fontSize = 48.sp,
                color = orange,
                modifier = Modifier
                    .offset {
                        IntOffset(
                            minusOffsetX.value.roundToInt(),
                            minusOffsetY.value.roundToInt()
                        )
                    }
                    .scale(minusScale.value)
                    .alpha(minusAlpha.value)
                    .rotate(minusRotation.value)
            )
        }
    }
}

@Composable
fun SuccessScoreAnimation(
    show: Boolean,
    multiplier: Int,
    pointsEarned: Float,
    isNewMultiplier: Boolean,
    onComplete: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var triggerAnimation by remember { mutableStateOf(false) }
    var showConfetti by remember { mutableStateOf(false) }

    LaunchedEffect(show) {
        if (show) {
            triggerAnimation = true
            showConfetti = true
        } else {
            triggerAnimation = false
            showConfetti = false
        }
    }

    Box(modifier = modifier) {
        if (showConfetti) {
            ConfettiExplosion(
                trigger = showConfetti,
                particleCount = when {
                    multiplier >= 5 -> 150
                    multiplier >= 4 -> 120
                    multiplier >= 3 -> 80
                    multiplier >= 2 -> 50
                    else -> 30
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        SuccessBurst(
            show = show,
            modifier = Modifier.fillMaxSize()
        )

        MegaScoreAnimation(
            trigger = triggerAnimation,
            multiplier = multiplier,
            pointsEarned = pointsEarned,
            onAnimationComplete = onComplete,
            modifier = Modifier.fillMaxSize()
        )
    }
}