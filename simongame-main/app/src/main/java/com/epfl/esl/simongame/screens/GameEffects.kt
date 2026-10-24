package com.epfl.esl.simongame.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

data class ConfettiParticle(
    var x: Float,
    var y: Float,
    var velocityX: Float,
    var velocityY: Float,
    var rotation: Float,
    var rotationSpeed: Float,
    var color: Color,
    var size: Float,
    var alpha: Float = 1f
)

@Composable
fun ConfettiExplosion(
    trigger: Boolean,
    modifier: Modifier = Modifier,
    particleCount: Int = 80,
    colors: List<Color> = listOf(
        Color(0xFFFF6B6B),
        Color(0xFF4ADE80),
        Color(0xFF60A5FA),
        Color(0xFFE8C46A),
        Color(0xFFA855F7),
        Color(0xFFF472B6),
        Color(0xFF5FE3FF),
    )
) {
    var particles by remember { mutableStateOf<List<ConfettiParticle>>(emptyList()) }
    var isAnimating by remember { mutableStateOf(false) }

    LaunchedEffect(trigger) {
        if (trigger && !isAnimating) {
            isAnimating = true
            particles = List(particleCount) {
                val angle = Random.nextFloat() * 2 * Math.PI.toFloat()
                val speed = Random.nextFloat() * 15f + 8f
                ConfettiParticle(
                    x = 0.5f,
                    y = 0.5f,
                    velocityX = cos(angle) * speed * (if (Random.nextBoolean()) 1f else -1f),
                    velocityY = sin(angle) * speed - Random.nextFloat() * 10f - 5f,  // Bias upward
                    rotation = Random.nextFloat() * 360f,
                    rotationSpeed = Random.nextFloat() * 20f - 10f,
                    color = colors.random(),
                    size = Random.nextFloat() * 12f + 6f
                )
            }

            repeat(120) {
                delay(16)
                particles = particles.map { p ->
                    p.copy(
                        x = p.x + p.velocityX * 0.003f,
                        y = p.y + p.velocityY * 0.003f,
                        velocityY = p.velocityY + 0.5f,
                        rotation = p.rotation + p.rotationSpeed,
                        alpha = (p.alpha - 0.008f).coerceAtLeast(0f)
                    )
                }.filter { it.alpha > 0f }
            }
            particles = emptyList()
            isAnimating = false
        }
    }

    if (particles.isNotEmpty()) {
        Canvas(modifier = modifier.fillMaxSize()) {
            particles.forEach { particle ->
                drawConfettiParticle(particle, size.width, size.height)
            }
        }
    }
}

private fun DrawScope.drawConfettiParticle(
    particle: ConfettiParticle,
    canvasWidth: Float,
    canvasHeight: Float
) {
    val x = particle.x * canvasWidth
    val y = particle.y * canvasHeight

    drawCircle(
        color = particle.color.copy(alpha = particle.alpha),
        radius = particle.size,
        center = Offset(x, y)
    )
}

@Composable
fun FloatingParticles(
    modifier: Modifier = Modifier,
    particleCount: Int = 20,
    color: Color = Color(0xFF5FE3FF)
) {
    val infiniteTransition = rememberInfiniteTransition(label = "particles")

    val particles = remember {
        List(particleCount) {
            FloatingParticleData(
                x = Random.nextFloat(),
                y = Random.nextFloat(),
                size = Random.nextFloat() * 4f + 2f,
                speed = Random.nextFloat() * 0.5f + 0.2f,
                alpha = Random.nextFloat() * 0.3f + 0.1f
            )
        }
    }

    val time by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 100000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "time"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        particles.forEach { p ->
            val yOffset = (p.y + time * p.speed * 0.001f) % 1.2f - 0.1f
            val xWobble = sin(time * 0.01f + p.x * 10f) * 0.02f

            drawCircle(
                color = color.copy(alpha = p.alpha),
                radius = p.size,
                center = Offset(
                    (p.x + xWobble) * size.width,
                    yOffset * size.height
                )
            )
        }
    }
}

private data class FloatingParticleData(
    val x: Float,
    val y: Float,
    val size: Float,
    val speed: Float,
    val alpha: Float
)

