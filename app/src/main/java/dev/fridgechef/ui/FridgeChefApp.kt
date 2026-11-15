package dev.fridgechef.ui

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun FridgeChefApp(vm: MainViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    var showSettings by remember { mutableStateOf(false) }

    BackHandler(enabled = state.screen != Screen.FRIDGE) { vm.back() }

    when (state.screen) {
        Screen.FRIDGE -> FridgeScreen(
            state = state,
            onAdd = vm::addIngredients,
            onRemove = vm::removeIngredient,
            onClear = vm::clearFridge,
            onSearch = vm::search,
            onSettings = { showSettings = true },
        )
        Screen.RESULTS -> ResultsScreen(
            state = state,
            onBack = { vm.back() },
            onRetry = vm::search,
            onOpen = vm::open,
        )
        Screen.RECIPE -> RecipeScreen(
            state = state,
            onBack = { vm.back() },
            onServings = vm::setServings,
        )
    }

    if (showSettings) {
        SettingsDialog(
            initialKey = state.spoonacularKey,
            initialMetric = state.metric,
            onDismiss = { showSettings = false },
            onSave = { key, metric ->
                vm.saveSettings(key, metric)
                showSettings = false
            },
        )
    }
}
