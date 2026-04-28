package dev.shinyparadise.sast.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.shinyparadise.sast.data.SettingsRepository
import dev.shinyparadise.sast.domain.AIMode
import dev.shinyparadise.sast.domain.AppSettings
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    val settings: StateFlow<AppSettings> = settingsRepository.settingsFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())

    fun updateAiAnalysisEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateSettings(
                settings.value.copy(aiAnalysisEnabled = enabled)
            )
        }
    }

    fun updateAiAnalysisMode(mode: AIMode) {
        viewModelScope.launch {
            settingsRepository.updateSettings(
                settings.value.copy(aiAnalysisMode = mode)
            )
        }
    }

    fun updateRemoteModelUrl(url: String) {
        viewModelScope.launch {
            settingsRepository.updateSettings(
                settings.value.copy(remoteModelUrl = url.ifBlank { null })
            )
        }
    }

    fun updateRemoteApiKey(apiKey: String) {
        viewModelScope.launch {
            settingsRepository.updateSettings(
                settings.value.copy(remoteApiKey = apiKey.ifBlank { null })
            )
        }
    }

    fun updateSelectedRemoteModel(model: String) {
        viewModelScope.launch {
            settingsRepository.updateSettings(
                settings.value.copy(selectedRemoteModel = model.ifBlank { null })
            )
        }
    }

    fun updateAiChunkSize(size: Int) {
        viewModelScope.launch {
            settingsRepository.updateSettings(
                settings.value.copy(aiChunkSize = size.coerceIn(5, 50))
            )
        }
    }
}