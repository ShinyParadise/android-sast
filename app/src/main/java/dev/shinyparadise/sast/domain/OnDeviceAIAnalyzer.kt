package dev.shinyparadise.sast.domain

import kotlinx.coroutines.delay

class OnDeviceAIAnalyzer : VulnerabilityAIAnalyzer {
    override suspend fun analyzeVulnerabilities(
        vulnerabilities: List<Vulnerability>
    ): Result<List<VulnerabilityWithAIInsight>> {
        return try {
            val insights = vulnerabilities.map { vuln ->
                val (riskScore, severity, recommendation) = when (vuln.type) {
                    "HARDCODED_SECRET" -> Triple(0.9f, "HIGH", "Use Android Keystore for sensitive data storage")
                    "INSECURE_CRYPTO" -> Triple(0.8f, "HIGH", "Migrate to modern algorithms like AES-GCM or ChaCha20")
                    "WEBVIEW_JS_ENABLED" -> Triple(0.6f, "MEDIUM", "Disable JavaScript in WebView if not required, or implement proper input validation")
                    else -> Triple(0.5f, "LOW", "Review this vulnerability in context of your application")
                }
                VulnerabilityWithAIInsight(
                    originalVulnerability = vuln,
                    aiRiskScore = riskScore,
                    aiSeverity = severity,
                    aiRecommendation = recommendation
                )
            }
            Result.success(insights)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
