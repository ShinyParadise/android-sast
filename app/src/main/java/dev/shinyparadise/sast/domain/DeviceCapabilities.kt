package dev.shinyparadise.sast.domain

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import com.google.mlkit.genai.common.FeatureStatus
import com.google.mlkit.genai.prompt.Generation
import com.google.mlkit.genai.prompt.GenerativeModel

data class AICoreAvailability(
    val isAvailable: Boolean,
    val status: Int? = null,
    val baseModelName: String? = null,
    val deviceModel: String = "${Build.MANUFACTURER} ${Build.MODEL}",
    val memoryClassMb: Int = 0,
    val largeMemoryClassMb: Int = 0,
    val reason: String? = null,
)

class DeviceCapabilities(private val context: Context) {
    private val activityManager: ActivityManager? =
        context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager

    fun createPromptModel(): GenerativeModel = Generation.getClient()

    suspend fun getAICoreAvailability(): AICoreAvailability {
        val memoryClassMb = activityManager?.memoryClass ?: 0
        val largeMemoryClassMb = activityManager?.largeMemoryClass ?: 0

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return AICoreAvailability(
                isAvailable = false,
                memoryClassMb = memoryClassMb,
                largeMemoryClassMb = largeMemoryClassMb,
                reason = "ML Kit Prompt API requires Android 8.0+",
            )
        }

        return try {
            val model = createPromptModel()
            try {
                val status = model.checkStatus()
                val baseModelName = runCatching { model.getBaseModelName() }.getOrNull()

                AICoreAvailability(
                    isAvailable = status == FeatureStatus.AVAILABLE,
                    status = status,
                    baseModelName = baseModelName,
                    memoryClassMb = memoryClassMb,
                    largeMemoryClassMb = largeMemoryClassMb,
                    reason = when (status) {
                        FeatureStatus.AVAILABLE -> null
                        FeatureStatus.DOWNLOADABLE -> "AICore model is downloadable but not ready"
                        FeatureStatus.DOWNLOADING -> "AICore model is still downloading"
                        FeatureStatus.UNAVAILABLE -> "AICore Prompt API is unavailable on this device"
                        else -> "Unknown AICore status: $status"
                    },
                )
            } finally {
                model.close()
            }
        } catch (e: Exception) {
            AICoreAvailability(
                isAvailable = false,
                memoryClassMb = memoryClassMb,
                largeMemoryClassMb = largeMemoryClassMb,
                reason = e.message ?: e::class.java.simpleName,
            )
        }
    }
}
