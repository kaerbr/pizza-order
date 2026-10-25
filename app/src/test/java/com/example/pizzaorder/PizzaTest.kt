package com.example.pizzaorder

import org.junit.Assert.assertEquals
import org.junit.Test

class PizzaTest {

    // Regression: 0 or 1 topping used to crash ordering (ClassCastException on the set type).
    @Test
    fun toppingsOfKeepsOrderForAnyCount() {
        for (names in listOf(emptyList(), listOf("Käse"), listOf("Salami", "Käse", "Rucola"))) {
            val set: LinkedHashSet<Topping> = Pizza.toppingsOf(names)
            assertEquals(LinkedHashSet::class.java, set.javaClass)
            assertEquals(names, set.map { it.name })
        }
    }

    @Test
    fun dotRowFillsExactlyOneLine() {
        assertEquals(32, dotRow("Gast", "Laura").removePrefix("[L]").replace("<b>", "").replace("</b>", "").length)
    }
}
