package com.banka.app.domain

import com.banka.app.data.db.TransactionEntity
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

data class MonthlyTotals(
    val month: YearMonth,
    var income: Double,
    var expense: Double,
)

data class CategoryTotal(
    val category: Category,
    val total: Double,
    val share: Float,
)

data class AccountStats(
    val balance: Double,
    val incomeThisMonth: Double,
    val expenseThisMonth: Double,
    val months: List<MonthlyTotals>,
    val categories: List<CategoryTotal>,
    val transactionCount: Int,
)

object StatsCalculator {

    private const val MAX_MONTHS = 6

    fun compute(transactions: List<TransactionEntity>, today: LocalDate = LocalDate.now()): AccountStats {
        val zone = ZoneId.systemDefault()
        val currentMonth = YearMonth.from(today)
        var incomeMonth = 0.0
        var expenseMonth = 0.0
        val byMonth = LinkedHashMap<YearMonth, MonthlyTotals>()
        val byCategory = HashMap<String, Double>()

        for (transaction in transactions) {
            val localDate = LocalDate.ofInstant(Instant.ofEpochMilli(transaction.date), zone)
            val month = YearMonth.from(localDate)
            val entry = byMonth.getOrPut(month) { MonthlyTotals(month, 0.0, 0.0) }
            if (transaction.amount >= 0) {
                entry.income += transaction.amount
            } else {
                entry.expense += -transaction.amount
            }
            if (month == currentMonth) {
                if (transaction.amount >= 0) incomeMonth += transaction.amount
                else expenseMonth += -transaction.amount
            }
            if (transaction.amount < 0) {
                byCategory[transaction.category] = (byCategory[transaction.category] ?: 0.0) + -transaction.amount
            }
        }

        val months = byMonth.entries
            .sortedBy { it.key }
            .takeLast(MAX_MONTHS)
            .map { it.value }

        val totalExpense = byCategory.values.sum()
        val categories = byCategory.entries
            .map { (key, total) ->
                CategoryTotal(
                    category = Category.fromEntryKey(key),
                    total = total,
                    share = if (totalExpense > 0) (total / totalExpense).toFloat() else 0f,
                )
            }
            .sortedByDescending { it.total }

        return AccountStats(
            balance = transactions.sumOf { it.amount },
            incomeThisMonth = incomeMonth,
            expenseThisMonth = expenseMonth,
            months = months,
            categories = categories,
            transactionCount = transactions.size,
        )
    }
}
