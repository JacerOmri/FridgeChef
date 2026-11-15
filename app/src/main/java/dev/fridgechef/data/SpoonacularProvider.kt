package dev.fridgechef.data

import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.json.JSONArray
import org.json.JSONObject

/**
 * Spoonacular (https://spoonacular.com/food-api) — needs a free API key, but searches
 * across all your ingredients at once and returns precise numeric quantities, so
 * servings can be rescaled.
 */
class SpoonacularProvider(
    private val apiKey: String,
    private val metric: Boolean,
) : RecipeProvider {

    override val source = RecipeSource.SPOONACULAR

    private val base = "https://api.spoonacular.com/".toHttpUrl()

    override suspend fun search(fridge: List<String>): List<RecipeSummary> {
        val url = endpoint("recipes", "findByIngredients")
            .addQueryParameter("ingredients", fridge.joinToString(",") { it.trim() })
            .addQueryParameter("number", "20")
            .addQueryParameter("ranking", "1") // maximise used ingredients
            .addQueryParameter("ignorePantry", "true")
            .build()
        return SpoonacularParser.summaries(Http.get(url)).ranked()
    }

    override suspend fun details(id: String, fridge: List<String>): RecipeDetail {
        val url = endpoint("recipes", id, "information")
            .addQueryParameter("includeNutrition", "false")
            .build()
        return SpoonacularParser.detail(Http.get(url), fridge, metric)
    }

    private fun endpoint(vararg segments: String): HttpUrl.Builder =
        base.newBuilder().apply { segments.forEach { addPathSegment(it) } }
            .addQueryParameter("apiKey", apiKey)
}

object SpoonacularParser {

    fun summaries(body: String): List<RecipeSummary> {
        val arr = JSONArray(body)
        return (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            RecipeSummary(
                id = o.get("id").toString(),
                title = (o.str("title") ?: "Untitled"),
                imageUrl = o.str("image"),
                source = RecipeSource.SPOONACULAR,
                usedIngredients = names(o.optJSONArray("usedIngredients")),
                missedIngredients = names(o.optJSONArray("missedIngredients"))
                    .filterNot { IngredientMatcher.isStaple(it) },
            )
        }
    }

    fun detail(body: String, fridge: List<String>, metric: Boolean): RecipeDetail {
        val o = JSONObject(body)
        val ingredients = o.optJSONArray("extendedIngredients")?.let { arr ->
            (0 until arr.length()).map { i -> ingredient(arr.getJSONObject(i), fridge, metric) }
        } ?: emptyList()

        return RecipeDetail(
            id = o.get("id").toString(),
            title = (o.str("title") ?: "Untitled"),
            imageUrl = o.str("image"),
            source = RecipeSource.SPOONACULAR,
            servings = o.optInt("servings", 0).takeIf { it > 0 },
            readyInMinutes = o.optInt("readyInMinutes", 0).takeIf { it > 0 },
            tags = (strings(o.optJSONArray("cuisines")) + strings(o.optJSONArray("dishTypes"))).distinct().take(4),
            ingredients = ingredients,
            steps = steps(o),
            sourceUrl = o.str("sourceUrl"),
            videoUrl = null,
        )
    }

    private fun ingredient(o: JSONObject, fridge: List<String>, metric: Boolean): IngredientLine {
        val name = o.str("nameClean") ?: o.str("name") ?: "?"
        val measures = o.optJSONObject("measures")?.optJSONObject(if (metric) "metric" else "us")
        val amount = measures?.optDouble("amount")?.takeIf { !it.isNaN() } ?: o.optDouble("amount").takeIf { !it.isNaN() }
        val unit = measures?.str("unitShort") ?: o.str("unit")
        return IngredientLine(
            name = name,
            amount = amount,
            unit = unit.orEmpty(),
            measure = o.str("original").orEmpty(),
            have = IngredientMatcher.userHas(fridge, name),
            isStaple = IngredientMatcher.isStaple(name),
        )
    }

    private fun steps(o: JSONObject): List<String> {
        val sections = o.optJSONArray("analyzedInstructions")
        val structured = buildList {
            if (sections != null) for (s in 0 until sections.length()) {
                val steps = sections.getJSONObject(s).optJSONArray("steps") ?: continue
                for (i in 0 until steps.length()) {
                    steps.getJSONObject(i).str("step")?.let { add(it) }
                }
            }
        }
        if (structured.isNotEmpty()) return structured

        // Fallback: free-text HTML instructions.
        return (o.str("instructions") ?: "")
            .replace(Regex("(?i)<\\s*(br|/p|/li)\\s*/?>"), "\n")
            .replace(Regex("<[^>]+>"), "")
            .split('\n')
            .map { it.trim() }
            .filter { it.isNotEmpty() && it != "null" }
    }

    private fun names(arr: JSONArray?): List<String> =
        if (arr == null) emptyList()
        else (0 until arr.length()).mapNotNull { arr.getJSONObject(it).str("name") }

    private fun strings(arr: JSONArray?): List<String> =
        if (arr == null) emptyList() else (0 until arr.length()).map { arr.optString(it) }.filter { it.isNotBlank() }

    /** Android's optString() turns JSON null into the string "null"; this returns real null instead. */
    private fun JSONObject.str(key: String): String? {
        if (!has(key) || isNull(key)) return null
        return optString(key).trim().takeIf { it.isNotEmpty() && it != "null" }
    }
}
