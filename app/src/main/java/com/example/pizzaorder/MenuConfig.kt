package com.example.pizzaorder

import kotlinx.serialization.Serializable

@Serializable
data class MenuConfig(
    val sauces: LinkedHashSet<Sauce> = linkedSetOf(),
    val toppings: LinkedHashSet<Topping> = linkedSetOf(),
    val presets: LinkedHashSet<Pizza> = linkedSetOf(),
    // Default here (not just in DEFAULT) so menu.json files saved before this field existed get the links too.
    val funLinks: List<FunLink> = DEFAULT_FUN_LINKS,
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

    fun withFunLink(link: FunLink) = copy(funLinks = funLinks + link)

    fun withoutFunLink(link: FunLink) = copy(funLinks = funLinks - link)

    companion object {
        // YouTube *search* links on purpose: they never go dead like single video IDs do.
        val DEFAULT_FUN_LINKS = listOf(
            FunLink("Mathe beweist: fair teilen!",       "https://de.wikipedia.org/wiki/Pizza-Theorem"),
            FunLink("Der kleine Tisch im Karton",        "https://en.wikipedia.org/wiki/Pizza_saver"),
            FunLink("Ananas: Krieg der Welten",          "https://en.wikipedia.org/wiki/Hawaiian_pizza"),
            FunLink("Warum Pizza erst in den USA berühmt wurde", "https://en.wikipedia.org/wiki/Pizza_effect"),
            FunLink("Pizza",                             "https://de.wikipedia.org/wiki/Pizza"),
            FunLink("PizzaGate",                         "https://de.wikipedia.org/wiki/Pizzagate"),
            FunLink("Pizza Karton",                      "https://de.wikipedia.org/wiki/Pizzakarton"),
            FunLink("Neapolitanische Pizza",             "https://de.wikipedia.org/wiki/Neapolitanische_Pizza"),
            // Short youtu.be URLs keep the QR code coarse for thermal printers.
            FunLink("Die beste NAPOLI PIZZA die DU zuhause backen kannst!", "https://youtu.be/mVJkdqTeSQ4"),
            FunLink("SO machst DU eine perfekte Napoli Pizza mit dem 0,59€ Mehl!", "https://youtu.be/1QR5NQj52Po"),
            FunLink("Wie man Pizza macht | SWR Handwerkskunst", "https://youtu.be/vf0god0LtP8"),
            FunLink("A day running my pizza truck at St Albans market", "https://youtu.be/PQJ6xljo0-U"),
            FunLink("The Peddling Pizza Neapolitan dough recipe.", "https://youtu.be/ObYx4ofdVCc"),
            FunLink("3 Fehler beim Pizza backen, die jeder macht!", "https://youtu.be/wGY3oaerRgI"),
            FunLink("Gelingsicher! 24 Stunden Pizzateig ganz einfach für zu Hause.\uD83C\uDF55", "https://youtu.be/JQ3nO5t0GNI"),
            FunLink("Das geheime Teigrezept",            "https://youtu.be/dQw4w9WgXcQ"),
        )

        val DEFAULT_SAUCE = Sauce("Tomate")
        val DEFAULT = MenuConfig(
            sauces = linkedSetOf(DEFAULT_SAUCE, Sauce("Pesto al basilico")),
            toppings = linkedSetOf(
                Topping("Zwiebeln"),
                Topping("Knoblauch"),
                Topping("Salami"),
                Topping("Oliven"),
                Topping("Käse"),
                Topping("Rucola"),
                Topping("Basilikum"),
                Topping("Kirschtomaten"),
                Topping("Pinienkerne"),
            ),
            presets = linkedSetOf(
                Pizza("Speciale",    Sauce("Tomate"), linkedSetOf(Topping("Zwiebeln"), Topping("Knoblauch"), Topping("Rucola"))),
                Pizza("Margherita",  Sauce("Tomate"), linkedSetOf(Topping("Käse"))),
                Pizza("Salami",      Sauce("Tomate"), linkedSetOf(Topping("Salami"), Topping("Käse"))),
                Pizza("Pizza Verde", Sauce("Pesto al basilico"), linkedSetOf(Topping("Kirschtomaten"), Topping("Pinienkerne"))),
            ),
        )
    }
}
