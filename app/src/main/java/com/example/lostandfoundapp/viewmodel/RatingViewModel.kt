package com.example.lostandfoundapp.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.lostandfoundapp.data.AppDatabase
import com.example.lostandfoundapp.model.Rating
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class RatingViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = AppDatabase.getDatabase(application).ratingDao()

    val allRatings: StateFlow<List<Rating>> = dao.getAllRatings()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun submitRating(rating: Rating) {
        viewModelScope.launch {
            dao.insertRating(rating)
        }
    }
}
