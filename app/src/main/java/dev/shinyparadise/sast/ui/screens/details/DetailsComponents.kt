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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.unit.dp
import dev.shinyparadise.sast.domain.Vulnerability
import dev.shinyparadise.sast.domain.VulnerabilityWithAIInsight
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
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Clear"
                    )
                }
            }
        },
        singleLine = true,
        modifier = modifier.fillMaxWidth()
    )
}

@Composable
fun StatisticsSection(
    categories: List<VulnerabilityCategory>,
    aiInsights: List<VulnerabilityWithAIInsight>? = null,
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

            if (aiInsights != null) {
                val highRiskCount = aiInsights.count { (it.aiRiskScore ?: 0f) >= 0.7f }
                val mediumRiskCount = aiInsights.count { (it.aiRiskScore ?: 0f) >= 0.4f && (it.aiRiskScore ?: 0f) < 0.7f }
                val lowRiskCount = aiInsights.size - highRiskCount - mediumRiskCount

                Text(
                    text = "AI Analysis",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    AiRiskCard(
                        label = "High",
                        count = highRiskCount,
                        color = Color(0xFFE53935),
                        modifier = Modifier.weight(1f)
                    )
                    AiRiskCard(
                        label = "Medium",
                        count = mediumRiskCount,
                        color = Color(0xFFFFA726),
                        modifier = Modifier.weight(1f)
                    )
                    AiRiskCard(
                        label = "Low",
                        count = lowRiskCount,
                        color = Color(0xFF4CAF50),
                        modifier = Modifier.weight(1f)
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
            }

            categories.forEachIndexed { index, category ->
                val percentage = if (totalCount > 0) category.count * 100f / totalCount else 0f

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
                        text = "(${String.format("%.1f", percentage)}%)",
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
    val percentage = if (totalCount > 0) category.count * 100f / totalCount else 0f

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
                        text = "${category.count} items (${String.format("%.1f", percentage)}%)",
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
fun AiRiskCard(
    label: String,
    count: Int,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.15f))
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "$count",
                style = MaterialTheme.typography.titleMedium,
                color = color
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = color
            )
        }
    }
}

@Composable
fun AiBadge(
    riskScore: Float?,
    severity: String?,
    modifier: Modifier = Modifier,
) {
    val color = when {
        riskScore == null -> Color.Gray
        riskScore >= 0.7f -> Color(0xFFE53935)
        riskScore >= 0.4f -> Color(0xFFFFA726)
        else -> Color(0xFF4CAF50)
    }

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.15f))
    ) {
        Text(
            text = severity ?: "AI",
            style = MaterialTheme.typography.labelSmall,
            color = color,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun VulnerabilityItem(
    item: Vulnerability,
    color: Color = Color.Gray,
    aiInsight: VulnerabilityWithAIInsight? = null,
    onCopied: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val clipboard = LocalClipboard.current
    val coroutineScope = rememberCoroutineScope()

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
                            aiInsight?.let {
                                appendLine("AI Risk: ${it.aiRiskScore}")
                                appendLine("AI Severity: ${it.aiSeverity}")
                                it.aiRecommendation?.let { appendLine("AI Recommendation: $it") }
                            }
                        }
                        coroutineScope.launch {
                            clipboard.setClipEntry(
                                androidx.compose.ui.platform.ClipEntry(android.content.ClipData.newPlainText("vulnerability", text))
                            )
                            onCopied()
                        }
                    }
                )
                .padding(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .width(4.dp)
                    .height(if (aiInsight != null) 80.dp else 56.dp)
                    .background(color)
            ) { }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.file.split("/").last(),
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        modifier = Modifier.weight(1f)
                    )
                    aiInsight?.let {
                        AiBadge(
                            riskScore = it.aiRiskScore,
                            severity = it.aiSeverity,
                        )
                    }
                }
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
                aiInsight?.aiRecommendation?.let { rec ->
                    Text(
                        text = rec,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 4.dp),
                        maxLines = 2
                    )
                }
            }
        }
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
