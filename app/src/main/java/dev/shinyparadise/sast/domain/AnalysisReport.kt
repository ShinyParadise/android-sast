package dev.shinyparadise.sast.domain

import kotlinx.serialization.Serializable

@Serializable
data class AnalysisReport(
    val apkPath: String,
    val vulnerabilities: List<Vulnerability>,
    val summary: String
)
