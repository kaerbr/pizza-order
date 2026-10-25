package com.example.pizzaorder

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class AdminLockTest {

    @Test
    fun onlyTheExactPatternOpensAndWrongPatternsLockOut() {
        var now = 0L
        val lock = AdminLock(clock = { now })

        assertNull(lock.unlock(listOf(3, 4, 1, 0, 6)))
        assertEquals("Falsches Muster.", lock.unlock(listOf(6, 0, 1, 4, 3)))  // same shape, backwards
        assertEquals("Falsches Muster.", lock.unlock(listOf(3, 4, 1, 0)))     // incomplete

        repeat(AdminLock.MAX_ATTEMPTS) { lock.unlock(listOf(0, 1, 2)) }
        assertNotNull(lock.unlock(AdminLock.PATTERN))           // locked: even the P is refused

        now += AdminLock.LOCK_MS
        assertNull(lock.unlock(AdminLock.PATTERN))
    }
}
