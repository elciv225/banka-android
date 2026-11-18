package com.banka.app.ui.accounts

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.banka.app.data.csv.ImportRepository
import com.banka.app.data.db.AccountBalance
import com.banka.app.data.db.AccountEntity
import com.banka.app.data.db.Graph
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.io.File

data class AccountWithBalance(
    val account: AccountEntity,
    val balance: Double?,
)

class AccountsViewModel(application: Application) : AndroidViewModel(application) {

    private val accountDao = Graph.accountDao
    private val transactionDao = Graph.transactionDao
    private val importRepository = ImportRepository(application)

    val items: Flow<List<AccountWithBalance>> = combine(
        accountDao.observeAccounts(),
        transactionDao.observeBalances(),
    ) { accounts, balances ->
        val balanceById = balances.associate { it.accountId to it.total }
        accounts.map { AccountWithBalance(it, balanceById[it.id]) }
    }

    val accounts: Flow<List<AccountEntity>> = accountDao.observeAccounts()

    fun add(name: String, emoji: String, color: Long) = viewModelScope.launch(Dispatchers.IO) {
        accountDao.insert(AccountEntity(name = name.trim(), emoji = emoji, color = color))
    }

    fun delete(account: AccountEntity) = viewModelScope.launch(Dispatchers.IO) {
        transactionDao.deleteAllForAccount(account.id)
        accountDao.delete(account.id)
        importRepository.accountDir(account.id).deleteRecursively()
    }

    fun seedIfNeeded() = viewModelScope.launch(Dispatchers.IO) {
        if (accountDao.count() == 0) {
            accountDao.insert(
                AccountEntity(name = "Compte courant", emoji = "\uD83C\uDFE6", color = 0xFF0B6E4F),
            )
        }
    }
}

fun accountDirectory(application: Application, accountId: Long): File =
    ImportRepository(application).accountDir(accountId)
