package dev.shinyparadise.sast.domain

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonSyntaxException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface ApiService {
    @POST("v1/chat/completions")
    suspend fun analyze(
        @Header("Authorization") authorization: String,
        @Body request: AnalysisRequest
    ): AnalysisResponse
}

data class AnalysisRequest(
    val model: String,
    val messages: List<Message>,
    val temperature: Float = 0.3f,
    val max_tokens: Int = 4096,
)

data class Message(
    val role: String,
    val content: String,
)

data class AnalysisResponse(
    val choices: List<Choice>,
    val usage: Usage? = null,
)

data class Choice(
    val message: Message,
    val finish_reason: String? = null,
)

data class Usage(
    val prompt_tokens: Int? = null,
    val completion_tokens: Int? = null,
    val total_tokens: Int? = null,
)

class RemoteAIAnalyzer(
    private val baseUrl: String,
    private val apiKey: String,
    private val model: String,
) : VulnerabilityAIAnalyzer {
    private val gson = Gson()
    private val api: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(baseUrl)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }

    override suspend fun analyzeVulnerabilities(
        vulnerabilities: List<Vulnerability>
    ): Result<List<VulnerabilityWithAIInsight>> {
        return try {
            if (vulnerabilities.isEmpty()) {
                return Result.success(emptyList())
            }

            val insights = mutableListOf<VulnerabilityWithAIInsight>()
            val chunkSize = 20

            vulnerabilities.chunked(chunkSize).forEach { chunk ->
                val prompt = buildSecurityPrompt(chunk)
                val request = AnalysisRequest(
                    model = model,
                    messages = listOf(
                        Message("system", SYSTEM_PROMPT),
                        Message("user", prompt)
                    )
                )
                val response = api.analyze("Bearer $apiKey", request)
                val chunkInsights = parseResponse(response, chunk)
                insights.addAll(chunkInsights)
            }

            Result.success(insights)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun buildSecurityPrompt(vulnerabilities: List<Vulnerability>): String {
        val vulnList = vulnerabilities.mapIndexed { index, vuln ->
            """
            |${index + 1}. File: ${vuln.file}
            |   Line: ${vuln.line}
            |   Type: ${vuln.type ?: "Unknown"}
            |   Description: ${vuln.description}
            """.trimMargin()
        }.joinToString("\n")

        return """
            |Analyze the following Android application security vulnerabilities and provide AI insights.
            |
            |Vulnerabilities:
            |${vulnList}
            |
            |For each vulnerability, respond with a JSON object containing:
            |{
            |  "file": "filename.smali",
            |  "line": 123,
            |  "risk_score": 0.0-1.0,
            |  "severity": "LOW|MEDIUM|HIGH|CRITICAL",
            |  "recommendation": "Security recommendation text"
            |}
            |
            |Respond ONLY with a JSON array of objects, no other text.
        """.trimMargin()
    }

    private fun parseResponse(
        response: AnalysisResponse,
        vulnerabilities: List<Vulnerability>
    ): List<VulnerabilityWithAIInsight> {
        val content = response.choices.firstOrNull()?.message?.content ?: return vulnerabilities.map {
            VulnerabilityWithAIInsight(
                originalVulnerability = it,
                aiRiskScore = 0.5f,
                aiSeverity = "LOW",
                aiRecommendation = "Unable to analyze"
            )
        }

        return try {
            val jsonArray = gson.fromJson(content.trim(), JsonArray::class.java)
            val insights = mutableListOf<VulnerabilityWithAIInsight>()

            for (i in 0 until jsonArray.size()) {
                val obj = jsonArray.get(i).asJsonObject
                val file = obj.get("file")?.asString ?: vulnerabilities.getOrNull(i)?.file ?: "unknown"
                val line = obj.get("line")?.asInt ?: vulnerabilities.getOrNull(i)?.line ?: 0
                val riskScore = obj.get("risk_score")?.asFloat ?: 0.5f
                val severity = obj.get("severity")?.asString ?: "LOW"
                val recommendation = obj.get("recommendation")?.asString ?: "Review this vulnerability"

                val originalVuln = vulnerabilities.find { it.file.contains(file) && it.line == line }
                    ?: vulnerabilities.getOrNull(i)
                    ?: Vulnerability(file = file, line = line, description = "")

                insights.add(
                    VulnerabilityWithAIInsight(
                        originalVulnerability = originalVuln,
                        aiRiskScore = riskScore.coerceIn(0f, 1f),
                        aiSeverity = severity,
                        aiRecommendation = recommendation
                    )
                )
            }

            insights
        } catch (e: JsonSyntaxException) {
            vulnerabilities.map {
                VulnerabilityWithAIInsight(
                    originalVulnerability = it,
                    aiRiskScore = 0.5f,
                    aiSeverity = "LOW",
                    aiRecommendation = "Parsing failed - review manually"
                )
            }
        }
    }

    companion object {
        private const val SYSTEM_PROMPT = """
            You are a security expert specializing in Android application security analysis.
            Your task is to analyze vulnerability findings from Android APK analysis (smali code) and provide:
            - Risk score (0.0-1.0): How likely this vulnerability can be exploited
            - Severity (LOW/MEDIUM/HIGH/CRITICAL): Impact assessment
            - Recommendation: Specific, actionable security fix for Android

            Focus on these security categories (let the model determine the category):
            - Hardcoded secrets/credentials/API keys
            - Insecure cryptographic practices (weak algorithms, hardcoded keys)
            - WebView security issues (JavaScript execution, file access)
            - Data storage vulnerabilities (insecure file storage, logs)
            - Network security issues (cleartext traffic, certificate validation)
            - Permission abuse (unnecessary permissions)
            - Intent/activity leakage (exported components)
            - Authentication bypass vulnerabilities

            Provide specific, actionable recommendations using Android best practices:
            - Android Keystore for key storage
            - EncryptedSharedPreferences for sensitive data
            - AES-GCM or ChaCha20 for encryption
            - Proper permission checking
            - WebView security settings
        """
    }
}