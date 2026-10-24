package com.epfl.esl.simongame.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.epfl.esl.simongame.R

@Composable
fun RegisterScreen(
    onRegister: (username: String, email: String, password: String, isLogin: Boolean) -> Unit,
    isLoading: Boolean = false,
    errorMessage: String? = null
) {
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoginMode by remember { mutableStateOf(false) }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {

        val titleY = maxHeight * 0.08f
        val formY = maxHeight * 0.32f

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

        HomeTitle(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = titleY)
        )

        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = formY)
                .padding(horizontal = 28.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Text(
                text = if (isLoginMode) "LOGIN" else "REGISTER",
                fontFamily = PressStart2P,
                fontSize = 16.sp,
                color = Color(0xFFE8C46A)
            )

            Spacer(Modifier.height(14.dp))

            if (!isLoginMode) {
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = !isLoading,
                    label = {
                        Text(
                            "Username",
                            fontFamily = PressStart2P,
                            fontSize = 10.sp,
                            color = Color(0xFFE8C46A)
                        )
                    },
                    colors = textFieldColors(),
                    shape = RoundedCornerShape(14.dp)
                )

                Spacer(Modifier.height(10.dp))
            }

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = !isLoading,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                label = {
                    Text(
                        "Email",
                        fontFamily = PressStart2P,
                        fontSize = 10.sp,
                        color = Color(0xFFE8C46A)
                    )
                },
                colors = textFieldColors(),
                shape = RoundedCornerShape(14.dp)
            )

            Spacer(Modifier.height(10.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = !isLoading,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                label = {
                    Text(
                        "Password",
                        fontFamily = PressStart2P,
                        fontSize = 10.sp,
                        color = Color(0xFFE8C46A)
                    )
                },
                colors = textFieldColors(),
                shape = RoundedCornerShape(14.dp)
            )

            Spacer(Modifier.height(18.dp))

            if (errorMessage != null) {
                Text(
                    text = errorMessage,
                    fontFamily = PressStart2P,
                    fontSize = 8.sp,
                    color = Color(0xFFFF6B6B),
                    modifier = Modifier.padding(bottom = 10.dp)
                )
            }

            if (isLoading) {
                CircularProgressIndicator(
                    color = Color(0xFFE8C46A),
                    modifier = Modifier.size(48.dp)
                )
            } else {
                FigmaMenuButton(
                    label = if (isLoginMode) "LOGIN" else "REGISTER",
                    font = PressStart2P,
                    onClick = {
                        val isValid = if (isLoginMode) {
                            email.trim().isNotEmpty() && password.isNotEmpty()
                        } else {
                            username.trim().isNotEmpty() && email.trim().isNotEmpty() && password.length >= 6
                        }
                        if (isValid) {
                            onRegister(username.trim(), email.trim(), password, isLoginMode)
                        }
                    },
                    modifier = Modifier.height(72.dp)
                )
            }

            Spacer(Modifier.height(10.dp))

            val validationMessage = when {
                isLoginMode && email.trim().isEmpty() -> "ENTER YOUR EMAIL"
                isLoginMode && password.isEmpty() -> "ENTER YOUR PASSWORD"
                !isLoginMode && username.trim().isEmpty() -> "ENTER A USERNAME"
                !isLoginMode && email.trim().isEmpty() -> "ENTER YOUR EMAIL"
                !isLoginMode && password.length < 6 -> "PASSWORD MIN 6 CHARS"
                else -> ""
            }

            Text(
                text = validationMessage,
                fontFamily = PressStart2P,
                fontSize = 8.sp,
                color = Color(0xFFB9B4FF)
            )

            Spacer(Modifier.height(20.dp))

            Text(
                text = if (isLoginMode) "No account? Register" else "Have an account? Login",
                fontFamily = PressStart2P,
                fontSize = 10.sp,
                color = Color(0xFFE8C46A),
                textDecoration = TextDecoration.Underline,
                modifier = Modifier.clickable(enabled = !isLoading) {
                    isLoginMode = !isLoginMode
                }
            )
        }
    }
}

@Composable
private fun textFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Color(0xFFD6B15D),
    unfocusedBorderColor = Color(0xFFD6B15D),
    focusedTextColor = Color(0xFFE8C46A),
    unfocusedTextColor = Color(0xFFE8C46A),
    cursorColor = Color(0xFFE8C46A),
    focusedContainerColor = Color.Black.copy(alpha = 0.25f),
    unfocusedContainerColor = Color.Black.copy(alpha = 0.25f),
    focusedLabelColor = Color(0xFFE8C46A),
    unfocusedLabelColor = Color(0xFFE8C46A),
    disabledBorderColor = Color(0xFFD6B15D).copy(alpha = 0.5f),
    disabledTextColor = Color(0xFFE8C46A).copy(alpha = 0.5f),
    disabledLabelColor = Color(0xFFE8C46A).copy(alpha = 0.5f),
    disabledContainerColor = Color.Black.copy(alpha = 0.25f)
)
