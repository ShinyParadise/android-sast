package dev.shinyparadise.sast.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files

class SmaliCandidateExtractorTest {

    @Test
    fun `extractCandidates returns method slice with detected signals`() {
        val baseDir = Files.createTempDirectory("smali-candidates").toFile()
        val smaliFile = baseDir.resolve("smali/com/example/Network.smali").apply {
            checkNotNull(parentFile).mkdirs()
            writeText(
                """
                .class public Lcom/example/Network;
                .super Ljava/lang/Object;

                .method public connect()V
                    const-string v0, "http://example.com"
                    invoke-static {}, Ljava/net/HttpURLConnection;->getFollowRedirects()Z
                    sget-object v1, Ljava/lang/System;->out:Ljava/io/PrintStream;
                    return-void
                .end method
                """.trimIndent()
            )
        }

        val slices = SmaliCandidateExtractor(baseDir).extractCandidates()

        assertEquals(1, slices.size)
        val slice = slices.single()
        assertEquals(smaliFile.relativeTo(baseDir).path, slice.file)
        assertEquals("Lcom/example/Network;", slice.className)
        assertEquals("public connect()V", slice.methodName)
        assertEquals(4, slice.startLine)
        assertEquals(9, slice.endLine)
        assertTrue(slice.signals.contains("cleartext_network"))
        assertTrue(slice.signals.contains("logging"))
    }

    @Test
    fun `extractCandidates respects max candidates limit`() {
        val baseDir = Files.createTempDirectory("smali-candidates").toFile()
        baseDir.resolve("Sample.smali").writeText(
            """
            .class public LSample;
            .method public first()V
                invoke-static {}, Ljava/lang/Runtime;->exec()V
            .end method
            .method public second()V
                invoke-static {}, Ljava/lang/ProcessBuilder;->new()V
            .end method
            """.trimIndent()
        )

        val slices = SmaliCandidateExtractor(baseDir).extractCandidates(maxCandidates = 1)

        assertEquals(1, slices.size)
        assertEquals("public first()V", slices.single().methodName)
        assertEquals(listOf("process_exec"), slices.single().signals)
    }
}
