package dev.fridgechef.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp

@Composable
fun SettingsDialog(
    initialKey: String,
    initialMetric: Boolean,
    onDismiss: () -> Unit,
    onSave: (String, Boolean) -> Unit,
) {
    var key by remember { mutableStateOf(initialKey) }
    var metric by remember { mutableStateOf(initialMetric) }
    val uri = LocalUriHandler.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Settings") },
        text = {
            Column {
                Text(
                    "Works out of the box with TheMealDB (free, no key). For far more recipes, " +
                        "precise quantities and adjustable servings, paste a free Spoonacular API key.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                TextButton(onClick = { uri.openUri("https://spoonacular.com/food-api/console#Dashboard") }) {
                    Text("Get a free Spoonacular key")
                }
                OutlinedTextField(
                    value = key,
                    onValueChange = { key = it },
                    label = { Text("Spoonacular API key (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Metric units (g, ml)", Modifier.weight(1f))
                    Switch(checked = metric, onCheckedChange = { metric = it })
                }
            }
        },
        confirmButton = { TextButton(onClick = { onSave(key, metric) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
