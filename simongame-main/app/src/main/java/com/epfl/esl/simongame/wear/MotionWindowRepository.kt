package com.epfl.esl.simongame.wear

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

class MotionWindowRepository {
    private val _windows = MutableSharedFlow<MotionWindow>(extraBufferCapacity = 16)
    val windows: SharedFlow<MotionWindow> = _windows

    fun emit(window: MotionWindow) {
        _windows.tryEmit(window)
    }
}
