package com.banka.app.data.csv

import java.text.Normalizer
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale

data class ParsedRow(
    val date: Long,
    val label: String,
    val amount: Double,
)

data class ParsedCsv(
    val separator: Char,
    val header: List<String>?,
    val rows: List<ParsedRow>,
    val skippedRows: Int,
    val totalRows: Int,
)

data class ColumnMapping(
    val dateColumn: Int,
    val labelColumn: Int,
    val amountColumn: Int,
    val debitColumn: Int? = null,
    val creditColumn: Int? = null,
)

object CsvParser {

    private val separators = listOf(';', ',', '\t', '|')

    private val dateKeywords = listOf("date")
    private val labelKeywords =
        listOf("libelle", "label", "operation", "designation", "description", "detail", "beneficiaire")
    private val amountKeywords = listOf("montant", "amount", "valeur", "virement")
    private val debitKeywords = listOf("debit")
    private val creditKeywords = listOf("credit")

    private val datePatterns = listOf(
        "dd/MM/yyyy", "d/M/yyyy", "dd/MM/yyyy HH:mm", "dd/MM/yyyy HH:mm:ss",
        "dd/MM/yy", "yyyy-MM-dd", "yyyy-MM-dd HH:mm", "yyyy-MM-dd HH:mm:ss",
        "dd-MM-yyyy", "dd.MM.yyyy", "d MMMM yyyy", "MMM d yyyy",
    )
    private val dateFormatters = datePatterns.map {
        DateTimeFormatter.ofPattern(it, Locale.FRENCH)
    }

    fun detectSeparator(text: String): Char {
        val headerLine = text.lineSequence().firstOrNull { it.isNotBlank() } ?: return ';'
        var best = ';'
        var bestCount = 0
        val quoted = countQuotedRegions(headerLine)
        separators.forEachIndexed { index, sep ->
            val count = countOutsideQuotes(headerLine, sep, quoted)
            if (count > bestCount || (count == bestCount && count > 0 && index == 0)) {
                best = sep
                bestCount = count
            }
        }
        return if (bestCount > 0) best else ';'
    }

