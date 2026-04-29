package dev.shinyparadise.sast.domain

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonSyntaxException
import com.google.mlkit.genai.common.GenAiException
import com.google.mlkit.genai.prompt.GenerativeModel
import com.google.mlkit.genai.prompt.TextPart
import com.google.mlkit.genai.prompt.generateContentRequest
import dev.shinyparadise.sast.data.SmaliCandidateExtractor
import dev.shinyparadise.sast.data.SmaliCodeSlice
import kotlinx.coroutines.delay
import java.io.File
import kotlin.math.pow

class AICoreSmaliDiscoverer(
    private val deviceCapabilities: DeviceCapabilities,
    private val fallbackDiscoverer: SmaliVulnerabilityDiscoverer,
) : SmaliVulnerabilityDiscoverer {
    private val gson = Gson()

    override suspend fun discoverVulnerabilities(baseDir: File): Result<List<Vulnerability>> {
        val candidates = SmaliCandidateExtractor(baseDir).extractCandidates(MAX_CANDIDATES)
        if (candidates.isEmpty()) return Result.success(emptyList())

        val fallbackFindings = fallbackDiscoverer.discoverVulnerabilities(baseDir).getOrElse { emptyList() }
        val availability = deviceCapabilities.getAICoreAvailability()
        if (!availability.isAvailable) return Result.success(fallbackFindings)

        return try {
            val model = deviceCapabilities.createPromptModel()
            try {
                runCatching { model.warmup() }

                val aiFindings = candidates.mapNotNull { candidate ->
                    discoverFromCandidate(model, candidate).getOrNull()
                }

                Result.success(fallbackFindings + aiFindings)
            } finally {
                model.close()
            }
        } catch (e: Exception) {
            Result.success(fallbackFindings)
        }
    }

    private suspend fun discoverFromCandidate(
        model: GenerativeModel,
        candidate: SmaliCodeSlice,
    ): Result<Vulnerability?> {
        repeat(MAX_ATTEMPTS) { attempt ->
            try {
                val request = generateContentRequest(TextPart(buildPrompt(candidate))) {
                    temperature = 0.1f
                    topK = 10
                    candidateCount = 1
                    maxOutputTokens = 256
                }
                val response = model.generateContent(request)
                val text = response.candidates.firstOrNull()?.text.orEmpty()

                return Result.success(parseResponse(text, candidate))
            } catch (e: GenAiException) {
                if (e.errorCode != GenAiException.ErrorCode.BUSY || attempt == MAX_ATTEMPTS - 1) {
                    return Result.failure(e)
                }
                delay(backoffDelayMs(attempt))
            } catch (e: Exception) {
                return Result.failure(e)
            }
        }

        return Result.failure(IllegalStateException("AICore smali discovery retries exhausted"))
    }

    private fun buildPrompt(candidate: SmaliCodeSlice): String {
        return """
            |You are an Android security reviewer analyzing one smali method slice.
            |Return only compact JSON. If there is no supported vulnerability, return {}.
            |Schema: {"type":"TYPE","description":"short evidence-based finding","line":123,"confidence":0.0}
            |
            |Rules:
            |- Report only vulnerabilities directly supported by this code.
            |- Use uppercase snake_case type.
            |- confidence must be 0.0 to 1.0.
            |- description must be under 180 characters.
            |- line must be within the provided slice line range.
            |
            |File: ${candidate.file}
            |Class: ${candidate.className ?: "unknown"}
            |Method: ${candidate.methodName ?: "unknown"}
            |Lines: ${candidate.startLine}-${candidate.endLine}
            |Signals: ${candidate.signals.joinToString()}
            |Code:
            |${candidate.code.take(MAX_CODE_CHARS)}
        """.trimMargin()
    }

    private fun parseResponse(responseText: String, candidate: SmaliCodeSlice): Vulnerability? {
        val jsonText = responseText
            .trim()
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()

        if (jsonText.isBlank() || jsonText == "{}") return null

        return try {
            val obj = gson.fromJson(jsonText, JsonObject::class.java) ?: return null
            val type = obj.get("type")?.asString?.trim()?.uppercase().orEmpty()
            val description = obj.get("description")?.asString?.trim().orEmpty()
            val confidence = obj.get("confidence")?.asFloat ?: 0f
            val line = obj.get("line")?.asInt ?: candidate.startLine

            if (type.isBlank() || description.isBlank()) return null
            if (confidence < MIN_CONFIDENCE) return null

            Vulnerability(
                file = candidate.file,
                line = line.coerceIn(candidate.startLine, candidate.endLine),
                type = type,
                description = "AI-discovered: $description",
            )
        } catch (e: JsonSyntaxException) {
            null
        } catch (e: IllegalStateException) {
            null
        } catch (e: NumberFormatException) {
            null
        }
    }

    private fun backoffDelayMs(attempt: Int): Long {
        return (BASE_BACKOFF_MS * 2.0.pow(attempt.toDouble())).toLong()
    }

    companion object {
        private const val MAX_CANDIDATES = 40
        private const val MAX_ATTEMPTS = 2
        private const val BASE_BACKOFF_MS = 500L
        private const val MAX_CODE_CHARS = 6_000
        private const val MIN_CONFIDENCE = 0.65f
    }
}
