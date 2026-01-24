package com.example.uleammed.burnoutprediction.presentation.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class BurnoutViewModelFactory(
    private val context: Context
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BurnoutAnalysisViewModel::class.java)) {
            return BurnoutAnalysisViewModel(context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}