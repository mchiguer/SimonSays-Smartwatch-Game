package com.epfl.esl.simongame.wear

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

class WearConnectionRepository {
    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected

    fun setConnected(value: Boolean) {
        _isConnected.update { value }
    }
}
