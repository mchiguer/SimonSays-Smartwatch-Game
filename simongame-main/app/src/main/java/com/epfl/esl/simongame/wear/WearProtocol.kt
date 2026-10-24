package com.epfl.esl.simongame.wear

object WearPaths {
    const val START_RECORDING = "/start_recording"
    const val STOP_RECORDING = "/stop_recording"
    const val SENSOR_BULK = "/sensor_bulk"
    const val ACK = "/ack"
    const val PING = "/ping"
}

data class StartCaptureRequest(
    val turnId: Long,
    val durationMs: Long,
    val moveName: String = ""
)

data class Vec3Sample(
    val tMs: Long,
    val x: Float,
    val y: Float,
    val z: Float
)

data class MotionWindow(
    val turnId: Long,
    val accel: List<Vec3Sample>,
    val gyro: List<Vec3Sample>
)

data class GameState(
    val phase: String,
    val moveName: String,
    val round: Int,
    val score: Int,
    val timeLeftSec: Int,
    val isRecording: Boolean,
    val lastResultCorrect: Boolean?,
    val retryCount: Int = 0
)
