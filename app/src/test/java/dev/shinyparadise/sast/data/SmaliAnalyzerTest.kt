package dev.shinyparadise.sast.data

import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files

class SmaliAnalyzerTest {

    @Test
    fun `analyze reports hardcoded secret with relative file and line`() = runBlocking {
        val baseDir = Files.createTempDirectory("smali-analyzer").toFile()
        val smaliFile = baseDir.resolve("com/example/Auth.smali").apply {
            checkNotNull(parentFile).mkdirs()
            writeText(
                """
                .class public Lcom/example/Auth;
                const-string v0, "api_key=123456"
                return-void
                """.trimIndent()
            )
        }

        val findings = SmaliAnalyzer(baseDir).analyze().toList()

        assertEquals(1, findings.size)
        assertEquals("HARDCODED_SECRET", findings.single().type)
        assertEquals(smaliFile.relativeTo(baseDir).path, findings.single().file)
        assertEquals(2, findings.single().line)
        assertTrue(findings.single().description.contains("api_key=123456"))
    }

    @Test
    fun `analyze reports crypto and webview findings`() = runBlocking {
        val baseDir = Files.createTempDirectory("smali-analyzer").toFile()
        baseDir.resolve("Risky.smali").writeText(
            """
            .class public LRisky;
            invoke-static {v0}, Ljavax/crypto/Cipher;->getInstance(Ljava/lang/String;)Ljavax/crypto/Cipher; # DES
            invoke-virtual {v0, v1}, Landroid/webkit/WebView;->setJavaScriptEnabled(true)V
            """.trimIndent()
        )

        val types = SmaliAnalyzer(baseDir).analyze().toList().map { it.type }

        assertEquals(listOf("INSECURE_CRYPTO", "WEBVIEW_JS_ENABLED"), types)
    }
}
