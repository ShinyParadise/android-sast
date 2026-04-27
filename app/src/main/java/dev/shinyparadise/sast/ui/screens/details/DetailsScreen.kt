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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.shinyparadise.sast.domain.AnalysisReport
import dev.shinyparadise.sast.domain.Vulnerability
import dev.shinyparadise.sast.ui.screens.main.MainUiEvent
import dev.shinyparadise.sast.ui.screens.main.MainUiState
import dev.shinyparadise.sast.ui.screens.main.MainViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class VulnerabilityCategory(
    val type: String,
    val count: Int,
    val vulnerabilities: List<Vulnerability>,
    val color: Color
)

fun groupVulnerabilities(report: AnalysisReport): List<VulnerabilityCategory> {
    return report.vulnerabilities
        .groupBy { it.type ?: "UNKNOWN" }
        .map { (type, vulns) ->
            VulnerabilityCategory(
                type = type,
                count = vulns.size,
                vulnerabilities = vulns,
                color = getTypeColor(type)
            )
        }
        .sortedByDescending { it.count }
}

fun getTypeColor(type: String): Color {
    return when (type) {
        "HARDCODED_SECRET" -> Color(0xFFD32F2F.toInt()) // Red
        "INSECURE_CRYPTO" -> Color(0xFFFF9800.toInt()) // Orange
        "WEBVIEW_JS_ENABLED" -> Color(0xFFFFEB3B.toInt()) // Yellow
        else -> Color.Gray
    }
}

fun generateTextReport(report: AnalysisReport): String {
    return buildString {
        appendLine("SAST Analysis Report")
        appendLine("=" .repeat(50))
        appendLine("APK: ${report.apkPath}")
        appendLine("Total Vulnerabilities: ${report.vulnerabilities.size}")
        appendLine()

        val grouped = report.vulnerabilities.groupBy { it.type ?: "UNKNOWN" }
        grouped.forEach { (type, vulns) ->
            appendLine("Type: $type (${vulns.size} vulnerabilities)")
            appendLine("-".repeat(50))
            vulns.forEach { vuln ->
                appendLine("  File: ${vuln.file}")
                appendLine("  Line: ${vuln.line}")
                appendLine("  Description: ${vuln.description}")
                appendLine()
            }
        }
    }
}

fun generateCsvReport(report: AnalysisReport): String {
    return buildString {
        appendLine("File,Line,Type,Description")
        report.vulnerabilities.forEach { vuln ->
            val desc = vuln.description.replace("\"", "\"\"")
            appendLine("\"${vuln.file}\",${vuln.line},\"${vuln.type ?: "UNKNOWN"}\",\"$desc\"")
        }
    }
}

fun generatePdfReport(report: AnalysisReport, context: android.content.Context): android.net.Uri? {
    return try {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "sast_report_$timestamp.pdf"

        val content = generateTextReport(report)

        val resolver = context.contentResolver
        val uri = android.provider.DocumentsContract.buildDocumentUri(
            "com.android.externalstorage.documents",
            fileName
        )

        // For simplicity, save as text file with .pdf extension
        // In production, use a proper PDF library like iText or PdfDocument
        val file = java.io.File(context.cacheDir, fileName)
        file.writeText(content)

        android.net.Uri.fromFile(file)
    } catch (e: Exception) {
        e.printStackTrace()
        null
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
    var searchQuery by remember { mutableStateOf("") }
    val expandedCategories = remember { mutableStateMapOf<String, Boolean>() }

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
                val coroutineScope = rememberCoroutineScope()
                val snackbarHostState = remember { SnackbarHostState() }

                ExportSection(
                    report = report,
                    snackbarHostState = snackbarHostState,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )

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

                if (expandedCategories[category.type] == true) {
                    item {
                        AnimatedVisibility(
                            visible = true,
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
}

@Composable
private fun SearchBar(
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
                    modifier = Modifier.clickable { onQueryChange("") }
                )
            }
        },
        singleLine = true,
        modifier = modifier.fillMaxWidth()
    )
}

@Composable
private fun StatisticsSection(
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
private fun CategoryHeader(
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
private fun VulnerabilityItem(
    item: Vulnerability,
    color: Color = Color.Gray,
    modifier: Modifier = Modifier,
) {
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = androidx.compose.material3.CardDefaults.cardColors(
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
                        clipboardManager.setText(AnnotatedString(text))
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("✔ Copied to clipboard")
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
private fun ExportSection(
    report: AnalysisReport,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Button(
            onClick = {
                coroutineScope.launch {
                    val text = generateTextReport(report)
                    val success = saveFile(context, "sast_report_${System.currentTimeMillis()}.txt", text)
                    snackbarHostState.showSnackbar(
                        if (success) "✔ TXT report saved" else "✘ Failed to save TXT"
                    )
                }
            },
            modifier = Modifier.weight(1f)
        ) {
            Text("TXT", style = MaterialTheme.typography.labelLarge)
        }

        Button(
            onClick = {
                coroutineScope.launch {
                    val csv = generateCsvReport(report)
                    val success = saveFile(context, "sast_report_${System.currentTimeMillis()}.csv", csv)
                    snackbarHostState.showSnackbar(
                        if (success) "✔ CSV report saved" else "✘ Failed to save CSV"
                    )
                }
            },
            modifier = Modifier.weight(1f)
        ) {
            Text("CSV", style = MaterialTheme.typography.labelLarge)
        }

        Button(
            onClick = {
                coroutineScope.launch {
                    val text = generateTextReport(report)
                    val success = saveFile(context, "sast_report_${System.currentTimeMillis()}.pdf", text)
                    snackbarHostState.showSnackbar(
                        if (success) "✔ PDF report saved" else "✘ Failed to save PDF"
                    )
                }
            },
            modifier = Modifier.weight(1f)
        ) {
            Text("PDF", style = MaterialTheme.typography.labelLarge)
        }
    }
}

suspend fun saveFile(context: android.content.Context, fileName: String, content: String): Boolean {
    return try {
        val resolver = context.contentResolver
        val values = android.content.ContentValues().apply {
            put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "text/plain")
            put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, "Documents")
        }

        val uri = resolver.insert(android.provider.MediaStore.Files.getContentUri("external"), values)
        uri?.let {
            resolver.openOutputStream(it)?.use { outputStream ->
                outputStream.write(content.toByteArray())
            }
            true
        } ?: false
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }
}
