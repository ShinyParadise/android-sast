package dev.shinyparadise.sast.domain

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

@Serializable
data class AnalysisReport(
    val apkPath: String,
    val vulnerabilities: List<Vulnerability>,
    val summary: String,
    @Contextual
    val aiInsights: List<VulnerabilityWithAIInsight>? = null
)
