package dev.fridgechef.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp

private val suggestions = listOf(
    "eggs", "chicken", "rice", "onion", "garlic", "tomatoes", "potatoes",
    "cheese", "milk", "butter", "pasta", "carrots", "spinach", "lemon",
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FridgeScreen(
    state: UiState,
    onAdd: (String) -> Unit,
    onRemove: (String) -> Unit,
    onClear: () -> Unit,
    onSearch: () -> Unit,
    onSettings: () -> Unit,
) {
    var input by rememberSaveable { mutableStateOf("") }
    val submit = {
        onAdd(input)
        input = ""
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("What's in your fridge?") },
                actions = {
                    IconButton(onClick = onSettings) { Icon(Icons.Filled.Settings, "Settings") }
                },
            )
        },
        bottomBar = {
            Column(Modifier.navigationBarsPadding().imePadding().padding(16.dp)) {
                Button(
                    onClick = onSearch,
                    enabled = state.fridge.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                ) {
                    Icon(Icons.Filled.Search, null)
                    Spacer(Modifier.size(8.dp))
                    Text("Find recipes")
                }
                Text(
                    text = if (state.spoonacularKey.isNotBlank()) "Searching Spoonacular" else "Searching TheMealDB (add a Spoonacular key in settings for more results)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp).align(Alignment.CenterHorizontally),
                )
            }
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Add an ingredient") },
                placeholder = { Text("e.g. eggs, spinach, feta") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.None,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = { submit() }),
                trailingIcon = {
                    IconButton(onClick = submit, enabled = input.isNotBlank()) {
                        Icon(Icons.Filled.Add, "Add")
                    }
                },
            )

            Spacer(Modifier.height(16.dp))

            if (state.fridge.isEmpty()) {
                Text(
                    "Your fridge is empty. Type what you have, or tap a suggestion below. " +
                        "You can paste a list separated by commas.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "In your fridge (${state.fridge.size})",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = onClear) { Text("Clear all") }
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.fridge.forEach { item ->
                        InputChip(
                            selected = true,
                            onClick = { onRemove(item) },
                            label = { Text(item) },
                            trailingIcon = {
                                Icon(Icons.Filled.Close, "Remove $item", Modifier.size(InputChipDefaults.IconSize))
                            },
                        )
                    }
                }
            }

            val remaining = suggestions.filter { s -> state.fridge.none { it.equals(s, ignoreCase = true) } }
            if (remaining.isNotEmpty()) {
                Spacer(Modifier.height(24.dp))
                Text("Quick add", style = MaterialTheme.typography.titleSmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    remaining.forEach { s ->
                        InputChip(
                            selected = false,
                            onClick = { onAdd(s) },
                            label = { Text(s) },
                            leadingIcon = { Icon(Icons.Filled.Add, null, Modifier.size(InputChipDefaults.IconSize)) },
                        )
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}
