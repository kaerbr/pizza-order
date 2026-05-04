package com.example.pizzaorder

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Generic JSON persistence for filesDir.
 * Replaces both MenuRepo and OrderRepo with shared logic.
 */
class JsonStore<T>(
    context: Context,
    fileName: String,
    private val serializer: KSerializer<T>,
    private val defaultValue: T,
    private val pretty: Boolean = false
) {
    private val file = File(context.filesDir, fileName)
    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = pretty
    }

    suspend fun load(): T = withContext(Dispatchers.IO) {
        if (!file.exists()) {
            save(defaultValue)
            return@withContext defaultValue
        }
        runCatching { json.decodeFromString(serializer, file.readText()) }
            .getOrElse { defaultValue }
    }

    suspend fun save(data: T) = withContext(Dispatchers.IO) {
        file.writeText(json.encodeToString(serializer, data))
    }
}