@Composable
fun AnimatedScoreCounter(
    targetScore: Int,
    modifier: Modifier = Modifier,
    durationMs: Int = 1500,
    textColor: Color = Color(0xFFE8C46A),
    fontSize: TextUnit = 48.sp
) {
    var displayedScore by remember { mutableIntStateOf(0) }

    LaunchedEffect(targetScore) {
        if (targetScore > 0) {
            val startTime = System.currentTimeMillis()
            while (displayedScore < targetScore) {
                val elapsed = System.currentTimeMillis() - startTime
                val progress = (elapsed.toFloat() / durationMs).coerceIn(0f, 1f)
                val easedProgress = 1f - (1f - progress) * (1f - progress)
                displayedScore = (targetScore * easedProgress).toInt()
                delay(16)
            }
            displayedScore = targetScore
        }
    }

    Text(
        text = displayedScore.toString(),
        fontFamily = PressStart2P,
        fontSize = fontSize,
        color = textColor,
        modifier = modifier
    )
}

@Composable
fun PulsingGlowBox(
    color: Color,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "glow")

    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Box(modifier = modifier.scale(pulseScale)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = glowAlpha }
                .background(color.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
        )
        content()
    }
}

@Composable
fun SuccessBurst(
    show: Boolean,
    modifier: Modifier = Modifier
) {
    var rings by remember { mutableStateOf<List<RingData>>(emptyList()) }

    LaunchedEffect(show) {
        if (show) {
            rings = listOf(
                RingData(0f, 1f),
                RingData(0f, 1f),
                RingData(0f, 1f)
            )
            repeat(60) { frame ->
                delay(16)
                rings = rings.mapIndexed { index, ring ->
                    val delay = index * 8
                    if (frame > delay) {
                        val progress = ((frame - delay).toFloat() / 40f).coerceIn(0f, 1f)
                        ring.copy(
                            radius = progress * 2f,
                            alpha = (1f - progress).coerceAtLeast(0f)
                        )
                    } else ring
                }
            }
            rings = emptyList()
        }
    }

    if (rings.isNotEmpty()) {
        Canvas(modifier = modifier.fillMaxSize()) {
            val centerX = size.width / 2
            val centerY = size.height / 2
            val maxRadius = size.minDimension / 2

            rings.forEach { ring ->
                drawCircle(
                    color = Color(0xFF4ADE80).copy(alpha = ring.alpha * 0.6f),
                    radius = ring.radius * maxRadius,
                    center = Offset(centerX, centerY),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4f)
                )
            }
        }
    }
}

private data class RingData(
    val radius: Float,
    val alpha: Float
)

@Composable
fun ScreenFlash(
    trigger: Boolean,
    color: Color = Color.White,
    modifier: Modifier = Modifier
) {
    var flashAlpha by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(trigger) {
        if (trigger) {
            flashAlpha = 0.6f
            repeat(10) {
                delay(30)
                flashAlpha = (flashAlpha - 0.06f).coerceAtLeast(0f)
            }
            flashAlpha = 0f
        }
    }

    if (flashAlpha > 0f) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(color.copy(alpha = flashAlpha))
        )
    }
}

data class SparkleParticle(
    var x: Float,
    var y: Float,
    var velocityX: Float,
    var velocityY: Float,
    var rotation: Float,
    var scale: Float,
    var alpha: Float,
    var color: Color
)

@Composable
fun SparkleExplosion(
    trigger: Boolean,
    modifier: Modifier = Modifier,
    particleCount: Int = 30,
    colors: List<Color> = listOf(
        Color(0xFFFFD700),
        Color(0xFFFFFFFF),
        Color(0xFFFFA500),
        Color(0xFF5FE3FF),
    )
) {
    var particles by remember { mutableStateOf<List<SparkleParticle>>(emptyList()) }
    var isAnimating by remember { mutableStateOf(false) }

    LaunchedEffect(trigger) {
        if (trigger && !isAnimating) {
            isAnimating = true
            particles = List(particleCount) {
                val angle = Random.nextFloat() * 2 * Math.PI.toFloat()
                val speed = Random.nextFloat() * 12f + 6f
                SparkleParticle(
                    x = 0.5f,
                    y = 0.5f,
                    velocityX = cos(angle) * speed,
                    velocityY = sin(angle) * speed,
                    rotation = Random.nextFloat() * 360f,
                    scale = Random.nextFloat() * 0.5f + 0.5f,
                    alpha = 1f,
                    color = colors.random()
                )
            }

            repeat(60) {
                delay(16)
                particles = particles.map { p ->
                    p.copy(
                        x = p.x + p.velocityX * 0.004f,
                        y = p.y + p.velocityY * 0.004f,
                        rotation = p.rotation + 15f,
                        scale = p.scale * 0.97f,
                        alpha = (p.alpha - 0.015f).coerceAtLeast(0f)
                    )
                }.filter { it.alpha > 0f }
            }
            particles = emptyList()
            isAnimating = false
        }
    }

    if (particles.isNotEmpty()) {
        Canvas(modifier = modifier.fillMaxSize()) {
            particles.forEach { p ->
                val x = p.x * size.width
                val y = p.y * size.height

                drawStar(
                    center = Offset(x, y),
                    radius = 12f * p.scale,
                    color = p.color.copy(alpha = p.alpha),
                    rotation = p.rotation
                )
            }
        }
    }
}

