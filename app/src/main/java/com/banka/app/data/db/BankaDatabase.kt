package com.banka.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [AccountEntity::class, TransactionEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class BankaDatabase : RoomDatabase() {

    abstract fun accountDao(): AccountDao
    abstract fun transactionDao(): TransactionDao

    companion object {
        private const val DB_NAME = "banka.db"
    }
}

object Graph {

    private var database: BankaDatabase? = null

    fun init(context: Context) {
        if (database == null) {
            database = Room.databaseBuilder(context.applicationContext, BankaDatabase::class.java, "banka.db")
                .fallbackToDestructiveMigration(dropAllTables = false)
                .build()
        }
    }

    val accountDao: AccountDao get() = requireNotNull(database).accountDao()
    val transactionDao: TransactionDao get() = requireNotNull(database).transactionDao()
}
