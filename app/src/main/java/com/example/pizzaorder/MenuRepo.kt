package com.example.pizzaorder

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Persists the menu as JSON in filesDir. Mirrors OrderRepo deliberately —
 * same pattern, same trade-offs.
 *
 * On first launch the file doesn't exist; load() writes the DEFAULT to disk
 * and returns it. So the user's first visit to settings already shows
 * everything as editable, and the on-disk file is the single source of truth
 * from launch one.
 */
class MenuRepo(context: Context) {

    private val file: File = File(context.filesDir, "menu.json")

    private val json = Json {
        ignoreUnknownKeys = true   // forward-compat if MenuConfig grows fields later
        prettyPrint = true         // adb-pull-friendly; menu file is small
    }

    suspend fun load(): MenuConfig = withContext(Dispatchers.IO) {
        if (!file.exists()) {
            save(MenuConfig.DEFAULT)
            return@withContext MenuConfig.DEFAULT
        }
        runCatching { json.decodeFromString<MenuConfig>(file.readText()) }
            .getOrElse {
                // Corrupt file (manual edit gone wrong, schema change, etc.) —
                // fall back to defaults rather than refusing to launch.
                MenuConfig.DEFAULT
            }
    }

    suspend fun save(menu: MenuConfig) = withContext(Dispatchers.IO) {
        file.writeText(json.encodeToString(menu))
    }
}
