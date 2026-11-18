package com.banka.app.ui.dashboard

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.banka.app.data.csv.CsvLoad
import com.banka.app.data.csv.ImportRepository
import com.banka.app.data.db.Graph
import com.banka.app.data.db.ImportSource
import com.banka.app.data.db.TransactionEntity
import com.banka.app.domain.AccountStats
import com.banka.app.domain.Categorizer
import com.banka.app.domain.StatsCalculator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.math.abs

class DashboardViewModel(private val accountId: Long, application: Application) :
    AndroidViewModel(application) {

    private val dao = Graph.transactionDao
    private val repository = ImportRepository(application)

    val transactions: StateFlow<List<TransactionEntity>> = dao.observeForAccount(accountId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val stats: StateFlow<AccountStats> = transactions
        .map { StatsCalculator.compute(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StatsCalculator.compute(emptyList()))

    val sources: StateFlow<List<ImportSource>> = dao.observeSources(accountId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _preview = MutableStateFlow<CsvLoad?>(null)
    val preview: StateFlow<CsvLoad?> = _preview.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    fun onFilePicked(uri: Uri, displayName: String) = viewModelScope.launch(Dispatchers.IO) {
        val load = runCatching { repository.load(accountId, uri, displayName) }.getOrNull()
        _message.value = load?.let { null } ?: "Impossible de lire ce fichier CSV"
        if (load != null) _preview.value = load
    }

    fun confirmImport() = viewModelScope.launch(Dispatchers.IO) {
        val load = _preview.value ?: return@launch
        val existing = dao.allForAccount(accountId).mapTo(HashSet()) { it.fingerprint() }
        val now = System.currentTimeMillis()
        val entities = load.parsed.rows.map { row ->
            TransactionEntity(
                accountId = accountId,
                date = row.date,
                label = row.label,
                amount = row.amount,
                category = Categorizer.classify(row.label).key,
                sourceFile = load.savedFile.name,
                importedAt = now,
            )
        }
        val fresh = entities.filter { it.fingerprint() !in existing }
        val inserted = dao.insertAll(fresh).count { it != -1L }
        val duplicates = entities.size - fresh.size
        _message.value = "$inserted opérations importées" + if (duplicates > 0) " ($duplicates doublons ignorés)" else ""
        _preview.value = null
    }

    fun cancelImport() = viewModelScope.launch(Dispatchers.IO) {
        _preview.value?.let { repository.delete(it.savedFile) }
        _preview.value = null
    }

    fun deleteSource(sourceFile: String) = viewModelScope.launch(Dispatchers.IO) {
        dao.deleteBySource(accountId, sourceFile)
    }

    fun deleteAllTransactions() = viewModelScope.launch(Dispatchers.IO) {
        dao.deleteAllForAccount(accountId)
    }

    fun consumeMessage() {
        _message.value = null
    }

    private fun TransactionEntity.fingerprint(): String =
        "$date|${label.lowercase().trim()}|${(abs(amount) * 100).toLong()}"
}
