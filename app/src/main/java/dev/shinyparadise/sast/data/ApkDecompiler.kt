package dev.shinyparadise.sast.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jf.baksmali.Baksmali
import org.jf.baksmali.BaksmaliOptions
import org.jf.dexlib2.DexFileFactory
import org.jf.dexlib2.Opcodes
import java.io.File
import java.util.zip.ZipFile

class ApkDecompiler(private val context: Context) {

    fun decompile(apkFile: File): File {
        val outputDir = File(context.cacheDir, "smali_output")
        outputDir.deleteRecursively()
        outputDir.mkdirs()

        // Распаковываем APK
        ZipFile(apkFile).use { zip ->
            zip.entries().asSequence().forEach { entry ->
                if (entry.name.startsWith("classes") && entry.name.endsWith(".dex")) {
                    val dexFile = File(outputDir, entry.name)
                    zip.getInputStream(entry).use { input ->
                        dexFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                    decompileDex(dexFile, outputDir)
                    dexFile.delete()
                }
            }
        }

        return outputDir
    }

    private fun decompileDex(dexFile: File, outputDir: File) {
        val options = BaksmaliOptions().apply {
            apiLevel = 31
        }

        val dexBackedDexFile = DexFileFactory.loadDexFile(dexFile, Opcodes.forApi(options.apiLevel))
        val smaliDir = File(outputDir, dexFile.nameWithoutExtension)
        smaliDir.mkdirs()

        Baksmali.disassembleDexFile(
            dexBackedDexFile,
            smaliDir,
            Runtime.getRuntime().availableProcessors(),
            options,
        )
    }

    suspend fun cleanup() = withContext(Dispatchers.IO) {
        context.cacheDir.deleteRecursively()
    }
}
