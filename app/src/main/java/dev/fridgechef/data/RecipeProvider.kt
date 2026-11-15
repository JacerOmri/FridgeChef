package dev.fridgechef.data

interface RecipeProvider {
    val source: RecipeSource

    /** Find recipes that use what's in the fridge, best matches first. */
    suspend fun search(fridge: List<String>): List<RecipeSummary>

    /** Full recipe with quantities and steps. */
    suspend fun details(id: String, fridge: List<String>): RecipeDetail
}

/** Ranking shared by both providers: use as much of the fridge as possible, buy as little as possible. */
internal fun List<RecipeSummary>.ranked(): List<RecipeSummary> =
    sortedWith(
        compareByDescending<RecipeSummary> { it.usedCount * 2 - it.missedCount }
            .thenByDescending { it.usedCount }
            .thenBy { it.missedCount }
    )
