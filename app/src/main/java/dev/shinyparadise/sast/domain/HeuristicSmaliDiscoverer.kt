package dev.shinyparadise.sast.domain

import dev.shinyparadise.sast.data.SmaliCandidateExtractor
import dev.shinyparadise.sast.data.SmaliCodeSlice
import java.io.File

class HeuristicSmaliDiscoverer : SmaliVulnerabilityDiscoverer {

    override suspend fun discoverVulnerabilities(baseDir: File): Result<List<Vulnerability>> {
        return runCatching {
            SmaliCandidateExtractor(baseDir)
                .extractCandidates()
                .mapNotNull(::discoverFromSlice)
        }
    }

    private fun discoverFromSlice(slice: SmaliCodeSlice): Vulnerability? {
        val code = slice.code

        return when {
            "network_tls" in slice.signals && code.contains("return v", ignoreCase = true) -> Vulnerability(
                file = slice.file,
                line = slice.startLine,
                type = "INSECURE_TLS_VALIDATION",
                description = "Potential custom TLS validation or hostname verification bypass in ${slice.methodName ?: "method"}",
            )

            "webview_bridge" in slice.signals && code.contains("addJavascriptInterface", ignoreCase = true) -> Vulnerability(
                file = slice.file,
                line = slice.startLine,
                type = "WEBVIEW_JS_INTERFACE",
                description = "Potential JavaScript bridge exposure in WebView; verify origin restrictions and exposed methods",
            )

            "cleartext_network" in slice.signals && code.contains("http://", ignoreCase = true) -> Vulnerability(
                file = slice.file,
                line = slice.startLine,
                type = "CLEARTEXT_NETWORK",
                description = "Potential cleartext HTTP endpoint usage in smali slice",
            )

            "process_exec" in slice.signals -> Vulnerability(
                file = slice.file,
                line = slice.startLine,
                type = "PROCESS_EXECUTION",
                description = "Potential command execution path; verify input sanitization and necessity",
            )

            "dynamic_code" in slice.signals -> Vulnerability(
                file = slice.file,
                line = slice.startLine,
                type = "DYNAMIC_CODE_LOADING",
                description = "Potential dynamic code loading or reflection path; verify code source trust boundaries",
            )

            "logging" in slice.signals && SENSITIVE_WORDS.any { code.contains(it, ignoreCase = true) } -> Vulnerability(
                file = slice.file,
                line = slice.startLine,
                type = "SENSITIVE_LOGGING",
                description = "Potential sensitive value logging detected near Android Log/System output usage",
            )

            "storage" in slice.signals && SENSITIVE_WORDS.any { code.contains(it, ignoreCase = true) } -> Vulnerability(
                file = slice.file,
                line = slice.startLine,
                type = "INSECURE_STORAGE",
                description = "Potential sensitive data storage; verify encryption and storage location",
            )

            else -> null
        }
    }

    companion object {
        private val SENSITIVE_WORDS = listOf("password", "token", "secret", "credential", "api_key", "auth")
    }
}
