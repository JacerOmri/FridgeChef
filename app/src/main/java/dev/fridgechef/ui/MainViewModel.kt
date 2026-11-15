package dev.fridgechef.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.fridgechef.data.HttpException
import dev.fridgechef.data.MealDbProvider
import dev.fridgechef.data.RecipeDetail
import dev.fridgechef.data.RecipeProvider
import dev.fridgechef.data.RecipeSource
import dev.fridgechef.data.RecipeSummary
import dev.fridgechef.data.Settings
import dev.fridgechef.data.SpoonacularProvider
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException

enum class Screen { FRIDGE, RESULTS, RECIPE }

data class UiState(
    val screen: Screen = Screen.FRIDGE,
    val fridge: List<String> = emptyList(),
    val searching: Boolean = false,
    val results: List<RecipeSummary> = emptyList(),
    val searchedWith: RecipeSource? = null,
    val notice: String? = null,
    val error: String? = null,
    val detailLoading: Boolean = false,
    val detail: RecipeDetail? = null,
    val detailError: String? = null,
    val servings: Int? = null,
    val spoonacularKey: String = "",
    val metric: Boolean = true,
)

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val settings = Settings(app)
    private val mealDb = MealDbProvider()

    private val _state = MutableStateFlow(
        UiState(fridge = settings.fridge, spoonacularKey = settings.spoonacularKey, metric = settings.metric)
    )
    val state: StateFlow<UiState> = _state.asStateFlow()

    private var job: Job? = null

    // ---- Fridge ---------------------------------------------------------------

    /** Accepts one item or a comma/newline separated list ("eggs, milk, spinach"). */
    fun addIngredients(text: String) {
        val incoming = text.split(',', '\n', ';')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        if (incoming.isEmpty()) return
        val current = _state.value.fridge
        val merged = current + incoming.filter { new -> current.none { it.equals(new, ignoreCase = true) } }
            .distinctBy { it.lowercase() }
        saveFridge(merged)
    }

    fun removeIngredient(item: String) = saveFridge(_state.value.fridge - item)

    fun clearFridge() = saveFridge(emptyList())

    private fun saveFridge(list: List<String>) {
        settings.fridge = list
        _state.update { it.copy(fridge = list) }
    }

    // ---- Settings -------------------------------------------------------------

    fun saveSettings(key: String, metric: Boolean) {
        settings.spoonacularKey = key
        settings.metric = metric
        _state.update { it.copy(spoonacularKey = key.trim(), metric = metric) }
    }

    private fun spoonacular(): RecipeProvider? =
        _state.value.spoonacularKey.takeIf { it.isNotBlank() }?.let { SpoonacularProvider(it, _state.value.metric) }

    private fun providerFor(source: RecipeSource): RecipeProvider =
        if (source == RecipeSource.SPOONACULAR) spoonacular() ?: mealDb else mealDb

    // ---- Search ---------------------------------------------------------------

    fun search() {
        val fridge = _state.value.fridge
        if (fridge.isEmpty()) return
        job?.cancel()
        _state.update {
            it.copy(screen = Screen.RESULTS, searching = true, results = emptyList(), error = null, notice = null)
        }
        job = viewModelScope.launch {
            val primary = spoonacular()
            try {
                val results = if (primary != null) {
                    try {
                        primary.search(fridge).also { r -> _state.update { it.copy(searchedWith = primary.source) } }
                    } catch (e: HttpException) {
                        // Bad key or daily quota used up: fall back to the free source.
                        _state.update {
                            it.copy(notice = "Spoonacular unavailable (${spoonacularReason(e.code)}), showing TheMealDB results instead.")
                        }
                        mealDb.search(fridge).also { _state.update { it.copy(searchedWith = RecipeSource.MEALDB) } }
                    }
                } else {
                    mealDb.search(fridge).also { _state.update { it.copy(searchedWith = RecipeSource.MEALDB) } }
                }
                _state.update { it.copy(searching = false, results = results) }
            } catch (e: IOException) {
                _state.update { it.copy(searching = false, error = "Couldn't reach the recipe service. Check your connection and try again.") }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                _state.update { it.copy(searching = false, error = "Something went wrong: ${e.message}") }
            }
        }
    }

    private fun spoonacularReason(code: Int) = when (code) {
        401 -> "API key rejected"
        402 -> "daily quota reached"
        429 -> "too many requests"
        else -> "error $code"
    }

    // ---- Recipe detail --------------------------------------------------------

    fun open(summary: RecipeSummary) {
        job?.cancel()
        _state.update { it.copy(screen = Screen.RECIPE, detail = null, detailError = null, detailLoading = true) }
        job = viewModelScope.launch {
            try {
                val detail = providerFor(summary.source).details(summary.id, _state.value.fridge)
                _state.update { it.copy(detailLoading = false, detail = detail, servings = detail.servings) }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                _state.update { it.copy(detailLoading = false, detailError = "Couldn't load this recipe (${e.message}).") }
            }
        }
    }

    fun setServings(n: Int) = _state.update { it.copy(servings = n.coerceIn(1, 50)) }

    // ---- Navigation -----------------------------------------------------------

    /** Returns false when already on the first screen (let the system handle back). */
    fun back(): Boolean {
        val s = _state.value.screen
        if (s == Screen.FRIDGE) return false
        job?.cancel()
        _state.update {
            when (s) {
                Screen.RECIPE -> it.copy(screen = Screen.RESULTS, detailLoading = false)
                else -> it.copy(screen = Screen.FRIDGE, searching = false)
            }
        }
        return true
    }
}
