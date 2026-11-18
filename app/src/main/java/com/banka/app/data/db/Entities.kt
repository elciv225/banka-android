package com.banka.app.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val emoji: String,
    val color: Long,
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(
    tableName = "transactions",
    indices = [
        Index(value = ["accountId"]),
        Index(value = ["accountId", "date"]),
        Index(value = ["accountId", "sourceFile"]),
    ],
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val accountId: Long,
    val date: Long,
    val label: String,
    val amount: Double,
    val category: String,
    val sourceFile: String,
    val importedAt: Long,
)

data class AccountBalance(
    val accountId: Long,
    val total: Double?,
)

data class ImportSource(
    val sourceFile: String,
    val count: Int,
    val minDate: Long,
    val maxDate: Long,
)
