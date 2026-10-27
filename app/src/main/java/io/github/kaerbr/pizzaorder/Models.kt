package io.github.kaerbr.pizzaorder

import kotlinx.serialization.Serializable
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Serializable
data class Sauce(val name: String)

@Serializable
data class Topping(val name: String)

/** Ordered by strictness: a pizza gets the highest ordinal among its ingredients. */
@Serializable
enum class Diet(val label: String) { VEGAN("Vegan"), VEGETARIAN("Vegetarisch"), NONE("Nichts davon") }

/** QR code on the printout: [url] is what it opens; [teaser] is only a label in Settings, not printed. */
@Serializable
data class FunLink(val teaser: String, val url: String)

@Serializable
data class Pizza(
    val name: String,
    val sauce: Sauce,
    val toppings: LinkedHashSet<Topping>,
) {
    val description: String get() = "${sauce.name} · ${toppings.takeIf { it.isNotEmpty() }?.joinToString(", ") { it.name } ?: "—"}"

    companion object {
        // Always a real LinkedHashSet: toSet() hands back emptySet()/singleton for 0–1 items,
        // which made the old `as LinkedHashSet` cast crash on a Margherita.
        fun toppingsOf(names: List<String>): LinkedHashSet<Topping> = names.mapTo(LinkedHashSet()) { Topping(it) }
    }
}

@Serializable
data class Order(
    val id: Long = System.currentTimeMillis(),
    val number: Int = 0,
    val customer: String,
    val pizza: Pizza,
    val note: String = "",
    val completedAt: Long? = null,
    // Snapshot at order time, so later menu edits don't relabel what the kitchen already got.
    val diet: Diet = Diet.NONE,
) {
    fun isDone() = completedAt != null
    val formattedTime: String get() = SimpleDateFormat("HH:mm", Locale.GERMAN).format(Date(id))
    val formattedNumber: String get() = "#%03d".format(number)
}
