package dev.fridgechef.data

enum class RecipeSource(val label: String) {
    MEALDB("TheMealDB"),
    SPOONACULAR("Spoonacular"),
}

/** One search hit, shown in the results list. */
data class RecipeSummary(
    val id: String,
    val title: String,
    val imageUrl: String?,
    val source: RecipeSource,
    val usedIngredients: List<String>,
    val missedIngredients: List<String>,
) {
    val usedCount get() = usedIngredients.size
    val missedCount get() = missedIngredients.size
}

/**
 * One line of a recipe's ingredient list.
 *
 * [amount] + [unit] are set when the source gives a numeric quantity (Spoonacular),
 * which lets the app rescale for a different number of servings. Otherwise the
 * quantity is the free-text [measure] (TheMealDB, e.g. "2 tbsp" or "1 handful").
 */
data class IngredientLine(
    val name: String,
    val amount: Double?,
    val unit: String,
    val measure: String,
    val have: Boolean,
    val isStaple: Boolean,
)

data class RecipeDetail(
    val id: String,
    val title: String,
    val imageUrl: String?,
    val source: RecipeSource,
    val servings: Int?,
    val readyInMinutes: Int?,
    val tags: List<String>,
    val ingredients: List<IngredientLine>,
    val steps: List<String>,
    val sourceUrl: String?,
    val videoUrl: String?,
)
