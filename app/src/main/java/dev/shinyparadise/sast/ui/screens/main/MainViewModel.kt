package dev.shinyparadise.sast.ui.screens.main

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.shinyparadise.sast.domain.AnalysisReport
import dev.shinyparadise.sast.domain.AnalyzerInteractor
import dev.shinyparadise.sast.domain.ReportGenerator
import dev.shinyparadise.sast.domain.ReportResult
import dev.shinyparadise.sast.domain.Vulnerability
import dev.shinyparadise.sast.domain.VulnerabilityWithAIInsight
import dev.shinyparadise.sast.ui.screens.details.DetailsItem
import dev.shinyparadise.sast.ui.screens.details.VulnerabilityCategory
import dev.shinyparadise.sast.ui.screens.details.groupVulnerabilities
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
    val loadingStage: String = "",
    val loadingProgress: Float = 0f,
    val report: AnalysisReport? = null,
    val exportResult: ExportResult? = null,
    val expandedCategories: Set<String> = emptySet(),
    val searchQuery: String = "",
    val categories: List<VulnerabilityCategory> = emptyList(),
    val totalCount: Int = 0,
    val detailsItems: List<DetailsItem> = emptyList(),
)

data class ExportResult(
    val format: ExportFormat,
    val success: Boolean,
)

sealed interface MainUiEvent {
    data class OnApkChosen(val uri: Uri?) : MainUiEvent
    data object OnChooseClicked : MainUiEvent
    data class ExportReport(val format: ExportFormat) : MainUiEvent
    data object OnExportResultShown : MainUiEvent
    data class ToggleCategory(val categoryType: String) : MainUiEvent
    data class SetSearchQuery(val query: String) : MainUiEvent
    data object NavigateBack : MainUiEvent
    data object NavigateToSettings : MainUiEvent
}

sealed interface MainUiEffect {
    data object OpenFilePicker : MainUiEffect
    data object NavigateToDetails : MainUiEffect
    data object NavigateBack : MainUiEffect
    data object NavigateToSettings : MainUiEffect
}

enum class ExportFormat {
    TXT, CSV, PDF
}

