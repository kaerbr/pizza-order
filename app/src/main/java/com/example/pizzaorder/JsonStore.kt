package com.example.pizzaorder

import android.content.Context
import androidx.core.util.AtomicFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileNotFoundException

/**
 * Generic JSON persistence for filesDir.
 * Replaces both MenuRepo and OrderRepo with shared logic.
 *
 * AtomicFile: a crash or dead battery mid-write leaves the previous file intact
 * instead of a half-written one (which load() would have replaced with the default).
 * Mutex: saves run one at a time in the order they were started, so an older
 * list can never overwrite a newer one.
 */
class JsonStore<T>(
    context: Context,
    fileName: String,
    private val serializer: KSerializer<T>,
    private val defaultValue: T,
    private val pretty: Boolean = false
) {
    private val file = AtomicFile(File(context.filesDir, fileName))
    private val mutex = Mutex()
    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = pretty
    }

    suspend fun load(): T = withContext(Dispatchers.IO) {
        val text = try {
            file.readFully().decodeToString()
        } catch (_: FileNotFoundException) {
            save(defaultValue)
            return@withContext defaultValue
        }
        runCatching { json.decodeFromString(serializer, text) }
            .getOrElse { defaultValue }
    }

    suspend fun save(data: T) = mutex.withLock {
        withContext(Dispatchers.IO) {
            val out = file.startWrite()
            try {
                out.write(json.encodeToString(serializer, data).toByteArray())
                file.finishWrite(out)
            } catch (e: Exception) {
                file.failWrite(out)
                throw e
            }
        }
    }
}
