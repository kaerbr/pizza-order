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
    fun withSauce(name: String) = copy(sauces = sauces + name)
    fun withoutSauce(name: String): MenuConfig {
        val next = sauces - name
        val fallback = next.firstOrNull() ?: ""
        return copy(
            sauces = next,
            presets = presets.map { if (it.sauce == name) it.copy(sauce = fallback) else it }
        )
    }

    fun withTopping(name: String) = copy(toppings = toppings + name)
    fun withoutTopping(name: String) = copy(
        toppings = toppings - name,
        presets = presets.map { it.copy(toppings = it.toppings - name) }
    )

    fun withPreset(pizza: Pizza) = copy(presets = presets + pizza)
    fun withoutPreset(name: String) = copy(presets = presets.filter { it.name != name })

    companion object {
        val DEFAULT = MenuConfig(
            sauces = listOf(
                "Tomate",
            ),
            toppings = listOf(
                "Zwiebeln",
                "Knoblauch",
                "Salami",
                "Mozzarella",
                "Käse",
                "Oliven",
                "Rucola",
                "Artischocken",
                "Basilikum",
            ),
            presets = listOf(
                Pizza("Speciale",         "Tomate", listOf("Zwiebeln", "Knoblauch", "Rucola")),
                Pizza("Margherita",       "Tomate", listOf("Käse")),
                Pizza("Neapolitana",      "Tomate", listOf("Mozarella", "Basilikum")),
                Pizza("Salami",           "Tomate", listOf("Salami", "Käse")),
                Pizza("Vegetariana",      "Tomate", listOf("Paprika", "Zwiebeln", "Oliven", "Artischocken", "Käse")),
            ),
        )
    }
}