class MainViewModel(
    private val interactor: AnalyzerInteractor,
    private val reportGenerator: ReportGenerator,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState = _uiState.asStateFlow()

    val uiEffects = Channel<MainUiEffect>()

    override fun onCleared() {
        uiEffects.close()
        super.onCleared()
    }

    init {
        interactor.analysisState
            .onEach { handleInteractorState(it) }
            .launchIn(viewModelScope)
    }

    private fun handleInteractorState(state: AnalyzerInteractor.AnalysisState) {
        when (state) {
            AnalyzerInteractor.AnalysisState.Decompiling -> _uiState.update {
                it.copy(
                    isLoading = true,
                    loadingStage = "Декомпиляция APK...",
                    loadingProgress = 0f
                )
            }

            is AnalyzerInteractor.AnalysisState.Analyzing -> _uiState.update {
                it.copy(
                    isLoading = true,
                    loadingStage = "Анализ кода...",
                    loadingProgress = state.progress
                )
            }

            is AnalyzerInteractor.AnalysisState.Completed -> {
                state.report.log()

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        loadingStage = "",
                        loadingProgress = 0f,
                        report = state.report
                    )
                }
                recalculateDetailsItems()
                uiEffects.trySend(MainUiEffect.NavigateToDetails)
            }

            is AnalyzerInteractor.AnalysisState.Error -> {
                state.log()
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        loadingStage = "",
                        loadingProgress = 0f
                    )
                }
            }

            else -> {}
        }
    }

    fun onEvent(event: MainUiEvent) {
        when (event) {
            is MainUiEvent.OnApkChosen -> onApkChosen(event)
            MainUiEvent.OnChooseClicked -> uiEffects.trySend(MainUiEffect.OpenFilePicker)
            is MainUiEvent.ExportReport -> exportReport(event.format)
            MainUiEvent.OnExportResultShown -> _uiState.update { it.copy(exportResult = null) }
            is MainUiEvent.ToggleCategory -> _uiState.update { state ->
                val newExpanded = if (event.categoryType in state.expandedCategories) {
                    state.expandedCategories - event.categoryType
                } else {
                    state.expandedCategories + event.categoryType
                }
                state.copy(expandedCategories = newExpanded)
            }.also { recalculateDetailsItems() }
            is MainUiEvent.SetSearchQuery -> _uiState.update { it.copy(searchQuery = event.query) }.also { recalculateDetailsItems() }
            MainUiEvent.NavigateBack -> uiEffects.trySend(MainUiEffect.NavigateBack)
            MainUiEvent.NavigateToSettings -> uiEffects.trySend(MainUiEffect.NavigateToSettings)
        }
    }

    private fun recalculateDetailsItems() {
        val report = _uiState.value.report ?: return
        val state = _uiState.value
        val categories = groupVulnerabilities(report).map { category ->
            val filteredVulns = if (state.searchQuery.isEmpty()) {
                category.vulnerabilities
            } else {
                category.vulnerabilities.filter {
                    it.file.contains(state.searchQuery, ignoreCase = true) ||
                    it.description.contains(state.searchQuery, ignoreCase = true)
                }
            }
            category.copy(vulnerabilities = filteredVulns, count = filteredVulns.size)
        }.filter { it.count > 0 }

        val totalCount = categories.sumOf { it.count }
        val aiInsightsMap = report.aiInsights?.associateBy { it.originalVulnerability }

        val detailsItems = buildList {
            categories.forEach { category ->
                add(DetailsItem.CategoryHeader(
                    category = category,
                    totalCount = totalCount,
                    isExpanded = category.type in state.expandedCategories,
                    onToggleExpand = { onEvent(MainUiEvent.ToggleCategory(category.type)) }
                ))
                if (category.type in state.expandedCategories) {
                    category.vulnerabilities.forEach { vuln ->
                        val aiInsight = aiInsightsMap?.get(vuln)
                        add(DetailsItem.Vuln(vuln = vuln, color = category.color, aiInsight = aiInsight))
                    }
                }
            }
        }

        _uiState.update { it.copy(categories = categories, totalCount = totalCount, detailsItems = detailsItems) }
    }

    private fun onApkChosen(event: MainUiEvent.OnApkChosen) {
        viewModelScope.launch {
            event.uri?.let { uri ->
                val absoluteUri = interactor.getUri(uri)
                val originalFileName = getOriginalFileName(uri)
                _uiState.update {
                    it.copy(
                        chosenApkPath = originalFileName ?: uri.toString(),
                        chosenApkName = originalFileName ?: absoluteUri.lastPathSegment,
                    )
                }
                interactor.analyzeApk(apkUri = event.uri)
            }
        }
    }

    private fun exportReport(format: ExportFormat) {
        viewModelScope.launch {
            val report = uiState.value.report ?: return@launch
            val result = when (format) {
                ExportFormat.TXT -> {
                    val contentResult = reportGenerator.generateTextReport(report)
                    if (contentResult is ReportResult.Success) {
                        reportGenerator.saveToFile(getContext(), "sast_report_${System.currentTimeMillis()}.txt", contentResult.data as String)
                    } else {
                        contentResult
                    }
                }
                ExportFormat.CSV -> {
                    val contentResult = reportGenerator.generateCsvReport(report)
                    if (contentResult is ReportResult.Success) {
                        reportGenerator.saveToFile(getContext(), "sast_report_${System.currentTimeMillis()}.csv", contentResult.data as String)
                    } else {
                        contentResult
                    }
                }
                ExportFormat.PDF -> {
                    val contentResult = reportGenerator.generatePdfReport(report)
                    if (contentResult is ReportResult.Success) {
                        reportGenerator.savePdfToFile(getContext(), "sast_report_${System.currentTimeMillis()}.pdf", contentResult.data as ByteArray)
                    } else {
                        contentResult
                    }
                }
            }
            val success = result is ReportResult.Success
            _uiState.update { it.copy(exportResult = ExportResult(format, success)) }
        }
    }

    private fun getOriginalFileName(uri: Uri): String? {
        return try {
            if (uri.scheme == "content") {
                val cursor = getContext().contentResolver.query(
                    uri,
                    arrayOf(OpenableColumns.DISPLAY_NAME),
                    null,
                    null,
                    null
                )
                cursor?.use {
                    if (it.moveToFirst()) {
                        it.getString(0)
                    } else null
                }
            } else {
                uri.lastPathSegment
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun getContext(): Context = interactor.getContext()
}