private fun DrawScope.drawStar(
    center: Offset,
    radius: Float,
    color: Color,
    rotation: Float
) {
    val points = 4
    for (i in 0 until points) {
        val angle = Math.toRadians((rotation + i * 90.0)).toFloat()
        val endX = center.x + cos(angle) * radius
        val endY = center.y + sin(angle) * radius
        drawLine(
            color = color,
            start = center,
            end = Offset(endX, endY),
            strokeWidth = 3f
        )
    }
    drawCircle(
        color = color,
        radius = radius * 0.3f,
        center = center
    )
}

@Composable
fun EnergyWave(
    trigger: Boolean,
    color: Color = Color(0xFF5FE3FF),
    modifier: Modifier = Modifier
) {
    var waves by remember { mutableStateOf<List<WaveData>>(emptyList()) }

    LaunchedEffect(trigger) {
        if (trigger) {
            waves = listOf(
                WaveData(0f, 1f, 6f),
                WaveData(0f, 1f, 4f),
                WaveData(0f, 1f, 2f)
            )

            repeat(50) { frame ->
                delay(16)
                waves = waves.mapIndexed { index, wave ->
                    val delayFrames = index * 6
                    if (frame > delayFrames) {
                        val progress = ((frame - delayFrames).toFloat() / 35f).coerceIn(0f, 1f)
                        wave.copy(
                            radius = progress * 1.5f,
                            alpha = (1f - progress).coerceAtLeast(0f),
                            strokeWidth = wave.strokeWidth * (1f - progress * 0.5f)
                        )
                    } else wave
                }
            }
            waves = emptyList()
        }
    }

    if (waves.isNotEmpty()) {
        Canvas(modifier = modifier.fillMaxSize()) {
            val centerX = size.width / 2
            val centerY = size.height / 2
            val maxRadius = size.minDimension / 2

            waves.forEach { wave ->
                drawCircle(
                    color = color.copy(alpha = wave.alpha * 0.8f),
                    radius = wave.radius * maxRadius,
                    center = Offset(centerX, centerY),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = wave.strokeWidth)
                )
            }
        }
    }
}

private data class WaveData(
    val radius: Float,
    val alpha: Float,
    val strokeWidth: Float
)

@Composable
fun RainbowGlowText(
    text: String,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 32.sp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "rainbow")

    val hueShift by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "hue"
    )

    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    val color = Color.hsv(hueShift, 0.8f, 1f)

    Box(modifier = modifier, contentAlignment = androidx.compose.ui.Alignment.Center) {
        Text(
            text = text,
            fontFamily = PressStart2P,
            fontSize = fontSize,
            color = color.copy(alpha = 0.4f),
            modifier = Modifier
                .scale(scale * 1.1f)
                .graphicsLayer { alpha = 0.6f }
        )
        Text(
            text = text,
            fontFamily = PressStart2P,
            fontSize = fontSize,
            color = color,
            modifier = Modifier.scale(scale)
        )
    }
}

@Composable
fun ElectricBorder(
    modifier: Modifier = Modifier,
    color: Color = Color(0xFF5FE3FF)
) {
    val infiniteTransition = rememberInfiniteTransition(label = "electric")

    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val segments = 20
        val segmentLength = size.width / segments

        for (i in 0 until segments) {
            val alpha = ((sin((phase + i * 0.3f) * Math.PI * 2) + 1f) / 2f).toFloat()
            val offset = sin((phase + i * 0.5f) * Math.PI * 4).toFloat() * 3f

            drawLine(
                color = color.copy(alpha = alpha * 0.8f),
                start = Offset(i * segmentLength, offset),
                end = Offset((i + 1) * segmentLength, offset),
                strokeWidth = 2f + alpha * 2f
            )

            drawLine(
                color = color.copy(alpha = alpha * 0.8f),
                start = Offset(i * segmentLength, size.height + offset),
                end = Offset((i + 1) * segmentLength, size.height + offset),
                strokeWidth = 2f + alpha * 2f
            )
        }
    }
}