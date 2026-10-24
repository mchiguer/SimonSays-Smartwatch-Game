package com.epfl.esl.simongame

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.rememberNavController
import com.epfl.esl.simongame.di.LocalAppContainer
import com.epfl.esl.simongame.navigation.AppNavHost

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SimonSaysApp()
        }
    }
}

@Composable
fun SimonSaysApp() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val container = (context.applicationContext as SimonGameApplication).container

    CompositionLocalProvider(LocalAppContainer provides container) {
        MaterialTheme {
            Surface(Modifier.fillMaxSize()) {
                AppNavHost(navController = navController)
            }
        }
    }
}