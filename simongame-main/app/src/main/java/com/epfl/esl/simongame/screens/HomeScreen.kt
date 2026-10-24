// HomeScreen.kt
package com.epfl.esl.simongame.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.Font
import com.epfl.esl.simongame.R
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale


val PressStart2P = FontFamily(
    Font(R.font.press_start)
)

@Composable
fun HomeTitle(modifier: Modifier = Modifier) {
    val bungee = FontFamily(Font(R.font.bungee_simon))

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
                fontSize = 64.sp,
                color = outline,
                style = TextStyle(drawStyle = Stroke(width = 12f))
            )
            Text(
                text = text,
                fontFamily = bungee,
                fontSize = 64.sp,
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
        OutlinedTitle("SIMON", fill = Color(0xFFE8C46A))
        Spacer(Modifier.height(6.dp))
        OutlinedTitle("SAYS", fill = Color(0xFF5FE3FF))
    }
}

@Composable
fun FigmaMenuButton(
    label: String,
    font: FontFamily,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(18.dp)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(72.dp)
            .clip(shape)
            .clickable(onClick = onClick),
        shape = shape,
        color = Color.Transparent,
        border = BorderStroke(2.dp, Color(0xFFD6B15D))
    ) {
        Box {
            Image(
                painter = painterResource(R.drawable.btn_background),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            Text(
                text = label,
                fontFamily = font,
                fontSize = 18.sp,
                color = Color(0xFFE8C46A),
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}

@Composable
fun HomeScreen(
    onPlaySoloClick: () -> Unit,
    onPlayVersusClick: () -> Unit,
    onProfileClick: () -> Unit,
    onSettingsClick: () -> Unit = {}
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {

        val diamondShift = 50.dp
        val buttonsShift = 60.dp

        val titleY = maxHeight * 0.08f
        val kidsY = maxHeight * 0.25f
        val diamondY = kidsY + diamondShift
        val buttonsY = maxHeight * 0.55f + buttonsShift

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

        Text(
            text = "\u2699",
            fontSize = 28.sp,
            color = Color(0xFFE8C46A),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
                .clickable { onSettingsClick() }
        )

        HomeTitle(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = titleY)
        )

        Image(
            painter = painterResource(R.drawable.logo_croped),
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .scale(1.4f)
                .offset(y = kidsY),
            contentScale = ContentScale.Fit
        )

        Image(
            painter = painterResource(R.drawable.diamond),
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = diamondY),
            contentScale = ContentScale.Fit
        )

        Column(Modifier
            .align(Alignment.TopCenter)
            .offset(y = buttonsY)
            .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally) {
            FigmaMenuButton("SOLO", PressStart2P, onClick = onPlaySoloClick)
            Spacer(Modifier.height(16.dp))
            FigmaMenuButton("MULTIPLAYER", PressStart2P, onClick = onPlayVersusClick)
            Spacer(Modifier.height(16.dp))
            FigmaMenuButton("MY PROFILE", PressStart2P, onClick = onProfileClick)
        }
    }
}

