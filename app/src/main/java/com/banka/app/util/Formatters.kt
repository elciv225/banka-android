package com.banka.app.util

import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

object Formatters {

    private val currencyFormat: NumberFormat =
        NumberFormat.getCurrencyInstance(Locale.FRANCE).apply { maximumFractionDigits = 2 }

    private val dayFormat = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.FRENCH)

    fun amount(value: Double, withSign: Boolean = false): String {
        val text = currencyFormat.format(Math.abs(value))
        return when {
            value < 0 -> "-$text"
            withSign -> "+$text"
            else -> text
        }
    }

    fun date(timestamp: Long): String =
        LocalDate.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault()).format(dayFormat)

    fun monthLabel(month: YearMonth): String =
        month.month.getDisplayName(TextStyle.FULL, Locale.FRENCH)
            .replaceFirstChar { it.uppercase(Locale.FRENCH) } + " " + month.year

    fun monthShortLabel(month: YearMonth): String =
        month.month.getDisplayName(TextStyle.SHORT, Locale.FRENCH).replaceFirstChar { it.uppercase() }
}
