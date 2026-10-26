package com.example.pizzaorder

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class MenuConfigTest {

    // Runs before every debug build (app/build.gradle.kts): a preset with an unknown sauce or
    // topping fails the build here instead of crashing the app on start.
    @Test
    fun defaultMenuIsValid() {
        MenuConfig.DEFAULT
    }

    @Test
    fun invalidMenuListsEveryProblem() {
        val e = assertThrows(IllegalArgumentException::class.java) {
            MenuConfig(
                sauces = linkedSetOf(Sauce("Tomate")),
                toppings = linkedSetOf(Topping("Käse")),
                presets = linkedSetOf(Pizza("error", Sauce("not vorhanden"), Pizza.toppingsOf(listOf("Käse", "nicht vorhanden")))),
            )
        }
        assertEquals(
            "Ungültiges Menü:\n" +
                "Pizza \"error\": Sauce \"not vorhanden\" fehlt in sauces\n" +
                "Pizza \"error\": Belag \"nicht vorhanden\" fehlt in toppings",
            e.message,
        )
    }
}
