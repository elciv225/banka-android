package com.banka.app.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.banka.app.data.csv.CsvLoad
import com.banka.app.util.Formatters
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.text.font.FontFamily

@Composable
fun ImportDialog(
    load: CsvLoad,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    val parsed = load.parsed
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Importer ce fichier ?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(load.originalName, style = MaterialTheme.typography.titleSmall)
                Text(
                    text = "${parsed.rows.size} opérations détectées" +
                        if (parsed.skippedRows > 0) " · ${parsed.skippedRows} lignes ignorées" else "",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = "Séparateur : « ${separatorName(parsed.separator)} » · " +
                        if (parsed.header != null) "en-tête reconnu" else "en-tête non détecté, colonnes déduites",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (parsed.header != null) {
                    Text(
                        text = "Colonnes : ${parsed.header.filter { it.isNotBlank() }.take(6).joinToString(" · ")}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Column(modifier = Modifier.fillMaxWidth()) {
                    parsed.rows.take(3).forEach { row ->
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = Formatters.date(row.date),
                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                            )
                            Text(
                                text = "  ${truncate(row.label, 20)}  ${Formatters.amount(row.amount)}",
                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                            )
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onConfirm) { Text("Importer") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } },
    )
}

private fun truncate(value: String, max: Int): String =
    if (value.length <= max) value else value.take(max - 1) + "…"

private fun separatorName(separator: Char): String = when (separator) {
    ';' -> "point-virgule"
    ',' -> "virgule"
    '\t' -> "tabulation"
    '|' -> "barre verticale"
    else -> separator.toString()
}
