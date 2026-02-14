package com.mafauser.service.example

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

@DisplayName("Example")
class ExampleTest {
    @Test
    fun `equals uses reference equality`() {
        val id = UUID.randomUUID()
        val a = Example(id = id, name = "A")
        val b = Example(id = id, name = "B")
        assertFalse(a == b)
        assertFalse(a.equals(null))
        assertFalse(a.equals("not an Example"))
    }

    @Test
    fun `toString includes id and name`() {
        val id = UUID.fromString("00000000-0000-0000-0000-000000000001")
        val ex = Example(id = id, name = "Test")
        assertEquals("Example(id=00000000-0000-0000-0000-000000000001, name=Test)", ex.toString())
    }

    @Test
    fun `setters for version and timestamps are invoked`() {
        val ex = Example(name = "X")
        val now = Instant.now()
        ex.version = 1L
        ex.createdAt = now
        ex.updatedAt = now
        assertEquals(1L, ex.version)
        assertEquals(now, ex.createdAt)
        assertEquals(now, ex.updatedAt)
    }
}
