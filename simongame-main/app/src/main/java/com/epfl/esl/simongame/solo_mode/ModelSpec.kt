package com.epfl.esl.simongame.solo_mode

import com.epfl.esl.simongame.game.Move

data class ModelSpec(
    val assetName: String = "move_classifier.tflite",
    val labels: List<Move>,
    val channelMapping: ChannelMapping
)

sealed class ChannelMapping {
    data object ACCEL_3 : ChannelMapping()
    data object ACCEL_GYRO_6 : ChannelMapping()
}
