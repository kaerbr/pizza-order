package com.example.pizzaorder

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Persists the full order list as one JSON file in the app's private filesDir.
 *
 * Why not Room/SQLite/DataStore?
 * - We have ONE blob (List<Order>) and party-scale data (≤ a few hundred entries).
 * - JSON read/write is faster to write, debug, and inspect (`adb pull` → readable file).
 * - DataStore is built around key/value preferences; we'd just be storing one big
 *   string under one key, which is exactly what File already does, plus indirection.
 *
 * Not crash-safe in the strict sense (no atomic rename, no fsync). For a party that's
 * acceptable; if you ever care, swap writeText for File("...tmp").apply{...}.renameTo(file).
 */
class OrderRepo(context: Context) {

    private val file: File = File(context.filesDir, "orders.json")

    private val json = Json {
        ignoreUnknownKeys = true   // forward-compat if the model grows fields later
        prettyPrint = false
    }

    suspend fun load(): List<Order> = withContext(Dispatchers.IO) {
        if (!file.exists()) return@withContext emptyList()
        runCatching { json.decodeFromString<List<Order>>(file.readText()) }
            .getOrElse {
                // Corrupt/incompatible file — start fresh rather than crash on launch.
                emptyList()
            }
    }

    suspend fun save(orders: List<Order>) = withContext(Dispatchers.IO) {
        file.writeText(json.encodeToString(orders))
    }
}
