package com.example.pizzaorder

import kotlinx.serialization.Serializable

/**
 * Pizza definition. Used both for menu presets and as the "what was ordered"
 * snapshot inside an Order. Snapshotting (storing names rather than menu refs)
 * means renaming a topping in MenuConfig later doesn't rewrite history —
 * old orders keep their original wording forever.
 */
@Serializable
data class Pizza(
    val name: String,
    val sauce: String,
    val toppings: List<String>,
)

/**
 * One order = one pizza for one person.
 *
 * @param completedAt  Whether the pizza has been served. Toggled from the orders list.
 *              Default false means existing orders.json files (written before
 *              this field existed) still deserialize cleanly — kotlinx.serialization
 *              uses the default for any missing field.
 */
@Serializable
data class Order(
    val id: Long = System.currentTimeMillis(),           // epoch millis; doubles as creation timestamp
    val customer: String,
    val pizza: Pizza,
    val note: String = "",
    val completedAt: Long? = null
) {
    fun isDone() = completedAt != null
}
