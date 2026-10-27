package io.github.kaerbr.pizzaorder

import kotlinx.serialization.Serializable

@Serializable
data class MenuConfig(
    val sauces: LinkedHashSet<Sauce> = linkedSetOf(),
    val toppings: LinkedHashSet<Topping> = linkedSetOf(),
    val presets: LinkedHashSet<Pizza> = linkedSetOf(),
    // Default here (not just in Standard.menu) so menu.json files saved before this field existed get the links too.
    val funLinks: List<FunLink> = Standard.funLinks,
    // Keyed by ingredient name instead of a field on Sauce/Topping: those get rebuilt from names
    // (OrderScreen) and compared by equality, a diet field would break every `in` check.
    // Unlisted = Diet.NONE, so nothing is ever labelled vegan by accident.
    val diets: Map<String, Diet> = Standard.diets,
) {
    // Lists every problem at once, so one failure shows all typos. MenuConfigTest runs this on Standard.menu
    // before each debug build; a broken menu.json never gets here as a crash (JsonStore falls back to Standard.menu).
    init {
        val problems = presets.flatMap { pizza ->
            listOfNotNull(pizza.sauce.takeIf { it !in sauces }?.let { "Pizza \"${pizza.name}\": Sauce \"${it.name}\" fehlt in sauces" }) +
                pizza.toppings.filter { it !in toppings }.map { "Pizza \"${pizza.name}\": Belag \"${it.name}\" fehlt in toppings" }
        }
        require(problems.isEmpty()) { "Ungültiges Menü:\n" + problems.joinToString("\n") }
    }

    // 1. Creates ONE new LinkedHashSet with the current items
    // 2. Adds the new item to it
    // 3. Passes the new set to the copy constructor
    fun withSauce(sauce: Sauce) = copy(
        sauces = LinkedHashSet(sauces).apply { add(sauce) }
    )

    fun withoutSauce(sauce: Sauce): MenuConfig {
        val nextSauces = LinkedHashSet(sauces).apply { remove(sauce) }
        val fallback = nextSauces.firstOrNull() ?: Standard.sauce

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

    fun withDiet(name: String, diet: Diet) = copy(diets = diets + (name to diet))

    fun dietOf(name: String) = diets[name] ?: Diet.NONE

    /** As strict as its least strict ingredient; a bare crust counts as vegan. */
    fun dietOf(pizza: Pizza) = ingredients(pizza).maxOfOrNull { dietOf(it) } ?: Diet.VEGAN

    /** The ingredients that set [pizza]'s diet, e.g. "Käse" on a Margherita. Empty when vegan. */
    fun dietCulprits(pizza: Pizza): List<String> {
        val diet = dietOf(pizza)
        return if (diet == Diet.VEGAN) emptyList() else ingredients(pizza).filter { dietOf(it) == diet }
    }

    // Blank sauce = Eigenkreation before a sauce is picked.
    private fun ingredients(pizza: Pizza) = (listOf(pizza.sauce.name) + pizza.toppings.map { it.name }).filter(String::isNotBlank)

    /** The built-in menu: what a fresh install and "Standardmenü wiederherstellen" start with. */
    object Standard {
        // Declared before [menu]: building it reads these through the constructor defaults.
        // Keys via .sauce/.topping, the exact objects on the menu: an enum's own `name` is "KAESE", not "Käse".
        val diets = Sauce.entries.associate { it.sauce.name to it.diet } + Topping.entries.associate { it.topping.name to it.diet }

        // YouTube *search* links on purpose: they never go dead like single video IDs do.
        val funLinks = listOf(
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

        /** Fallback when a preset's sauce gets deleted. */
        val sauce = Sauce.TOMATE.sauce

        val menu = MenuConfig(
            // Every entry is on the menu, so a preset can't use one that's missing here.
            sauces = Sauce.entries.mapTo(LinkedHashSet()) { it.sauce },
            toppings = Topping.entries.mapTo(LinkedHashSet()) { it.topping },
            presets = linkedSetOf(
                preset("Speciale",    Sauce.TOMATE, Topping.ZWIEBELN, Topping.KNOBLAUCH, Topping.RUCOLA),
                preset("Margherita",  Sauce.TOMATE, Topping.KAESE),
                preset("Salami",      Sauce.TOMATE, Topping.SALAMI, Topping.KAESE),
                preset("Pizza Verde", Sauce.PESTO, Topping.KIRSCHTOMATEN, Topping.PINIENKERNE),
            ),
        )

        private fun preset(name: String, sauce: Sauce, vararg toppings: Topping) =
            Pizza(name, sauce.sauce, toppings.mapTo(LinkedHashSet()) { it.topping })

        // Typed on purpose: presets take these, not strings, so an ingredient that isn't declared
        // here is a compile error. Only the built-ins; Settings still adds plain Sauce/Topping by name.
        // Everything vegan for now (vegan cheese, pesto and salami); retag in Settings when that changes.
        private enum class Sauce(val label: String, val diet: Diet) {
            TOMATE("Tomate", Diet.VEGAN),
            PESTO("Pesto al basilico", Diet.VEGAN);

            val sauce get() = Sauce(label)
        }

        private enum class Topping(val label: String, val diet: Diet) {
            ZWIEBELN("Zwiebeln", Diet.VEGAN),
            KNOBLAUCH("Knoblauch", Diet.VEGAN),
            SALAMI("Salami", Diet.VEGAN),
            OLIVEN("Oliven", Diet.VEGAN),
            KAESE("Käse", Diet.VEGAN),
            RUCOLA("Rucola", Diet.VEGAN),
            BASILIKUM("Basilikum", Diet.VEGAN),
            KIRSCHTOMATEN("Kirschtomaten", Diet.VEGAN),
            PINIENKERNE("Pinienkerne", Diet.VEGAN);

            val topping get() = Topping(label)
        }
    }
}
