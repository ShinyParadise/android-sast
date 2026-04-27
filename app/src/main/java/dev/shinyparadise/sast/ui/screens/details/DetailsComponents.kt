package dev.shinyparadise.sast.ui.screens.details

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import dev.shinyparadise.sast.domain.Vulnerability
import dev.shinyparadise.sast.ui.screens.main.ExportFormat
import dev.shinyparadise.sast.ui.screens.main.MainUiEvent
import kotlinx.coroutines.launch

@Composable
fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        label = { Text("Search by file name or description") },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Clear",
                    modifier = Modifier.combinedClickable(
                        onClick = { onQueryChange("") },
                        onLongClick = { }
                    )
                )
            }
        },
        singleLine = true,
        modifier = modifier.fillMaxWidth()
    )
}

@Composable
fun StatisticsSection(
    categories: List<VulnerabilityCategory>,
    modifier: Modifier = Modifier,
) {
    val totalCount = categories.sumOf { it.count }

    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Statistics (Total: $totalCount)",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            categories.forEachIndexed { index, category ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(category.color)
                    )
                    Text(
                        text = category.type,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "${category.count}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "(${String.format("%.1f", category.count * 100f / totalCount)}%)",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                LinearProgressIndicator(
                    progress = { if (totalCount > 0) category.count.toFloat() / totalCount else 0f },
                    color = category.color,
                    trackColor = category.color.copy(alpha = 0.2f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = if (index < categories.size - 1) 8.dp else 0.dp)
                )
            }
        }
    }
}

@Composable
fun CategoryHeader(
    category: VulnerabilityCategory,
    totalCount: Int,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggleExpand() }
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(category.color)
                )
                Column {
                    Text(
                        text = category.type,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "${category.count} items (${String.format("%.1f", category.count * 100f / totalCount)}%)",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            Icon(
                if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = if (isExpanded) "Collapse" else "Expand"
            )
        }
    }
}

@Composable
fun VulnerabilityItem(
    item: Vulnerability,
    color: Color = Color.Gray,
    modifier: Modifier = Modifier,
) {
    val clipboard = LocalClipboard.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.08f)
        )
    ) {
        Row(
            modifier = Modifier
                .combinedClickable(
                    onClick = {},
                    onLongClick = {
                        val text = buildString {
                            appendLine("File: ${item.file}")
                            appendLine("Line: ${item.line}")
                            item.type?.let { appendLine("Type: $it") }
                            appendLine("Description: ${item.description}")
                        }
                        coroutineScope.launch {
                            clipboard.setClipEntry(
                                androidx.compose.ui.platform.ClipEntry(android.content.ClipData.newPlainText("vulnerability", text))
                            )
                            snackbarHostState.showSnackbar("Copied to clipboard")
                        }
                    }
                )
                .padding(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .width(4.dp)
                    .height(56.dp)
                    .background(color)
            ) { }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.file.split("/").last(),
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1
                )
                Text(
                    text = "Line: ${item.line}",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 2.dp)
                )
                Text(
                    text = item.description,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp),
                    maxLines = 2
                )
            }
        }

        SnackbarHost(hostState = snackbarHostState)
    }
}

@Composable
fun ExportSection(
    onEvent: (MainUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Button(
            onClick = { onEvent(MainUiEvent.ExportReport(ExportFormat.TXT)) },
            modifier = Modifier.weight(1f)
        ) {
            Text("TXT", style = MaterialTheme.typography.labelLarge)
        }

        Button(
            onClick = { onEvent(MainUiEvent.ExportReport(ExportFormat.CSV)) },
            modifier = Modifier.weight(1f)
        ) {
            Text("CSV", style = MaterialTheme.typography.labelLarge)
        }

        Button(
            onClick = { onEvent(MainUiEvent.ExportReport(ExportFormat.PDF)) },
            modifier = Modifier.weight(1f)
        ) {
            Text("PDF", style = MaterialTheme.typography.labelLarge)
        }
    }
}
