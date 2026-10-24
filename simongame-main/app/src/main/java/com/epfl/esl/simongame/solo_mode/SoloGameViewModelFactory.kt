package com.epfl.esl.simongame.solo_mode

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.epfl.esl.simongame.wear.MotionWindowRepository
import com.epfl.esl.simongame.wear.PhoneWearClient

class SoloGameViewModelFactory(
    private val predictor: MotionToMovePredictor,
    private val motionWindowRepository: MotionWindowRepository,
    private val phoneWearClient: PhoneWearClient,
    private val appContext: Context
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SoloGameViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SoloGameViewModel(predictor, motionWindowRepository, phoneWearClient, appContext) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: $modelClass")
    }
}