package com.example.pizzaorder

import kotlinx.serialization.Serializable
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Serializable
data class Sauce(val name: String)

@Serializable
data class Topping(val name: String)

@Serializable
data class Pizza(
    val name: String,
    val sauce: Sauce,
    val toppings: LinkedHashSet<Topping>,
) {
    val description: String get() = "${sauce.name} · ${toppings.takeIf { it.isNotEmpty() }?.joinToString(", ") { it.name } ?: "—"}"
}

@Serializable
data class Order(
    val id: Long = System.currentTimeMillis(),
    val customer: String,
    val pizza: Pizza,
    val note: String = "",
    val completedAt: Long? = null
) {
    fun isDone() = completedAt != null
    val formattedTime: String get() = SimpleDateFormat("HH:mm", Locale.GERMAN).format(Date(id))
}
