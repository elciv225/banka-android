package com.banka.app.data.csv

import android.content.Context
import android.net.Uri
import java.io.File
import java.nio.charset.Charset
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets

data class CsvLoad(
    val originalName: String,
    val savedFile: File,
    val parsed: ParsedCsv,
)

class ImportRepository(private val context: Context) {

    fun accountDir(accountId: Long): File =
        File(context.filesDir, "accounts/$accountId").apply { mkdirs() }

    fun listFiles(accountId: Long): List<File> =
        accountDir(accountId).listFiles { f -> f.isFile }?.sortedBy { it.name }.orEmpty()

    fun load(accountId: Long, uri: Uri, displayName: String): CsvLoad? {
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return null
        if (bytes.isEmpty()) return null
        val text = decode(bytes)
        val parsed = CsvParser.parse(text)
        if (parsed.rows.isEmpty()) return null
        val safeName = displayName.ifBlank { "import.csv" }
            .replace(Regex("[^A-Za-z0-9._ -]"), "_")
        val dir = accountDir(accountId)
        var target = File(dir, safeName)
        var index = 1
        while (target.exists()) {
            val dot = safeName.lastIndexOf('.')
            val base = if (dot > 0) safeName.substring(0, dot) else safeName
            val ext = if (dot > 0) safeName.substring(dot) else ""
            target = File(dir, "$base ($index)$ext")
            index++
        }
        bytes.inputStream().use { input -> target.outputStream().use { input.copyTo(it) } }
        return CsvLoad(originalName = safeName, savedFile = target, parsed = parsed)
    }

    fun delete(file: File): Boolean = file.delete()

    private fun decode(bytes: ByteArray): String {
        val utf8 = try {
            StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(java.nio.ByteBuffer.wrap(bytes))
                .toString()
        } catch (_: java.nio.charset.CharacterCodingException) {
            val cp1252 = Charset.forName("windows-1252")
            String(bytes, cp1252)
        }
        return utf8.removePrefix("\uFEFF")
    }
}
