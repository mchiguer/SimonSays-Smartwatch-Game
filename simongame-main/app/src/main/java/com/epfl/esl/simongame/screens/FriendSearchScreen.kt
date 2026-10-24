package com.epfl.esl.simongame.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.epfl.esl.simongame.R
import com.epfl.esl.simongame.data.UserProfile
import com.epfl.esl.simongame.friends.FriendSearchUiState

@Composable
private fun FriendSearchTitle(modifier: Modifier = Modifier) {
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
        OutlinedTitle("FIND", fill = Color(0xFFE8C46A))
        Spacer(Modifier.height(4.dp))
        OutlinedTitle("FRIENDS", fill = Color(0xFF5FE3FF))
    }
}

@Composable
private fun SmallFigmaButton(
    label: String,
    font: FontFamily,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(16.dp)

    Surface(
        modifier = modifier
            .height(46.dp)
            .clip(shape)
            .clickable(onClick = onClick),
        shape = shape,
        color = Color.Transparent,
        border = BorderStroke(2.dp, Color(0xFFD6B15D))
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(R.drawable.btn_background),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            Text(
                text = label,
                fontFamily = font,
                fontSize = 14.sp,
                color = Color(0xFFE8C46A),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun FriendCard(
    user: UserProfile,
    onAdd: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(18.dp)
    val gold = Color(0xFFE8C46A)
    val cyan = Color(0xFF5FE3FF)

    Surface(
        modifier = modifier.fillMaxWidth(),
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

            Row(
                modifier = Modifier
                    .padding(horizontal = 14.dp, vertical = 12.dp)
                    .fillMaxWidth(),
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
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "BEST: ${user.bestScore}",
                            fontFamily = PressStart2P,
                            fontSize = 8.sp,
                            color = cyan
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "🏆", fontSize = 12.sp)
                            Spacer(Modifier.width(2.dp))
                            Text(
                                text = "${user.totalTrophies}",
                                fontFamily = PressStart2P,
                                fontSize = 8.sp,
                                color = gold
                            )
                        }
                    }
                }

                Spacer(Modifier.width(10.dp))

                SmallFigmaButton(
                    label = "ADD",
                    font = PressStart2P,
                    onClick = onAdd,
                    modifier = Modifier.width(88.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendSearchScreen(
    ui: FriendSearchUiState,
    onBack: () -> Unit,
    onQueryChange: (String) -> Unit,
    onAddFriend: (String) -> Unit,
    onDismissSuggestion: (String) -> Unit = {}
) {
    val gold = Color(0xFFE8C46A)
    val cyan = Color(0xFF5FE3FF)

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
                    text = "FIND FRIENDS",
                    fontFamily = PressStart2P,
                    fontSize = 12.sp,
                    color = cyan
                )
            }

            Spacer(Modifier.height(20.dp))

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 12.dp)
            ) {
                if (ui.usersWhoAddedMe.isNotEmpty()) {
                    item {
                        Text(
                            text = "👋 THEY ADDED YOU",
                            fontFamily = PressStart2P,
                            fontSize = 11.sp,
                            color = Color(0xFFFF9500)
                        )
                        Spacer(Modifier.height(8.dp))
                    }

                    items(ui.usersWhoAddedMe, key = { "added_${it.id}" }) { user ->
                        SwipeToRemoveCard(
                            onDismiss = { onDismissSuggestion(user.id) }
                        ) {
                            FriendCardAddBack(
                                user = user,
                                onAddBack = { onAddFriend(user.id) }
                            )
                        }
                    }

                    item {
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = "👥 ALL USERS",
                            fontFamily = PressStart2P,
                            fontSize = 11.sp,
                            color = gold
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                }

                if (ui.results.isEmpty()) {
                    item {
                        Text(
                            text = if (ui.query.isBlank()) "NO USERS AVAILABLE" else "NO MATCH FOUND",
                            fontFamily = PressStart2P,
                            fontSize = 10.sp,
                            color = Color(0xFFB9B4FF)
                        )
                    }
                } else {
                    items(ui.results, key = { it.id }) { user ->
                        SwipeToRemoveCard(
                            onDismiss = { onDismissSuggestion(user.id) }
                        ) {
                            FriendCard(
                                user = user,
                                onAdd = { onAddFriend(user.id) }
                            )
                        }
                    }
                }
            }

            OutlinedTextField(
                value = ui.query,
                onValueChange = onQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                singleLine = true,
                placeholder = {
                    Text(
                        text = "Search username...",
                        fontFamily = PressStart2P,
                        fontSize = 9.sp,
                        color = gold.copy(alpha = 0.6f)
                    )
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = gold,
                    unfocusedBorderColor = gold.copy(alpha = 0.6f),
                    focusedTextColor = gold,
                    unfocusedTextColor = gold,
                    cursorColor = gold,
                    focusedContainerColor = Color.Black.copy(alpha = 0.4f),
                    unfocusedContainerColor = Color.Black.copy(alpha = 0.4f)
                ),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun FriendCardAddBack(
    user: UserProfile,
    onAddBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(18.dp)
    val gold = Color(0xFFE8C46A)
    val orange = Color(0xFFFF9500)
    val cyan = Color(0xFF5FE3FF)

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = shape,
        color = Color.Transparent,
        border = BorderStroke(2.dp, orange)
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
                    .padding(horizontal = 14.dp, vertical = 12.dp)
                    .fillMaxWidth(),
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
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "BEST: ${user.bestScore}",
                            fontFamily = PressStart2P,
                            fontSize = 8.sp,
                            color = cyan
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "🏆", fontSize = 12.sp)
                            Spacer(Modifier.width(2.dp))
                            Text(
                                text = "${user.totalTrophies}",
                                fontFamily = PressStart2P,
                                fontSize = 8.sp,
                                color = gold
                            )
                        }
                    }
                }

                Spacer(Modifier.width(10.dp))

                SmallFigmaButton(
                    label = "ADD BACK",
                    font = PressStart2P,
                    onClick = onAddBack,
                    modifier = Modifier.width(110.dp)
                )
            }
        }
    }
}

