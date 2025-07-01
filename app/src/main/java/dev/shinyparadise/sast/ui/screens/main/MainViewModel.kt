package dev.shinyparadise.sast.ui.screens.main

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.shinyparadise.sast.domain.AnalysisReport
import dev.shinyparadise.sast.domain.AnalyzerInteractor
import dev.shinyparadise.sast.utils.log
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MainUiState(
    val chosenApkPath: String? = null,
    val chosenApkName: String? = null,
    val isLoading: Boolean = false,
    val report: AnalysisReport? = null,
)

sealed interface MainUiEvent {
    data class OnApkChosen(val uri: Uri?) : MainUiEvent
    data object OnChooseClicked : MainUiEvent
}

sealed interface MainUiEffect {
    data object OpenFilePicker : MainUiEffect
    data object NavigateToDetails : MainUiEffect
}

class MainViewModel(
    private val interactor: AnalyzerInteractor,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState = _uiState.asStateFlow()

    val uiEffects = Channel<MainUiEffect>()

    init {
        interactor.analysisState
            .onEach { handleInteractorState(it) }
            .launchIn(viewModelScope)
    }

    private fun handleInteractorState(state: AnalyzerInteractor.AnalysisState) {
        when (state) {
            is AnalyzerInteractor.AnalysisState.Analyzing,
            AnalyzerInteractor.AnalysisState.Decompiling -> _uiState.update {
                it.copy(isLoading = true)
            }

            is AnalyzerInteractor.AnalysisState.Completed -> {
                state.report.log()

                _uiState.update {
                    it.copy(isLoading = false, report = state.report)
                }
                uiEffects.trySend(MainUiEffect.NavigateToDetails)
            }

            is AnalyzerInteractor.AnalysisState.Error -> { state.log() }

            else -> {}
        }
    }

    fun onEvent(event: MainUiEvent) {
        when (event) {
            is MainUiEvent.OnApkChosen -> onApkChosen(event)
            MainUiEvent.OnChooseClicked -> uiEffects.trySend(MainUiEffect.OpenFilePicker)
        }
    }

    private fun onApkChosen(event: MainUiEvent.OnApkChosen) {
        viewModelScope.launch {
            event.uri?.path?.let { path ->
                _uiState.update {
                    it.copy(
                        chosenApkPath = path,
                        chosenApkName = interactor.getUri(event.uri).lastPathSegment,
                    )
                }
                interactor.analyzeApk(apkUri = event.uri)
            }
        }
    }
}
