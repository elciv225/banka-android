package com.banka.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {

    @Query("SELECT * FROM accounts ORDER BY createdAt")
    fun observeAccounts(): Flow<List<AccountEntity>>

    @Query("SELECT COUNT(*) FROM accounts")
    suspend fun count(): Int

    @Insert
    suspend fun insert(account: AccountEntity): Long

    @Query("DELETE FROM transactions WHERE accountId = :accountId")
    suspend fun deleteTransactionsOf(accountId: Long)

    @Query("DELETE FROM accounts WHERE id = :accountId")
    suspend fun delete(accountId: Long)
}

@Dao
interface TransactionDao {

    @Query("SELECT * FROM transactions WHERE accountId = :accountId ORDER BY date DESC, id DESC")
    fun observeForAccount(accountId: Long): Flow<List<TransactionEntity>>

    @Query("SELECT accountId AS accountId, SUM(amount) AS total FROM transactions GROUP BY accountId")
    fun observeBalances(): Flow<List<AccountBalance>>

    @Query(
        "SELECT sourceFile, COUNT(*) AS count, MIN(date) AS minDate, MAX(date) AS maxDate " +
            "FROM transactions WHERE accountId = :accountId GROUP BY sourceFile ORDER BY MAX(importedAt) DESC"
    )
    fun observeSources(accountId: Long): Flow<List<ImportSource>>

    @Query("SELECT * FROM transactions WHERE accountId = :accountId")
    suspend fun allForAccount(accountId: Long): List<TransactionEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(transactions: List<TransactionEntity>): List<Long>

    @Query("DELETE FROM transactions WHERE accountId = :accountId AND sourceFile = :sourceFile")
    suspend fun deleteBySource(accountId: Long, sourceFile: String)

    @Query("DELETE FROM transactions WHERE accountId = :accountId")
    suspend fun deleteAllForAccount(accountId: Long)
}