@Composable
private fun SuggestedFriendCard(
    user: UserProfile,
    onAdd: () -> Unit,
    modifier: Modifier = Modifier
) {
    val gold = Color(0xFFE8C46A)
    val cyan = Color(0xFF5FE3FF)
    val deep = Color(0xFF0A0833)
    val shape = RoundedCornerShape(16.dp)

    Surface(
        modifier = modifier
            .width(140.dp)
            .clickable { onAdd() },
        shape = shape,
        color = Color.Transparent,
        border = BorderStroke(2.dp, gold.copy(alpha = 0.7f))
    ) {
        Box {
            Image(
                painter = painterResource(R.drawable.btn_background),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            Column(
                modifier = Modifier
                    .padding(12.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.3f))
                        .border(2.dp, cyan, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = user.username.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                        fontFamily = PressStart2P,
                        fontSize = 18.sp,
                        color = gold
                    )
                }

                Spacer(Modifier.height(8.dp))

                Text(
                    text = user.username,
                    fontFamily = PressStart2P,
                    fontSize = 10.sp,
                    color = gold,
                    maxLines = 1
                )

                Spacer(Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(text = "🏆", fontSize = 14.sp)
                    Text(
                        text = "${user.totalTrophies}",
                        fontFamily = PressStart2P,
                        fontSize = 10.sp,
                        color = gold
                    )
                }

                Spacer(Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(28.dp)
                        .background(deep.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .border(1.dp, cyan, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "+ ADD",
                        fontFamily = PressStart2P,
                        fontSize = 8.sp,
                        color = cyan
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeToRemoveCard(
    onDismiss: () -> Unit,
    content: @Composable () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onDismiss()
                true
            } else {
                false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
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
        content()
    }
}
