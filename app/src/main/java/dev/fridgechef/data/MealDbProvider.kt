package dev.fridgechef.data

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap

/**
 * TheMealDB (https://www.themealdb.com) — free, no sign-up, uses the public test key "1".
 *
 * The free API filters by one ingredient at a time, so we query each fridge item,
 * keep the meals that come up most often, then fetch those meals in full to work out
 * exactly which ingredients you have and which you'd need to buy.
 */
class MealDbProvider(
    private val maxCandidates: Int = 15,
) : RecipeProvider {

    override val source = RecipeSource.MEALDB

    private val base = "https://www.themealdb.com/api/json/v1/1/".toHttpUrl()
    private val cache = ConcurrentHashMap<String, JSONObject>()

    override suspend fun search(fridge: List<String>): List<RecipeSummary> = coroutineScope {
        // meal id -> how many of the user's ingredients returned it
        val hitsPerMeal = fridge.map { ingredient ->
            async { mealIdsFor(ingredient) }
        }.awaitAll()

        // Every request failed: that's a network problem, not "no recipes".
        if (hitsPerMeal.all { it == null }) throw java.io.IOException("TheMealDB unreachable")

        val counts = HashMap<String, Int>()
        hitsPerMeal.filterNotNull().forEach { ids -> ids.forEach { counts[it] = (counts[it] ?: 0) + 1 } }

        val candidates = counts.entries
            .sortedByDescending { it.value }
            .take(maxCandidates)
            .map { it.key }

        candidates.map { id -> async { runCatching { fetchMeal(id) }.getOrNull() } }
            .awaitAll()
            .filterNotNull()
            .map { MealDbParser.summary(it, fridge) }
            .ranked()
    }

    override suspend fun details(id: String, fridge: List<String>): RecipeDetail =
        MealDbParser.detail(fetchMeal(id), fridge)

    /** Meal ids using this ingredient, or null if every request for it failed. */
    private suspend fun mealIdsFor(ingredient: String): Set<String>? {
        val ids = HashSet<String>()
        var anySucceeded = false
        for (variant in IngredientMatcher.queryVariants(ingredient)) {
            val url = endpoint("filter.php").addQueryParameter("i", variant.replace(' ', '_')).build()
            val body = runCatching { Http.get(url) }.getOrNull() ?: continue
            anySucceeded = true
            ids += MealDbParser.idsFromFilter(body)
        }
        return if (anySucceeded) ids else null
    }

    private suspend fun fetchMeal(id: String): JSONObject {
        cache[id]?.let { return it }
        val url = endpoint("lookup.php").addQueryParameter("i", id).build()
        val meal = MealDbParser.mealFromLookup(Http.get(url))
            ?: throw IllegalStateException("Recipe $id not found")
        cache[id] = meal
        return meal
    }

    private fun endpoint(path: String): HttpUrl.Builder = base.newBuilder().addPathSegment(path)
}

/** Pure JSON -> model conversion, kept separate so it can be unit-tested. */
object MealDbParser {

    fun idsFromFilter(body: String): List<String> {
        val meals = JSONObject(body).optJSONArray("meals") ?: return emptyList()
        return (0 until meals.length()).map { meals.getJSONObject(it).getString("idMeal") }
    }

    fun mealFromLookup(body: String): JSONObject? =
        JSONObject(body).optJSONArray("meals")?.optJSONObject(0)

    /** (ingredient, measure) pairs from strIngredient1..20 / strMeasure1..20. */
    fun ingredients(meal: JSONObject): List<Pair<String, String>> =
        (1..20).mapNotNull { i ->
            val name = meal.cleanString("strIngredient$i") ?: return@mapNotNull null
            name to (meal.cleanString("strMeasure$i") ?: "")
        }

    fun summary(meal: JSONObject, fridge: List<String>): RecipeSummary {
        val names = ingredients(meal).map { it.first }
        val used = names.filter { IngredientMatcher.userHas(fridge, it) }
        val missed = names.filter { !IngredientMatcher.userHas(fridge, it) && !IngredientMatcher.isStaple(it) }
        return RecipeSummary(
            id = meal.getString("idMeal"),
            title = meal.cleanString("strMeal") ?: "Untitled",
            imageUrl = meal.cleanString("strMealThumb"),
            source = RecipeSource.MEALDB,
            usedIngredients = used,
            missedIngredients = missed,
        )
    }

    fun detail(meal: JSONObject, fridge: List<String>): RecipeDetail = RecipeDetail(
        id = meal.getString("idMeal"),
        title = meal.cleanString("strMeal") ?: "Untitled",
        imageUrl = meal.cleanString("strMealThumb"),
        source = RecipeSource.MEALDB,
        servings = null, // TheMealDB doesn't say
        readyInMinutes = null,
        tags = listOfNotNull(meal.cleanString("strArea"), meal.cleanString("strCategory")) +
            (meal.cleanString("strTags")?.split(',')?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList()),
        ingredients = ingredients(meal).map { (name, measure) ->
            IngredientLine(
                name = name,
                amount = null,
                unit = "",
                measure = measure,
                have = IngredientMatcher.userHas(fridge, name),
                isStaple = IngredientMatcher.isStaple(name),
            )
        },
        steps = splitSteps(meal.cleanString("strInstructions").orEmpty()),
        sourceUrl = meal.cleanString("strSource"),
        videoUrl = meal.cleanString("strYoutube"),
    )

    /** Instructions come as one blob; split into paragraphs and drop "STEP 1"-style headings. */
    fun splitSteps(instructions: String): List<String> =
        instructions.split(Regex("\\r?\\n"))
            .map { it.trim().replace(Regex("^(step\\s*\\d+[:.)]?|\\d+[.)])\\s*", RegexOption.IGNORE_CASE), "").trim() }
            .filter { it.isNotEmpty() && !it.matches(Regex("(?i)step\\s*\\d+")) }

    private fun JSONObject.cleanString(key: String): String? {
        if (isNull(key)) return null
        return optString(key).trim().takeIf { it.isNotEmpty() && it != "null" }
    }
}
