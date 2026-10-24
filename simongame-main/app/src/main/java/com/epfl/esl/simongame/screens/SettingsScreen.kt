package com.epfl.esl.simongame.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.epfl.esl.simongame.R
import com.epfl.esl.simongame.data.AppLanguage
import com.epfl.esl.simongame.data.AppStrings
import com.epfl.esl.simongame.data.GameSettings

@Composable
fun SettingsScreen(
    settings: GameSettings,
    onBack: () -> Unit,
    onLanguageSelected: (AppLanguage) -> Unit
) {
    val strings = AppStrings.get(settings.language)
    val gold = Color(0xFFE8C46A)
    val cyan = Color(0xFF5FE3FF)
    val deep = Color(0xFF0A0833)
    val panel = RoundedCornerShape(18.dp)

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
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = strings.back,
                    fontFamily = PressStart2P,
                    fontSize = 12.sp,
                    color = gold,
                    modifier = Modifier.clickable { onBack() }
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = strings.settings,
                    fontFamily = PressStart2P,
                    fontSize = 12.sp,
                    color = cyan
                )
            }

            Spacer(Modifier.height(30.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(deep.copy(alpha = 0.7f), panel)
                    .border(2.dp, gold, panel)
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = strings.language,
                    fontFamily = PressStart2P,
                    fontSize = 14.sp,
                    color = gold
                )

                Spacer(Modifier.height(8.dp))

                AppLanguage.entries.forEach { lang ->
                    val isSelected = settings.language == lang
                    val bgColor = if (isSelected) cyan.copy(alpha = 0.3f) else Color.Transparent
                    val borderColor = if (isSelected) cyan else gold.copy(alpha = 0.5f)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(bgColor, RoundedCornerShape(12.dp))
                            .border(2.dp, borderColor, RoundedCornerShape(12.dp))
                            .clickable { onLanguageSelected(lang) }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = lang.flag,
                            fontSize = 24.sp
                        )
                        Text(
                            text = lang.displayName,
                            fontFamily = PressStart2P,
                            fontSize = 12.sp,
                            color = if (isSelected) cyan else gold
                        )
                        if (isSelected) {
                            Spacer(Modifier.weight(1f))
                            Text(
                                text = "✓",
                                fontFamily = PressStart2P,
                                fontSize = 14.sp,
                                color = cyan
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.weight(1f))
        }
    }
}
