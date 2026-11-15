package dev.fridgechef.data

/**
 * Loose matching between what the user typed ("tomatoes") and what a recipe lists
 * ("cherry tomatoes", "Tomato"). Works on whole words after light singularisation.
 */
object IngredientMatcher {

    /** Things almost every kitchen has; never counted as "missing". */
    private val staples = setOf(
        "salt", "pepper", "black pepper", "salt and pepper", "sea salt",
        "water", "ice", "oil", "olive oil", "vegetable oil", "sunflower oil",
        "cooking oil", "extra virgin olive oil",
    )

    fun normalize(raw: String): String =
        raw.lowercase()
            .replace(Regex("[^a-z\\s]"), " ")
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }
            .joinToString(" ") { singular(it) }

    private fun singular(word: String): String = when {
        word.length <= 3 -> word
        word.endsWith("ies") -> word.dropLast(3) + "y"
        word.endsWith("oes") -> word.dropLast(2)
        word.endsWith("ches") || word.endsWith("shes") -> word.dropLast(2)
        word.endsWith("ss") || word.endsWith("us") -> word
        word.endsWith("s") -> word.dropLast(1)
        else -> word
    }

    fun isStaple(ingredient: String): Boolean = normalize(ingredient) in staples

    /** True when one name is contained in the other as whole words. */
    fun matches(have: String, recipeIngredient: String): Boolean {
        val h = normalize(have)
        val r = normalize(recipeIngredient)
        if (h.isEmpty() || r.isEmpty()) return false
        return " $r ".contains(" $h ") || " $h ".contains(" $r ")
    }

    fun userHas(fridge: List<String>, recipeIngredient: String): Boolean =
        fridge.any { matches(it, recipeIngredient) }

    /** Variants worth sending to an exact-match API: "Eggs" -> ["eggs", "egg"]. */
    fun queryVariants(raw: String): List<String> {
        val trimmed = raw.trim().lowercase()
        return listOf(trimmed, normalize(trimmed)).filter { it.isNotBlank() }.distinct()
    }
}
