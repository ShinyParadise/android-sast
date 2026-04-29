package dev.shinyparadise.sast.data

import java.io.File

class SmaliCandidateExtractor(private val baseDir: File) {

    fun extractCandidates(maxCandidates: Int = MAX_CANDIDATES): List<SmaliCodeSlice> {
        val smaliRoot = File(baseDir, "smali").takeIf { it.exists() } ?: baseDir
        val candidates = mutableListOf<SmaliCodeSlice>()

        smaliRoot.walkTopDown()
            .filter { it.isFile && it.extension == "smali" }
            .forEach { file ->
                if (candidates.size >= maxCandidates) return@forEach

                candidates += extractFromFile(file, maxCandidates - candidates.size)
            }

        return candidates
    }

    private fun extractFromFile(file: File, remaining: Int): List<SmaliCodeSlice> {
        val lines = runCatching { file.readLines() }.getOrElse { return emptyList() }
        val result = mutableListOf<SmaliCodeSlice>()
        val className = lines.firstOrNull { it.startsWith(".class") }?.substringAfterLast(' ')
        var methodStart = -1
        var methodName: String? = null

        lines.forEachIndexed { index, line ->
            when {
                line.startsWith(".method") -> {
                    methodStart = index
                    methodName = line.substringAfter(".method").trim()
                }

                line.startsWith(".end method") && methodStart >= 0 -> {
                    val methodLines = lines.subList(methodStart, index + 1)
                    val signals = detectSignals(methodLines)
                    if (signals.isNotEmpty()) {
                        result += SmaliCodeSlice(
                            file = file.relativeTo(baseDir).path,
                            className = className,
                            methodName = methodName,
                            startLine = methodStart + 1,
                            endLine = index + 1,
                            signals = signals,
                            code = methodLines.take(MAX_LINES_PER_SLICE).joinToString("\n"),
                        )
                    }

                    methodStart = -1
                    methodName = null

                    if (result.size >= remaining) return result
                }
            }
        }

        return result
    }

    private fun detectSignals(lines: List<String>): List<String> {
        val text = lines.joinToString("\n")
        return SIGNALS.mapNotNull { (name, patterns) ->
            name.takeIf { patterns.any { pattern -> text.contains(pattern, ignoreCase = true) } }
        }
    }

    companion object {
        private const val MAX_CANDIDATES = 80
        private const val MAX_LINES_PER_SLICE = 120

        private val SIGNALS = mapOf(
            "webview_bridge" to listOf("addJavascriptInterface", "setAllowFileAccess", "setAllowUniversalAccessFromFileURLs"),
            "network_tls" to listOf("HostnameVerifier", "X509TrustManager", "checkServerTrusted", "setHostnameVerifier"),
            "cleartext_network" to listOf("Ljava/net/HttpURLConnection;", "http://"),
            "crypto" to listOf("Ljavax/crypto/Cipher;->getInstance", "Ljava/security/MessageDigest;->getInstance", "Ljava/security/SecureRandom;"),
            "storage" to listOf("SharedPreferences", "getExternalStorageDirectory", "openFileOutput", "SQLiteDatabase"),
            "logging" to listOf("Landroid/util/Log;", "Ljava/lang/System;->out"),
            "dynamic_code" to listOf("DexClassLoader", "PathClassLoader", "loadClass", "Ljava/lang/reflect/Method;->invoke"),
            "process_exec" to listOf("Ljava/lang/Runtime;->exec", "Ljava/lang/ProcessBuilder;"),
        )
    }
}
