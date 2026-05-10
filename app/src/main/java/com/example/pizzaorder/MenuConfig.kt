package com.example.pizzaorder

import kotlinx.serialization.Serializable

@Serializable
data class MenuConfig(
    val sauces: LinkedHashSet<Sauce> = linkedSetOf(),
    val toppings: LinkedHashSet<Topping> = linkedSetOf(),
    val presets: LinkedHashSet<Pizza> = linkedSetOf(),
) {
    init {
        presets.forEach {
            require(it.sauce in sauces) { "Pizza ${it.name} nutzt unbekannte Sauce: ${it.sauce.name}" }
            it.toppings.forEach {
                require(it in toppings) { "Pizza ${it.name} nutzt unbekannten Belag: ${it.name}" }
            }
        }
    }

    // 1. Creates ONE new LinkedHashSet with the current items
    // 2. Adds the new item to it
    // 3. Passes the new set to the copy constructor
    fun withSauce(sauce: Sauce) = copy(
        sauces = LinkedHashSet(sauces).apply { add(sauce) }
    )

    fun withoutSauce(sauce: Sauce): MenuConfig {
        val nextSauces = LinkedHashSet(sauces).apply { remove(sauce) }
        val fallback = nextSauces.firstOrNull() ?: DEFAULT_SAUCE

        return copy(
            sauces = nextSauces,
            presets = presets.mapTo(LinkedHashSet()) {
                if (it.sauce == sauce) it.copy(sauce = fallback) else it
            }
        )
    }

    fun withTopping(topping: Topping) = copy(
        toppings = LinkedHashSet(toppings).apply { add(topping) }
    )

    fun withoutTopping(topping: Topping) = copy(
        toppings = LinkedHashSet(toppings).apply { remove(topping) },
        presets = presets.mapTo(LinkedHashSet()) {
            it.copy(toppings = LinkedHashSet(it.toppings).apply { remove(topping) })
        }
    )

    fun withPreset(pizza: Pizza) = copy(
        presets = LinkedHashSet(presets).apply { add(pizza) }
    )

    fun withoutPreset(name: String) = copy(
        presets = presets.filterTo(LinkedHashSet()) { it.name != name }
    )

    companion object {
        val DEFAULT_SAUCE = Sauce("Tomate")
        val DEFAULT = MenuConfig(
            sauces = linkedSetOf(DEFAULT_SAUCE),
            toppings = linkedSetOf(
                Topping("Zwiebeln"),
                Topping("Knoblauch"),
                Topping("Salami"),
                Topping("Oliven"),
                Topping("Käse"),
                Topping("Rucola"),
                Topping("Basilikum"),
            ),
            presets = linkedSetOf(
                Pizza("Speciale",    Sauce("Tomate"), linkedSetOf(Topping("Zwiebeln"), Topping("Knoblauch"), Topping("Rucola"))),
                Pizza("Margherita",  Sauce("Tomate"), linkedSetOf(Topping("Käse"))),
                Pizza("Salami",      Sauce("Tomate"), linkedSetOf(Topping("Salami"), Topping("Käse"))),
            ),
        )
    }
}
