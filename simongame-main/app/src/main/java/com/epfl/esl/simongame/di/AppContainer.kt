package com.epfl.esl.simongame.di

import android.content.Context
import androidx.compose.runtime.staticCompositionLocalOf
import com.epfl.esl.simongame.data.FirebaseUserRepository
import com.epfl.esl.simongame.data.SettingsRepository
import com.epfl.esl.simongame.data.UserRepository
import com.epfl.esl.simongame.game.Move
import com.epfl.esl.simongame.solo_mode.ChannelMapping
import com.epfl.esl.simongame.solo_mode.ModelSpec
import com.epfl.esl.simongame.wear.MotionWindowRepository
import com.epfl.esl.simongame.wear.PhoneWearClient
import com.epfl.esl.simongame.wear.WearConnectionRepository
import com.epfl.esl.simongame.solo_mode.MotionToMovePredictor
import com.epfl.esl.simongame.solo_mode.TfLiteMotionToMovePredictor
import com.epfl.esl.simongame.solo_mode.SimpleEnergyMovePredictor
class AppContainer(context: Context) {
    val userRepository: UserRepository = FirebaseUserRepository()
    val settingsRepository: SettingsRepository = SettingsRepository(context)

    val wearConnectionRepository = WearConnectionRepository()
    val motionWindowRepository = MotionWindowRepository()
    val phoneWearClient = PhoneWearClient(context.applicationContext, motionWindowRepository)

    val motionToMovePredictor: MotionToMovePredictor by lazy {
        try {
            TfLiteMotionToMovePredictor(
                context.applicationContext,
                ModelSpec(
                    assetName = "move_classifier.tflite",
                    channelMapping = ChannelMapping.ACCEL_GYRO_6,
                    labels = listOf(
                        Move.CLAP,
                        Move.JUMP,
                        Move.RAISE_ARMS,
                        Move.RUN_IN_PLACE,
                        Move.SAY_HI,
                        Move.SPIN_AROUND
                    )
                )
            )
        } catch (e: Throwable) {
            SimpleEnergyMovePredictor()
        }
    }
}

val LocalAppContainer = staticCompositionLocalOf<AppContainer> {
    error("AppContainer missing. Provide it in SimonSaysApp().")
}