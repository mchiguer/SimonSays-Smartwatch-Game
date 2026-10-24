package com.epfl.esl.simongame.friends

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.epfl.esl.simongame.data.UserRepository

class FriendsListViewModelFactory(
    private val repo: UserRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FriendsListViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return FriendsListViewModel(repo) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: $modelClass")
    }
}
