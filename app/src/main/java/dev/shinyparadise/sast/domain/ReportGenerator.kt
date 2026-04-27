package dev.shinyparadise.sast.domain

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

sealed class ReportResult {
    data class Success(val data: Any) : ReportResult()
    data class Error(val message: String) : ReportResult()
}

interface ReportGenerator {
    suspend fun generateTextReport(report: AnalysisReport): ReportResult
    suspend fun generateCsvReport(report: AnalysisReport): ReportResult
    suspend fun generatePdfReport(report: AnalysisReport): ReportResult
    suspend fun saveToFile(context: Context, fileName: String, content: String): ReportResult
    suspend fun savePdfToFile(context: Context, fileName: String, content: ByteArray): ReportResult
}

class ReportGeneratorImpl : ReportGenerator {

    override suspend fun generateTextReport(report: AnalysisReport): ReportResult {
        return try {
            val content = buildString {
                appendLine("SAST Analysis Report")
                appendLine("=".repeat(50))
                appendLine("APK: ${report.apkPath}")
                appendLine("Total Vulnerabilities: ${report.vulnerabilities.size}")
                appendLine()

                val grouped = report.vulnerabilities.groupBy { it.type ?: "UNKNOWN" }
                grouped.forEach { (type, vulns) ->
                    appendLine("Type: $type (${vulns.size} vulnerabilities)")
                    appendLine("-".repeat(50))
                    vulns.forEach { vuln ->
                        appendLine("  File: ${vuln.file}")
                        appendLine("  Line: ${vuln.line}")
                        appendLine("  Description: ${vuln.description}")
                        appendLine()
                    }
                }
            }
            ReportResult.Success(content)
        } catch (e: Exception) {
            ReportResult.Error("Failed to generate text report: ${e.message}")
        }
    }

    override suspend fun generateCsvReport(report: AnalysisReport): ReportResult {
        return try {
            val content = buildString {
                appendLine("File,Line,Type,Description")
                report.vulnerabilities.forEach { vuln ->
                    val desc = vuln.description.replace("\"", "\"\"")
                    appendLine("\"${vuln.file}\",${vuln.line},\"${vuln.type ?: "UNKNOWN"}\",\"$desc\"")
                }
            }
            ReportResult.Success(content)
        } catch (e: Exception) {
            ReportResult.Error("Failed to generate CSV report: ${e.message}")
        }
    }

    override suspend fun generatePdfReport(report: AnalysisReport): ReportResult {
        return try {
            val document = android.graphics.pdf.PdfDocument()

            val pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(595, 842, 1).create()
            var page = document.startPage(pageInfo)
            var canvas = page.canvas
            val paint = android.graphics.Paint()
            paint.textSize = 12f
            paint.color = android.graphics.Color.BLACK

            var yPosition = 50
            val lineHeight = 20

            // Title
            paint.textSize = 18f
            canvas.drawText("SAST Analysis Report", 50f, yPosition.toFloat(), paint)
            yPosition += lineHeight * 2

            // APK info
            paint.textSize = 12f
            canvas.drawText("APK: ${report.apkPath}", 50f, yPosition.toFloat(), paint)
            yPosition += lineHeight
            canvas.drawText("Total Vulnerabilities: ${report.vulnerabilities.size}", 50f, yPosition.toFloat(), paint)
            yPosition += lineHeight * 2

            // Group by type
            val grouped = report.vulnerabilities.groupBy { it.type ?: "UNKNOWN" }

            for ((type, vulns) in grouped) {
                if (yPosition > 750) {
                    document.finishPage(page)
                    val newPageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(595, 842, document.pages.size + 1).create()
                    page = document.startPage(newPageInfo)
                    canvas = page.canvas
                    yPosition = 50
                }

                paint.isFakeBoldText = true
                canvas.drawText("Type: $type (${vulns.size})", 50f, yPosition.toFloat(), paint)
                yPosition += lineHeight
                paint.isFakeBoldText = false

                vulns.forEach { vuln ->
                    if (yPosition > 750) {
                        document.finishPage(page)
                        val newPageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(595, 842, document.pages.size + 1).create()
                        page = document.startPage(newPageInfo)
                        canvas = page.canvas
                        yPosition = 50
                    }

                    canvas.drawText("File: ${vuln.file}", 70f, yPosition.toFloat(), paint)
                    yPosition += lineHeight
                    canvas.drawText("Line: ${vuln.line}", 70f, yPosition.toFloat(), paint)
                    yPosition += lineHeight
                    canvas.drawText("Desc: ${vuln.description.take(80)}", 70f, yPosition.toFloat(), paint)
                    yPosition += lineHeight * 2
                }
            }

            document.finishPage(page)

            val outputStream = java.io.ByteArrayOutputStream()
            document.writeTo(outputStream)
            document.close()
            ReportResult.Success(outputStream.toByteArray())
        } catch (e: Exception) {
            ReportResult.Error("Failed to generate PDF report: ${e.message}")
        }
    }

    override suspend fun saveToFile(context: Context, fileName: String, content: String): ReportResult {
        return try {
            val resolver = context.contentResolver
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "text/plain")
                put(MediaStore.MediaColumns.RELATIVE_PATH, "Documents")
            }

            val uri = resolver.insert(MediaStore.Files.getContentUri("external"), values)
            if (uri != null) {
                resolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.write(content.toByteArray())
                }
                ReportResult.Success(true)
            } else {
                ReportResult.Error("Failed to create file: $fileName")
            }
        } catch (e: Exception) {
            ReportResult.Error("Failed to save file: ${e.message}")
        }
    }

    override suspend fun savePdfToFile(context: Context, fileName: String, content: ByteArray): ReportResult {
        return try {
            val resolver = context.contentResolver
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                put(MediaStore.MediaColumns.RELATIVE_PATH, "Documents")
            }

            val uri = resolver.insert(MediaStore.Files.getContentUri("external"), values)
            if (uri != null) {
                resolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.write(content)
                }
                ReportResult.Success(true)
            } else {
                ReportResult.Error("Failed to create PDF file: $fileName")
            }
        } catch (e: Exception) {
            ReportResult.Error("Failed to save PDF file: ${e.message}")
        }
    }
}
