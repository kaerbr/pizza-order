package io.github.kaerbr.pizzaorder

/**
 * Pattern gate for the admin panel. The pattern is compiled in: a "P" on the
 * 3×3 grid, dots numbered
 *
 *     0 1 2        ●───●
 *     3 4 5        │   │
 *     6 7 8        ●───●
 *                  │
 *                  ●
 *
 * drawn as one stroke 3→4→1→0→6 (the last move runs back through 3, which
 * is already used and therefore skipped, like on Android's lock screen).
 * After [MAX_ATTEMPTS] wrong tries the gate locks for [LOCK_MS].
 *
 * ponytail: compiled-in pattern keeps party guests out, not someone who decompiles the APK.
 * Lockout counter lives in memory, so an app restart resets it.
 */
class AdminLock(private val clock: () -> Long = System::currentTimeMillis) {
    private var failed = 0
    private var lockedUntil = 0L

    /** @return null when unlocked, otherwise the message to show. */
    fun unlock(pattern: List<Int>): String? {
        val now = clock()
        if (now < lockedUntil) return "Zu viele Versuche. Noch ${(lockedUntil - now + 999) / 1000} s warten."
        if (pattern == PATTERN) {
            failed = 0
            return null
        }
        if (++failed >= MAX_ATTEMPTS) {
            failed = 0
            lockedUntil = now + LOCK_MS
            return "Zu viele Versuche. ${LOCK_MS / 1000} s gesperrt."
        }
        return "Falsches Muster."
    }

    companion object {
        val PATTERN = listOf(3, 4, 1, 0, 6)
        const val MAX_ATTEMPTS = 5
        const val LOCK_MS = 30_000L
    }
}
