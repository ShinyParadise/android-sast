package dev.shinyparadise.sast.domain

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonSyntaxException
import com.google.mlkit.genai.common.GenAiException
import com.google.mlkit.genai.prompt.GenerativeModel
import com.google.mlkit.genai.prompt.TextPart
import com.google.mlkit.genai.prompt.generateContentRequest
import kotlinx.coroutines.delay
import kotlin.math.pow

class AICoreAnalyzer(
    private val deviceCapabilities: DeviceCapabilities,
    private val fallbackAnalyzer: VulnerabilityAIAnalyzer,
) : VulnerabilityAIAnalyzer {
    private val gson = Gson()

    override suspend fun analyzeVulnerabilities(
        vulnerabilities: List<Vulnerability>
    ): Result<List<VulnerabilityWithAIInsight>> {
        if (vulnerabilities.isEmpty()) {
            return Result.success(emptyList())
        }

        val availability = deviceCapabilities.getAICoreAvailability()
        if (!availability.isAvailable) {
            return fallbackAnalyzer.analyzeVulnerabilities(vulnerabilities)
        }

        return try {
            val model = deviceCapabilities.createPromptModel()
            try {
                runCatching { model.warmup() }

                val insights = vulnerabilities.map { vulnerability ->
                    analyzeSingleVulnerability(model, vulnerability).getOrElse {
                        fallbackAnalyzer.analyzeVulnerabilities(listOf(vulnerability))
                            .getOrElse { listOf(buildHeuristicInsight(vulnerability)) }
                            .firstOrNull()
                            ?: buildHeuristicInsight(vulnerability)
                    }
                }

                Result.success(insights)
            } finally {
                model.close()
            }
        } catch (e: Exception) {
            fallbackAnalyzer.analyzeVulnerabilities(vulnerabilities)
        }
    }

    private suspend fun analyzeSingleVulnerability(
        model: GenerativeModel,
        vulnerability: Vulnerability,
    ): Result<VulnerabilityWithAIInsight> {
        repeat(MAX_ATTEMPTS) { attempt ->
            try {
                val request = generateContentRequest(TextPart(buildPrompt(vulnerability))) {
                    temperature = 0.2f
                    topK = 10
                    candidateCount = 1
                    maxOutputTokens = 256
                }
                val response = model.generateContent(request)
                val text = response.candidates.firstOrNull()?.text.orEmpty()

                return Result.success(parseResponse(text, vulnerability))
            } catch (e: GenAiException) {
                if (e.errorCode != GenAiException.ErrorCode.BUSY || attempt == MAX_ATTEMPTS - 1) {
                    return Result.failure(e)
                }
                delay(backoffDelayMs(attempt))
            } catch (e: Exception) {
                return Result.failure(e)
            }
        }

        return Result.failure(IllegalStateException("AICore analysis retries exhausted"))
    }

    private fun buildPrompt(vulnerability: Vulnerability): String {
        return """
            |You are an Android application security reviewer.
            |Analyze one smali finding and respond only as compact JSON:
            |{"risk_score":0.0,"severity":"LOW","recommendation":"short fix"}
            |
            |Rules:
            |- risk_score must be 0.0 to 1.0
            |- severity must be LOW, MEDIUM, HIGH, or CRITICAL
            |- recommendation must be actionable and under 160 characters
            |
            |Finding:
            |file=${vulnerability.file}
            |line=${vulnerability.line}
            |type=${vulnerability.type ?: "Unknown"}
            |description=${vulnerability.description.take(MAX_DESCRIPTION_LENGTH)}
        """.trimMargin()
    }

    private fun parseResponse(
        responseText: String,
        vulnerability: Vulnerability,
    ): VulnerabilityWithAIInsight {
        val jsonText = responseText
            .trim()
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()

        return try {
            val obj = gson.fromJson(jsonText, JsonObject::class.java)
            VulnerabilityWithAIInsight(
                originalVulnerability = vulnerability,
                aiRiskScore = obj.get("risk_score")?.asFloat?.coerceIn(0f, 1f) ?: 0.5f,
                aiSeverity = obj.get("severity")?.asString?.uppercase() ?: "LOW",
                aiRecommendation = obj.get("recommendation")?.asString ?: "Review this vulnerability in context",
            )
        } catch (e: JsonSyntaxException) {
            buildHeuristicInsight(vulnerability)
        } catch (e: IllegalStateException) {
            buildHeuristicInsight(vulnerability)
        }
    }

    private fun buildHeuristicInsight(vulnerability: Vulnerability): VulnerabilityWithAIInsight {
        val (riskScore, severity, recommendation) = when (vulnerability.type) {
            "HARDCODED_SECRET" -> Triple(0.9f, "HIGH", "Use Android Keystore for sensitive data storage")
            "INSECURE_CRYPTO" -> Triple(0.8f, "HIGH", "Migrate to AES-GCM or ChaCha20")
            "WEBVIEW_JS_ENABLED" -> Triple(0.6f, "MEDIUM", "Disable JavaScript if not required or validate all inputs")
            else -> Triple(0.5f, "LOW", "Review this vulnerability in context")
        }

        return VulnerabilityWithAIInsight(
            originalVulnerability = vulnerability,
            aiRiskScore = riskScore,
            aiSeverity = severity,
            aiRecommendation = recommendation,
        )
    }

    private fun backoffDelayMs(attempt: Int): Long {
        return (BASE_BACKOFF_MS * 2.0.pow(attempt.toDouble())).toLong()
    }

    companion object {
        private const val MAX_ATTEMPTS = 3
        private const val BASE_BACKOFF_MS = 500L
        private const val MAX_DESCRIPTION_LENGTH = 1_500
    }
}
