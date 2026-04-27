package dev.shinyparadise.sast.ui.screens.details

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.shinyparadise.sast.ui.screens.main.MainUiEvent
import dev.shinyparadise.sast.ui.screens.main.MainUiState
import dev.shinyparadise.sast.ui.screens.main.MainViewModel

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
    var searchQuery by remember { mutableStateOf("") }
    val expandedCategories = remember { mutableStateMapOf<String, Boolean>() }
    val snackbarHostState = remember { SnackbarHostState() }

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
        val categories = remember(report, searchQuery) {
            groupVulnerabilities(report).map { category ->
                val filteredVulns = if (searchQuery.isEmpty()) {
                    category.vulnerabilities
                } else {
                    category.vulnerabilities.filter {
                        it.file.contains(searchQuery, ignoreCase = true) ||
                        it.description.contains(searchQuery, ignoreCase = true)
                    }
                }
                category.copy(vulnerabilities = filteredVulns, count = filteredVulns.size)
            }.filter { it.count > 0 }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
        ) {
            item {
                Column(
                    modifier = Modifier
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "Analysis Results",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = uiState.chosenApkPath ?: report.apkPath,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            item {
                SearchBar(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            item {
                StatisticsSection(
                    categories = categories,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            item {
                ExportSection(
                    onEvent = onEvent,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }

            item {
                SnackbarHost(hostState = snackbarHostState)
            }

            categories.forEach { category ->
                item(key = "header_${category.type}") {
                    CategoryHeader(
                        category = category,
                        totalCount = categories.sumOf { it.count },
                        isExpanded = expandedCategories[category.type] ?: false,
                        onToggleExpand = {
                            expandedCategories[category.type] = !(expandedCategories[category.type] ?: false)
                        },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }

                item {
                    AnimatedVisibility(
                        visible = expandedCategories[category.type] == true,
                        enter = fadeIn(animationSpec = tween(300)) + slideInVertically(animationSpec = tween(300)),
                        exit = fadeOut(animationSpec = tween(300)) + slideOutVertically(animationSpec = tween(300))
                    ) {
                        Column {
                            category.vulnerabilities.forEach { vuln ->
                                VulnerabilityItem(
                                    item = vuln,
                                    color = category.color,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
