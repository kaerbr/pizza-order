package io.github.kaerbr.pizzaorder

import kotlinx.coroutines.delay
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/**
 * [JsonStore] without Android: same JSON, same "keep the last good file" idea, plain java.io.File.
 *
 * Shared by the tests that need a real desk: the service, the HTTP server and the menu all talk files.
 */
class FileStore<T>(
    private val file: File,
    private val serializer: KSerializer<T>,
    private val defaultValue: T,
) : JsonFile<T> {
    // Same settings as JsonStore, or a test would pass on files the app never writes.
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    override suspend fun load(): T =
        if (file.exists()) runCatching { json.decodeFromString(serializer, file.readText()) }.getOrElse { defaultValue }
        else defaultValue

    override suspend fun save(data: T) {
        file.parentFile?.mkdirs()
        // Write-then-rename, like the app's AtomicFile: a test that reads while a print's save is in flight
        // sees the previous file, never half of the next one.
        //
        // One atomic replace, not File.renameTo: on Windows that refuses an existing target, and a
        // delete-then-copy fallback leaves a moment in which the file is not there at all. The temp file has
        // its own name per save, so two saves in flight never write into each other's.
        val next = File.createTempFile("${file.name}.", ".tmp", file.parentFile)
        next.writeText(json.encodeToString(serializer, data))
        replace(next, file)
    }

    /**
     * Windows may refuse the move for the instant another thread has the target open; a few short retries
     * cover that, and the last attempt lets the real error through.
     */
    private suspend fun replace(next: File, target: File) {
        repeat(REPLACE_TRIES - 1) {
            try {
                Files.move(next.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
                return
            } catch (_: IOException) {
                delay(10)
            }
        }
        Files.move(next.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
    }

    private companion object {
        const val REPLACE_TRIES = 50
    }
}

/** Counts printouts instead of printing them; Bluetooth is not what these tests are about. */
class RecordingPrinter : PrinterPort {
    val markups = mutableListOf<String>()
    override suspend fun print(markup: String): Result<Unit> {
        markups += markup
        return Result.success(Unit)
    }
}
