package se.elnor.elprisnu.ui.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import se.elnor.elprisnu.repository.AnalysisRepository
import se.elnor.elprisnu.repository.ElectricityRepository
import se.elnor.elprisnu.repository.SettingsRepository

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
