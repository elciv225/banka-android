package com.banka.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.enableEdgeToEdge
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.banka.app.ui.accounts.AccountsScreen
import com.banka.app.ui.dashboard.DashboardScreen
import com.banka.app.ui.theme.BankaTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        com.banka.app.data.db.Graph.init(this)
        enableEdgeToEdge()
        setContent {
            BankaTheme {
                AppRoot()
            }
        }
    }
}

@androidx.compose.runtime.Composable
fun AppRoot() {
    var selectedAccountId by rememberSaveable { mutableStateOf(0L) }
    val currentAccountId = selectedAccountId.takeIf { it > 0L }

    currentAccountId?.let { accountId ->
        DashboardScreen(
            accountId = accountId,
            onBack = { selectedAccountId = 0L },
        )
    } ?: AccountsScreen(
        onOpenAccount = { accountId -> selectedAccountId = accountId },
    )
}
