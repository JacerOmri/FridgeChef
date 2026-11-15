package dev.fridgechef.data

import android.content.Context

/** Remembers the fridge contents and settings between launches. */
class Settings(context: Context) {
    private val prefs = context.getSharedPreferences("fridgechef", Context.MODE_PRIVATE)

    var fridge: List<String>
        get() = prefs.getString(KEY_FRIDGE, "").orEmpty().split('\n').filter { it.isNotBlank() }
        set(value) = prefs.edit().putString(KEY_FRIDGE, value.joinToString("\n")).apply()

    var spoonacularKey: String
        get() = prefs.getString(KEY_API, "").orEmpty()
        set(value) = prefs.edit().putString(KEY_API, value.trim()).apply()

    var metric: Boolean
        get() = prefs.getBoolean(KEY_METRIC, true)
        set(value) = prefs.edit().putBoolean(KEY_METRIC, value).apply()

    private companion object {
        const val KEY_FRIDGE = "fridge"
        const val KEY_API = "spoonacular_key"
        const val KEY_METRIC = "metric"
    }
}
