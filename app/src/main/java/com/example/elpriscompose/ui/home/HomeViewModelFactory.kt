package com.example.elpriscompose.ui.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.elpriscompose.repository.AnalysisRepository
import com.example.elpriscompose.repository.ElectricityRepository
import com.example.elpriscompose.repository.SettingsRepository

class HomeViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            return HomeViewModel(
                electricityRepository = ElectricityRepository(),
                analysisRepository = AnalysisRepository(),
                settingsRepository = SettingsRepository(context)
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
