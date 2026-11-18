package com.banka.app.ui.dashboard

import android.app.Application
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.banka.app.domain.Category
import com.banka.app.ui.components.CategoryLegend
import com.banka.app.ui.components.DonutChart
import com.banka.app.ui.components.MonthlyBarsChart
import com.banka.app.util.Formatters

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    accountId: Long,
    onBack: () -> Unit,
) {
    val app = LocalContext.current.applicationContext as Application
    val viewModel: DashboardViewModel = viewModel(
        key = "dashboard-$accountId",
        factory = DashboardViewModelFactory(accountId, app),
    )

    val stats by viewModel.stats.collectAsState()
    val transactions by viewModel.transactions.collectAsState()
    val sources by viewModel.sources.collectAsState()
    val preview by viewModel.preview.collectAsState()
    val message by viewModel.message.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    var showDeleteAll by remember { mutableStateOf(false) }

    LaunchedEffect(message) {
        message?.let {
            snackbar.showSnackbar(it)
            viewModel.consumeMessage()
        }
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let { viewModel.onFilePicked(it, "import.csv") }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Détail du compte") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Retour")
                    }
                },
                actions = {
                    if (transactions.isNotEmpty()) {
                        IconButton(onClick = { showDeleteAll = true }) {
                            Icon(Icons.Rounded.Close, contentDescription = "Vider le compte")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                picker.launch(
                    arrayOf(
                        "text/*", "text/csv", "text/comma-separated-values",
                        "application/csv", "application/vnd.ms-excel", "application/octet-stream",
                    ),
                )
            }) {
                Icon(Icons.Rounded.Add, contentDescription = "Importer un CSV")
            }
        },
        snackbarHost = { SnackbarHost(snackbar) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                ) {
                    Column(Modifier.padding(20.dp)) {
                        Text(
                            "Solde total",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                        Text(
                            Formatters.amount(stats.balance),
                            style = MaterialTheme.typography.displayMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Column {
                                Text(
                                    "Revenus (mois)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                )
                                Text(
                                    Formatters.amount(stats.incomeThisMonth, withSign = true),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                )
                            }
                            Column {
                                Text(
                                    "Dépenses (mois)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                )
                                Text(
                                    "-" + Formatters.amount(stats.expenseThisMonth),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                )
                            }
                        }
                    }
                }
            }

            if (transactions.isEmpty()) {
                item {
                    EmptyState(onImportClick = {
                        picker.launch(arrayOf("text/*", "text/csv", "application/*"))
                    })
                }
            } else {
                item {
                    DashboardCard(title = "Derniers 6 mois") {
                        MonthlyBarsChart(months = stats.months, modifier = Modifier.fillMaxWidth())
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            LegendDot(MaterialTheme.colorScheme.primary, "Revenus")
                            LegendDot(MaterialTheme.colorScheme.tertiary, "Dépenses")
                        }
                    }
                }
                item {
                    DashboardCard(title = "Dépenses par catégorie") {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            DonutChart(
                                slices = stats.categories,
                                modifier = Modifier.size(140.dp),
                            )
                            Spacer(Modifier.width(16.dp))
                            CategoryLegend(
                                slices = stats.categories,
                                max = 5,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
                if (sources.isNotEmpty()) {
                    item {
                        DashboardCard(title = "Fichiers sauvegardés") {
                            Column {
                                sources.forEachIndexed { index, source ->
                                    if (index > 0) HorizontalDivider()
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(source.sourceFile, style = MaterialTheme.typography.bodyMedium)
                                            Text(
                                                "${source.count} opérations · ${Formatters.date(source.minDate)} → ${Formatters.date(source.maxDate)}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                        IconButton(onClick = { viewModel.deleteSource(source.sourceFile) }) {
                                            Icon(
                                                Icons.Rounded.Close,
                                                contentDescription = "Supprimer les opérations de ce fichier",
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                item {
                    DashboardCard(title = "Opérations récentes") {
                        Column {
                            transactions.take(20).forEachIndexed { index, transaction ->
                                if (index > 0) HorizontalDivider()
                                TransactionRow(transaction)
                            }
                        }
                    }
                }
            }
        }
    }

    preview?.let { load ->
        ImportDialog(
            load = load,
            onDismiss = { viewModel.cancelImport() },
            onConfirm = { viewModel.confirmImport() },
        )
    }

    if (showDeleteAll) {
        AlertDialog(
            onDismissRequest = { showDeleteAll = false },
            title = { Text("Vider ce compte ?") },
            text = { Text("Toutes les opérations et fichiers liés seront supprimés.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteAll = false
                    viewModel.deleteAllTransactions()
                }) { Text("Supprimer") }
            },
            dismissButton = { TextButton(onClick = { showDeleteAll = false }) { Text("Annuler") } },
        )
    }
}

@Composable
private fun DashboardCard(title: String, content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).background(color, CircleShape))
        Spacer(Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun TransactionRow(transaction: com.banka.app.data.db.TransactionEntity) {
    val category = Category.fromEntryKey(transaction.category)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = category.emoji, style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                transaction.label,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
            )
            Text(
                "${category.label} · ${Formatters.date(transaction.date)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = Formatters.amount(transaction.amount, withSign = true),
            style = MaterialTheme.typography.titleSmall,
            color = if (transaction.amount >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary,
        )
    }
}

@Composable
private fun EmptyState(onImportClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onImportClick),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("\uD83D\uDCC4", style = MaterialTheme.typography.displayMedium)
            Spacer(Modifier.height(8.dp))
            Text("Aucune opération pour ce compte", style = MaterialTheme.typography.titleMedium)
            Text(
                "Touche le bouton + pour importer ton premier fichier CSV.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

class DashboardViewModelFactory(private val accountId: Long, private val application: Application) :
    androidx.lifecycle.ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T =
        DashboardViewModel(accountId, application) as T
}
