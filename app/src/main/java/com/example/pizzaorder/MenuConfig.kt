package com.example.pizzaorder

import kotlinx.serialization.Serializable

/**
 * The editable menu. Persisted as JSON in filesDir, loaded on app start, mutated
 * via the SettingsScreen.
 *
 * Why a data class and not strings.xml / Room / DataStore?
 * - Serializable + immutable: state changes are dirt-simple (.copy(...)).
 * - One blob → one file. Same pattern as OrderRepo, no new persistence machinery.
 * - DEFAULT below is the "ship-with-the-app" value; first launch writes it to
 *   disk so future launches always read from a real file.
 */
@Serializable
data class MenuConfig(
    val sauces: List<String>,
    val toppings: List<String>,
    val presets: List<Pizza>,
) {
    companion object {
        val DEFAULT = MenuConfig(
            sauces = listOf(
                "Tomate",
                "Crème fraîche",
                "BBQ",
            ),
            toppings = listOf(
                "Mozzarella",
                "Salami",
                "Schinken",
                "Champignons",
                "Paprika",
                "Zwiebeln",
                "Oliven",
                "Ananas",
                "Rucola",
                "Thunfisch",
                "Ei",
                "Peperoni",
            ),
            presets = listOf(
                Pizza("Margherita",       "Tomate", listOf("Mozzarella")),
                Pizza("Salami",           "Tomate", listOf("Mozzarella", "Salami")),
                Pizza("Prosciutto",       "Tomate", listOf("Mozzarella", "Schinken")),
                Pizza("Funghi",           "Tomate", listOf("Mozzarella", "Champignons")),
                Pizza("Hawaii",           "Tomate", listOf("Mozzarella", "Schinken", "Ananas")),
                Pizza("Tonno",            "Tomate", listOf("Mozzarella", "Thunfisch", "Zwiebeln")),
                Pizza("Diavola",          "Tomate", listOf("Mozzarella", "Salami", "Peperoni")),
                Pizza("Quattro Formaggi", "Tomate", listOf("Mozzarella")),
                Pizza("Vegetariana",      "Tomate", listOf("Mozzarella", "Paprika", "Champignons", "Zwiebeln", "Oliven")),
                Pizza("Bianca",           "Crème fraîche", listOf("Mozzarella", "Schinken", "Rucola")),
            ),
        )
    }
}
