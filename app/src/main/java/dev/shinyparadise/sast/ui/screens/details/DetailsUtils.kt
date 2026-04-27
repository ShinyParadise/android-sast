package dev.shinyparadise.sast.ui.screens.details

import androidx.compose.ui.graphics.Color
import dev.shinyparadise.sast.domain.AnalysisReport
import dev.shinyparadise.sast.domain.Vulnerability

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
