package dev.fridgechef.data

import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.roundToInt

object Quantities {
    private val fractions = listOf(
        0.25 to "¼", 1.0 / 3 to "⅓", 0.5 to "½", 2.0 / 3 to "⅔", 0.75 to "¾",
    )

    /** 1.5 -> "1½", 0.333 -> "⅓", 250.0 -> "250", 2.37 -> "2.4". */
    fun format(value: Double): String {
        if (value <= 0) return "0"
        if (value >= 20) return value.roundToInt().toString()
        val whole = floor(value)
        val rest = value - whole
        if (rest < 0.05) return whole.toInt().toString()
        if (rest > 0.95) return (whole.toInt() + 1).toString()
        fractions.firstOrNull { abs(it.first - rest) < 0.04 }?.let { (_, glyph) ->
            return if (whole == 0.0) glyph else "${whole.toInt()}$glyph"
        }
        return "%.1f".format(value).trimEnd('0').trimEnd('.', ',')
    }

    fun line(ingredient: IngredientLine, scale: Double): String {
        val amount = ingredient.amount
        return if (amount != null && amount > 0) {
            listOf(format(amount * scale), ingredient.unit).filter { it.isNotBlank() }.joinToString(" ")
        } else {
            ingredient.measure
        }
    }
}