    fun parseRows(text: String, separator: Char): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        val field = StringBuilder()
        var row = mutableListOf<String>()
        var inQuotes = false
        var i = 0
        while (i < text.length) {
            val c = text[i]
            when {
                inQuotes -> when {
                    c == '"' && i + 1 < text.length && text[i + 1] == '"' -> {
                        field.append('"')
                        i++
                    }

                    c == '"' -> inQuotes = false

                    else -> field.append(c)
                }

                c == '"' -> inQuotes = true

                c == separator -> {
                    row.add(field.toString())
                    field.clear()
                }

                c == '\r' -> Unit

                c == '\n' -> {
                    row.add(field.toString())
                    field.clear()
                    if (row.any { it.isNotBlank() }) rows.add(row)
                    row = mutableListOf()
                }

                else -> field.append(c)
            }
            i++
        }
        if (field.isNotEmpty() || row.isNotEmpty()) {
            row.add(field.toString())
            if (row.any { it.isNotBlank() }) rows.add(row)
        }
        return rows
    }

    fun parse(text: String): ParsedCsv {
        val separator = detectSeparator(text)
        val rows = parseRows(text, separator)
        if (rows.isEmpty()) return ParsedCsv(separator, null, emptyList(), 0, 0)

        val (mapping, header, hadHeader) = detectMapping(rows)
        var skipped = 0
        val parsed = mutableListOf<ParsedRow>()
        for (row in rows) {
            if (hadHeader && row === rows[0]) continue
            val rawDate = row.getOrNull(mapping.dateColumn)?.trim().orEmpty()
            val rawLabel = row.getOrNull(mapping.labelColumn)?.trim().orEmpty()
            val amount = resolveAmount(row, mapping)
            val timestamp = parseDate(rawDate)
            if (timestamp == null || amount == null || rawLabel.isBlank()) {
                skipped++
                continue
            }
            parsed.add(ParsedRow(timestamp, rawLabel, amount))
        }
        return ParsedCsv(separator, header, parsed, skipped, rows.size)
    }

    private fun detectMapping(rows: List<List<String>>): Triple<ColumnMapping, List<String>?, Boolean> {
        val first = rows.first()
        val normalized = first.map { normalizeCell(it) }

        fun indexOfAny(keywords: List<String>, exclude: List<Int> = emptyList()): Int? =
            normalized.indices
                .filterNot { it in exclude }
                .firstOrNull { index -> keywords.any { normalized[index].contains(it) } }

        val dateCol = indexOfAny(dateKeywords)
        val debitCol = indexOfAny(debitKeywords)
        val creditCol = indexOfAny(creditKeywords)
        val amountCol = indexOfAny(amountKeywords, listOfNotNull(dateCol, debitCol, creditCol))
        val labelCol = indexOfAny(labelKeywords, listOfNotNull(dateCol))

        if (dateCol != null && (amountCol != null || (debitCol != null && creditCol != null))) {
            val mapping = ColumnMapping(
                dateColumn = dateCol,
                labelColumn = labelCol ?: first.indices.firstOrNull { it != dateCol && it != amountCol } ?: 0,
                amountColumn = amountCol ?: -1,
                debitColumn = debitCol,
                creditColumn = creditCol,
            )
            return Triple(mapping, normalized, true)
        }

        val sniffed = sniffColumns(rows)
        return sniffed
    }

    private fun sniffColumns(rows: List<List<String>>): Triple<ColumnMapping, List<String>?, Boolean> {
        val width = rows.minOf { it.size }.coerceAtLeast(3)
        val sample = rows.take(30)
        val dateScores = DoubleArray(width)
        val numberScores = DoubleArray(width)
        val textScores = DoubleArray(width)
        for (col in 0 until width) {
            var dateHits = 0
            var numberHits = 0
            var textLen = 0
            var nonEmpty = 0
            for (row in sample) {
                val cell = row.getOrNull(col)?.trim().orEmpty()
                if (cell.isEmpty()) continue
                nonEmpty++
                if (parseDate(cell) != null) dateHits++
                if (parseAmount(cell) != null) numberHits++
                textLen += cell.length
            }
            if (nonEmpty == 0) continue
            dateScores[col] = dateHits.toDouble() / nonEmpty
            numberScores[col] = numberHits.toDouble() / nonEmpty
            textScores[col] = textLen.toDouble() / nonEmpty
        }
        val dateColumn = dateScores.withIndex().maxByOrNull { it.value }
            ?.takeIf { it.value >= 0.5 }?.index ?: 0
        val amountColumn = numberScores.withIndex().maxByOrNull { it.value }
            ?.takeIf { it.value >= 0.5 && it.index != dateColumn }?.index ?: (width - 1)
        val labelColumn = textScores.withIndex()
            .filter { it.index != dateColumn && it.index != amountColumn }
            .maxByOrNull { it.value }
            ?.takeIf { it.value > 0.0 }?.index
            ?: width.indices.firstOrNull { it != dateColumn && it != amountColumn }
            ?: maxOf(0, width - 1)
        return Triple(ColumnMapping(dateColumn, labelColumn, amountColumn), null, false)
    }

    private fun resolveAmount(row: List<String>, mapping: ColumnMapping): Double? {
        val debit = mapping.debitColumn
            ?.let { row.getOrNull(it)?.trim().orEmpty() }
            ?.takeIf { it.isNotEmpty() }
            ?.let { parseAmount(it)?.let { v -> -kotlin.math.abs(v) } }
        val credit = mapping.creditColumn
            ?.let { row.getOrNull(it)?.trim().orEmpty() }
            ?.takeIf { it.isNotEmpty() }
            ?.let { parseAmount(it)?.let { v -> kotlin.math.abs(v) } }
        if (debit != null || credit != null) return (debit ?: 0.0) + (credit ?: 0.0)
        if (mapping.amountColumn < 0) return null
        return parseAmount(row.getOrNull(mapping.amountColumn)?.trim().orEmpty())
    }

    fun parseDate(raw: String): Long? {
        val s = raw.trim()
        if (s.isEmpty()) return null
        runCatching { return Instant.parse(s).toEpochMilli() }
        runCatching { return LocalDateTime.parse(s).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() }
        runCatching { return LocalDate.parse(s).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() }
        for (formatter in dateFormatters) {
            runCatching {
                return runCatching {
                    LocalDateTime.parse(s, formatter)
                }.getOrElse {
                    LocalDate.parse(s, formatter).atStartOfDay()
                }.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            }
        }
        runCatching {
            val numeric = parseAmount(s)
            if (numeric != null && numeric > 20_000 && s.length >= 5) {
                return LocalDate.of(1899, 12, 30).plusDays(numeric.toLong())
                    .atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            }
        }
        return null
    }

    fun parseAmount(raw: String): Double? {
        var s = raw.filterNot { it.isWhitespace() || it == '\u00a0' }.replace("€", "").trim()
        if (s.isEmpty()) return null
        var negative = false
        if (s.startsWith("(") && s.endsWith(")") && s.length > 2) {
            negative = true
            s = s.substring(1, s.length - 1)
        }
        if (s.startsWith("-")) {
            negative = true
            s = s.drop(1)
        }
        if (s.endsWith("-")) {
            negative = true
            s = s.dropLast(1)
        }
        s = s.replace("+", "")
        val dotCount = s.count { it == '.' }
        val commaCount = s.count { it == ',' }
        val normalized = when {
            dotCount > 0 && commaCount > 0 ->
                if (s.lastIndexOf('.') > s.lastIndexOf(',')) s.replace(",", "")
                else s.replace(".", "").replace(',', '.')

            commaCount > 1 -> s.replace(",", "")
            commaCount == 1 -> s.replace(',', '.')
            dotCount > 1 -> s.replace(".", "")
            else -> s
        }.filter { it.isDigit() || it == '.' }
        val value = normalized.toDoubleOrNull() ?: return null
        return if (negative) -value else value
    }

    private fun normalizeCell(raw: String): String =
        normalize(raw.lowercase(Locale.FRENCH)).trim()

    internal fun normalize(value: String): String = Normalizer
        .normalize(value, Normalizer.Form.NFD)
        .replace(Regex("\\p{M}+"), "")
        .lowercase(Locale.ROOT)
        .trim()

    private fun countQuotedRegions(line: String): Int =
        line.count { it == '"' }
}

private fun countOutsideQuotes(line: String, separator: Char, quoteCount: Int): Int =
    if (quoteCount == 0) line.count { it == separator } else {
        var inQuotes = false
        var count = 0
        for (c in line) {
            when {
                c == '"' -> inQuotes = !inQuotes
                c == separator && !inQuotes -> count++
            }
        }
        count
    }
