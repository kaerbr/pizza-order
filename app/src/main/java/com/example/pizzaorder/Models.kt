package com.example.pizzaorder

import kotlinx.serialization.Serializable
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Serializable
data class Sauce(val name: String)

@Serializable
data class Topping(val name: String)

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
    val completedAt: Long? = null
) {
    fun isDone() = completedAt != null
    val formattedTime: String get() = SimpleDateFormat("HH:mm", Locale.GERMAN).format(Date(id))
    val formattedNumber: String get() = "#%03d".format(number)
}
