package dev.shinyparadise.sast.domain

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.core.net.toFile
import dev.shinyparadise.sast.data.ApkDecompiler
import dev.shinyparadise.sast.data.SmaliAnalyzer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.withContext
import java.io.File

class AnalyzerInteractor(
    private val context: Context,
    private val decompiler: ApkDecompiler,
) {

    sealed class AnalysisState {
        object Idle : AnalysisState()
        object Decompiling : AnalysisState()
        data class Analyzing(val progress: Float) : AnalysisState()
        data class Completed(val report: AnalysisReport) : AnalysisState()
        data class Error(val message: String) : AnalysisState()
    }

    private val _analysisState = MutableStateFlow<AnalysisState>(AnalysisState.Idle)
    val analysisState: StateFlow<AnalysisState> = _analysisState.asStateFlow()

    suspend fun analyzeApk(apkUri: Uri) = withContext(Dispatchers.IO) {
        try {
            val absoluteUri = getUri(apkUri)
            val apkFile = absoluteUri.toFile()

            // 1. Декомпиляция APK
            _analysisState.value = AnalysisState.Decompiling
            val decompiledDir = decompiler.decompile(apkFile)

            // 2. Анализ Smali
            val analyzer = SmaliAnalyzer(decompiledDir)
            val vulnerabilities = mutableListOf<Vulnerability>()
            var fileCount = 0
            val totalFiles = countSmaliFiles(File(decompiledDir, "smali"))

            analyzer.analyze()
                .onCompletion { cause ->
                    if (cause == null) {
                        val report = generateReport(apkFile, vulnerabilities)
                        _analysisState.value = AnalysisState.Completed(report)
                        cleanup()
                    }
                }
                .collect { vulnerability ->
                    vulnerabilities.add(vulnerability)
                    fileCount++
                    val progress = if (totalFiles > 0) fileCount.toFloat() / totalFiles else 0f
                    _analysisState.value = AnalysisState.Analyzing(progress)
                }

        } catch (e: Exception) {
            _analysisState.value = AnalysisState.Error("Ошибка анализа: ${e.message}")
            cleanup()
        }
    }

    private fun generateReport(apkFile: File, vulnerabilities: List<Vulnerability>): AnalysisReport {
        val summary = buildString {
            appendLine("Результаты анализа: ${apkFile.name}")
            appendLine("Всего уязвимостей: ${vulnerabilities.size}")

            val grouped = vulnerabilities.groupBy { it.type }
            grouped.forEach { (type, list) ->
                appendLine("$type: ${list.size}")
            }
        }

        return AnalysisReport(
            apkPath = apkFile.path,
            vulnerabilities = vulnerabilities,
            summary = summary
        )
    }

    private fun countSmaliFiles(dir: File): Int {
        return dir.walkTopDown().count { it.isFile && it.extension == "smali" }
    }

    fun getUri(uri: Uri): Uri {
        var resultURI = uri
        if (ContentResolver.SCHEME_CONTENT == uri.scheme) {
            val cr: ContentResolver = context.contentResolver
            val mimeTypeMap = MimeTypeMap.getSingleton()
            val extensionFile = mimeTypeMap.getExtensionFromMimeType(cr.getType(uri))
            val file = File.createTempFile(
                "myTempFile",
                ".$extensionFile",
                context.cacheDir
            )
            cr.openInputStream(uri)?.use { input ->
                file.outputStream().use { stream ->
                    input.copyTo(stream)
                }
            }
            resultURI = Uri.fromFile(file)
        }
        return resultURI
    }

    fun getContext(): Context = context

    suspend fun cleanup() {
        decompiler.cleanup()
    }
}
