package dev.fridgechef.data

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ParsersTest {

    private val fridge = listOf("chicken", "Soy sauce", "garlic", "eggs")

    private val meal = JSONObject(
        """
        {"meals":[{"idMeal":"52772","strMeal":"Teriyaki Chicken Casserole","strCategory":"Chicken",
          "strArea":"Japanese","strTags":"Meat,Casserole",
          "strInstructions":"STEP 1\r\nPreheat oven to 350F.\r\n\r\nSTEP 2\r\nCombine soy sauce and garlic.",
          "strMealThumb":"https://www.themealdb.com/images/media/meals/x.jpg",
          "strYoutube":"https://www.youtube.com/watch?v=4aZr5hZXP_s","strSource":null,
          "strIngredient1":"soy sauce","strMeasure1":"3/4 cup",
          "strIngredient2":"water","strMeasure2":"1/2 cup",
          "strIngredient3":"chicken breasts","strMeasure3":"2",
          "strIngredient4":"garlic","strMeasure4":"1/2 tsp",
          "strIngredient5":"brown rice","strMeasure5":"3 cups",
          "strIngredient6":"","strMeasure6":" ",
          "strIngredient7":null,"strMeasure7":null}]}
        """.trimIndent()
    ).toString()

    @Test fun mealDbFilterHandlesNoResults() {
        assertEquals(emptyList<String>(), MealDbParser.idsFromFilter("""{"meals":null}"""))
        assertEquals(listOf("1", "2"), MealDbParser.idsFromFilter("""{"meals":[{"idMeal":"1"},{"idMeal":"2"}]}"""))
    }

    @Test fun mealDbDetail() {
        val detail = MealDbParser.detail(MealDbParser.mealFromLookup(meal)!!, fridge)
        assertEquals("Teriyaki Chicken Casserole", detail.title)
        assertEquals(5, detail.ingredients.size)
        assertEquals("3/4 cup", detail.ingredients[0].measure)
        assertTrue(detail.ingredients.first { it.name == "chicken breasts" }.have)
        assertTrue(detail.ingredients.first { it.name == "water" }.isStaple)
        assertFalse(detail.ingredients.first { it.name == "brown rice" }.have)
        assertEquals(listOf("Preheat oven to 350F.", "Combine soy sauce and garlic."), detail.steps)
        assertNull(detail.sourceUrl)
        assertEquals(listOf("Japanese", "Chicken", "Meat", "Casserole"), detail.tags)
    }

    @Test fun mealDbSummaryExcludesStaplesFromMissing() {
        val s = MealDbParser.summary(MealDbParser.mealFromLookup(meal)!!, fridge)
        assertEquals(3, s.usedCount)
        assertEquals(listOf("brown rice"), s.missedIngredients)
    }

    @Test fun spoonacularSearch() {
        val body = """[{"id":73420,"title":"Apple Strudel","image":null,"usedIngredientCount":1,
            "missedIngredientCount":2,"usedIngredients":[{"name":"apples"}],
            "missedIngredients":[{"name":"cinnamon"},{"name":"salt"}]}]"""
        val r = SpoonacularParser.summaries(body).single()
        assertEquals("73420", r.id)
        assertNull(r.imageUrl)
        assertEquals(listOf("cinnamon"), r.missedIngredients)
    }

    @Test fun spoonacularDetailUsesMetricAndSteps() {
        val body = """{"id":1,"title":"Garlic Chicken","servings":4,"readyInMinutes":30,"sourceUrl":"https://x",
            "cuisines":["Asian"],"dishTypes":["dinner"],
            "extendedIngredients":[
              {"name":"chicken thighs","nameClean":"chicken thigh","amount":1.5,"unit":"lb","original":"1.5 lb chicken thighs",
               "measures":{"us":{"amount":1.5,"unitShort":"lb"},"metric":{"amount":680.389,"unitShort":"g"}}},
              {"name":"lime","nameClean":null,"amount":1,"unit":"","original":"1 lime",
               "measures":{"us":{"amount":1.0,"unitShort":""},"metric":{"amount":1.0,"unitShort":""}}}],
            "analyzedInstructions":[{"name":"","steps":[{"number":1,"step":"Brown the chicken."},{"number":2,"step":"Add garlic."}]}]}"""
        val d = SpoonacularParser.detail(body, fridge, metric = true)
        assertEquals(4, d.servings)
        assertEquals("g", d.ingredients[0].unit)
        assertTrue(d.ingredients[0].have)
        assertEquals("lime", d.ingredients[1].name)
        assertFalse(d.ingredients[1].have)
        assertEquals(2, d.steps.size)
        assertEquals("1361 g", Quantities.line(d.ingredients[0], scale = 2.0))
        assertEquals("1", Quantities.line(d.ingredients[1], scale = 1.0))
    }

    @Test fun spoonacularHtmlInstructionsFallback() {
        val body = """{"id":2,"title":"X","instructions":"<ol><li>Mix.</li><li>Bake.</li></ol>","analyzedInstructions":[]}"""
        assertEquals(listOf("Mix.", "Bake."), SpoonacularParser.detail(body, fridge, metric = true).steps)
    }

    @Test fun matcher() {
        assertTrue(IngredientMatcher.matches("tomatoes", "Cherry Tomatoes"))
        assertTrue(IngredientMatcher.matches("chicken breast", "chicken"))
        assertFalse(IngredientMatcher.matches("pea", "peanut butter"))
        assertEquals("1½", Quantities.format(1.5))
    }
}
