package dev.shinyparadise.sast.domain

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
)

data class Message(
    val role: String,
    val content: String,
)

data class AnalysisResponse(
    val choices: List<Choice>,
)

data class Choice(
    val message: Message,
)

class RemoteAIAnalyzer(
    private val baseUrl: String,
    private val apiKey: String,
    private val model: String,
) : VulnerabilityAIAnalyzer {
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
            val prompt = buildPrompt(vulnerabilities)
            val request = AnalysisRequest(
                model = model,
                messages = listOf(
                    Message("system", "You are a security expert analyzing Android application vulnerabilities."),
                    Message("user", prompt)
                )
            )
            val response = api.analyze("Bearer $apiKey", request)
            parseResponse(response, vulnerabilities)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun buildPrompt(vulnerabilities: List<Vulnerability>): String {
        return """
            Analyze these Android app vulnerabilities and provide AI insights:
            ${vulnerabilities.mapIndexed { index, vuln ->
            "${index + 1}. File: ${vuln.file}, Line: ${vuln.line}, Type: ${vuln.type}, Description: ${vuln.description}"
        }.joinToString("\n")}
            
            For each vulnerability, provide:
            - Risk score (0.0-1.0)
            - Severity (LOW/MEDIUM/HIGH/CRITICAL)
            - Recommendation
        """.trimIndent()
    }

    private fun parseResponse(
        response: AnalysisResponse,
        vulnerabilities: List<Vulnerability>
    ): Result<List<VulnerabilityWithAIInsight>> {
        return Result.success(
            vulnerabilities.map { vuln ->
                VulnerabilityWithAIInsight(
                    originalVulnerability = vuln,
                    aiRiskScore = 0.7f,
                    aiSeverity = "MEDIUM",
                    aiRecommendation = "AI analysis pending full implementation"
                )
            }
        )
    }
}
