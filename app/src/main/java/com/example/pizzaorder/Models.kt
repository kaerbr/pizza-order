package com.example.pizzaorder

import kotlinx.serialization.Serializable
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Serializable
data class Pizza(
    val name: String,
    val sauce: String,
    val toppings: List<String>,
) {
    val description: String get() = "$sauce · ${toppings.takeIf { it.isNotEmpty() }?.joinToString(", ") ?: "—"}"
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
