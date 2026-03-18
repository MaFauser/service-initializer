package com.mafauser.service.exception

import jakarta.validation.ConstraintViolation
import jakarta.validation.ConstraintViolationException
import jakarta.validation.Path
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@DisplayName("GlobalExceptionHandler")
class GlobalExceptionHandlerTest {
    private val handler = GlobalExceptionHandler()

    @Test
    fun `handleInvalidId returns 400 with detail`() {
        val result = handler.handleInvalidId(InvalidIdException("id must be a valid UUID"))
        assertEquals(400, result.status)
        assertEquals("id must be a valid UUID", result.detail)
    }

    @Test
    fun `handleConstraintViolation joins violations into detail`() {
        val path = mock<Path>()
        whenever(path.toString()).thenReturn("email")
        val violation = mock<ConstraintViolation<*>>()
        whenever(violation.propertyPath).thenReturn(path)
        whenever(violation.message).thenReturn("must be valid")

        val result = handler.handleConstraintViolation(ConstraintViolationException(setOf(violation)))
        assertEquals(400, result.status)
        assertEquals("email: must be valid", result.detail)
    }

    @Test
    fun `handleUpstreamRateLimit returns 429 with default message`() {
        val result = handler.handleUpstreamRateLimit(UpstreamRateLimitException())
        assertEquals(429, result.status)
        assertEquals("External service rate limit exceeded. Try again later.", result.detail)
    }

    @Test
    fun `handleGeneric returns 500 without leaking exception details`() {
        val result = handler.handleGeneric(RuntimeException("sensitive db info"))
        assertEquals(500, result.status)
        assertEquals("An unexpected error occurred", result.detail)
    }
}
