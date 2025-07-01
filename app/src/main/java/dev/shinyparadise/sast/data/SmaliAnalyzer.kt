package dev.shinyparadise.sast.data

import dev.shinyparadise.sast.domain.Vulnerability
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.File

class SmaliAnalyzer(private val baseDir: File) {

    fun analyze(): Flow<Vulnerability> = flow {
        baseDir.walkTopDown()
            .filter { it.isFile && it.extension == "smali" }
            .forEach { smaliFile ->
                smaliFile.readLines().forEachIndexed { lineIndex, lineContent ->
                    detectIssues(lineContent, lineIndex + 1, smaliFile)?.let {
                        emit(it)
                    }
                }
            }
    }

    private fun detectIssues(
        line: String,
        lineNum: Int,
        file: File
    ): Vulnerability? {
        // Правило 1: Хардкодные секреты
        if (line.contains("const-string")
            && SECRET_KEYWORDS.any { line.contains(it, ignoreCase = true) }
        ) {
            return Vulnerability(
                type = "HARDCODED_SECRET",
                description = "Найден хардкодный секрет: ${line.trim()}",
                file = file.relativeTo(baseDir).path,
                line = lineNum,
            )
        }

        // Правило 2: Небезопасные криптоалгоритмы
        if (line.contains("Ljavax/crypto/Cipher;") &&
            INSECURE_CIPHERS.any { line.contains(it, ignoreCase = true) }) {
            return Vulnerability(
                type = "INSECURE_CRYPTO",
                description = "Использован небезопасный криптоалгоритм",
                file = file.relativeTo(baseDir).path,
                line = lineNum,
            )
        }

        // Правило 3: Неправильная работа с WebView
        if (line.contains("Landroid/webkit/WebView;") &&
            line.contains("setJavaScriptEnabled(true)")) {
            return Vulnerability(
                type = "WEBVIEW_JS_ENABLED",
                description = "Включена поддержка JavaScript в WebView без проверки",
                file = file.relativeTo(baseDir).path,
                line = lineNum,
            )
        }

        return null
    }

    companion object {
        private val SECRET_KEYWORDS = listOf("password", "secret", "key", "token", "credential", "api_key")
        private val INSECURE_CIPHERS = listOf("DES", "RC2", "RC4", "Blowfish", "PBEWithMD5AndDES")
    }
}
