package dev.shinyparadise.sast.domain

class FallbackAIAnalyzer(
    private val analyzers: List<VulnerabilityAIAnalyzer>,
) : VulnerabilityAIAnalyzer {
    override suspend fun analyzeVulnerabilities(
        vulnerabilities: List<Vulnerability>
    ): Result<List<VulnerabilityWithAIInsight>> {
        var lastError: Throwable? = null

        analyzers.forEach { analyzer ->
            val result = analyzer.analyzeVulnerabilities(vulnerabilities)
            if (result.isSuccess) {
                return result
            }
            lastError = result.exceptionOrNull()
        }

        return Result.failure(lastError ?: IllegalStateException("No AI analyzers configured"))
    }
}
