package io.github.kaerbr.pizzaorder

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class MenuConfigTest {

    // Runs before every debug build (app/build.gradle.kts): a preset with an unknown sauce or
    // topping fails the build here instead of crashing the app on start.
    @Test
    fun standardMenuIsValid() {
        MenuConfig.Standard.menu
    }

    // Regression: diets were keyed by enum constant ("KAESE") instead of the name on the menu ("Käse"),
    // so every lookup missed and all diet badges vanished.
    @Test
    fun standardDietsCoverExactlyTheStandardIngredients() {
        val menu = MenuConfig.Standard.menu
        assertEquals(menu.sauces.map { it.name }.toSet() + menu.toppings.map { it.name }, menu.diets.keys)
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
