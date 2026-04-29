package dev.shinyparadise.sast.ui.screens.details

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.shinyparadise.sast.domain.Vulnerability
import dev.shinyparadise.sast.domain.VulnerabilityWithAIInsight
import dev.shinyparadise.sast.ui.screens.main.MainUiEvent
import dev.shinyparadise.sast.ui.screens.main.MainUiState
import dev.shinyparadise.sast.ui.screens.main.MainViewModel
import kotlinx.coroutines.launch

sealed class DetailsItem {
    abstract val key: String

    data class CategoryHeader(
        val category: VulnerabilityCategory,
        val totalCount: Int,
        val isExpanded: Boolean,
        val onToggleExpand: () -> Unit
    ) : DetailsItem() {
        override val key: String get() = "header_${category.type}"
    }

    data class Vuln(
        val vuln: Vulnerability,
        val color: Color,
        val aiInsight: VulnerabilityWithAIInsight? = null
    ) : DetailsItem() {
        override val key: String get() = "vuln_${vuln.file}_${vuln.line}"
    }
}

@Composable
fun DetailsScreen(
    viewModel: MainViewModel,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    DetailsScreenContent(uiState, viewModel::onEvent)
}

@Composable
private fun DetailsScreenContent(
    uiState: MainUiState,
    onEvent: (MainUiEvent) -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(uiState.exportResult) {
        uiState.exportResult?.let { result ->
            val message = if (result.success) {
                "${result.format.name} report saved"
            } else {
                "Failed to save ${result.format.name}"
            }
            snackbarHostState.showSnackbar(message)
            onEvent(MainUiEvent.OnExportResultShown)
        }
    }

    uiState.report?.let { report ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                item(key = "header") {
                    Column(
                        modifier = Modifier
                            .statusBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            IconButton(onClick = { onEvent(MainUiEvent.NavigateBack) }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back"
                                )
                            }
                            Text(
                                text = "Analysis Results",
                                style = MaterialTheme.typography.titleLarge,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(start = 8.dp)
                            )
                            IconButton(
                                onClick = { onEvent(MainUiEvent.NavigateToSettings) },
                                modifier = Modifier.padding(start = 8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = "Settings"
                                )
                            }
                        }
                        Text(
                            text = uiState.chosenApkPath ?: report.apkPath,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }

                item(key = "search") {
                    SearchBar(
                        query = uiState.searchQuery,
                        onQueryChange = { onEvent(MainUiEvent.SetSearchQuery(it)) },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }

                val aiInsights = report.aiInsights

                item(key = "statistics") {
                    StatisticsSection(
                        categories = uiState.categories,
                        aiInsights = aiInsights,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }

                item(key = "export") {
                    ExportSection(
                        onEvent = onEvent,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }

                items(uiState.detailsItems, key = { it.key }) { item ->
                    when (item) {
                        is DetailsItem.CategoryHeader -> {
                            CategoryHeader(
                                category = item.category,
                                totalCount = item.totalCount,
                                isExpanded = item.isExpanded,
                                onToggleExpand = item.onToggleExpand,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                            )
                        }
                        is DetailsItem.Vuln -> {
                            VulnerabilityItem(
                                item = item.vuln,
                                color = item.color,
                                aiInsight = item.aiInsight,
                                onCopied = {
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar("Copied to clipboard")
                                    }
                                },
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
            )
        }
    }
}
