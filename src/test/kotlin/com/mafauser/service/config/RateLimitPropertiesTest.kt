package com.mafauser.service.config

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("RateLimitProperties")
class RateLimitPropertiesTest {
    @Test
    fun `defaults to enabled with 60 requests per minute`() {
        val defaults = RateLimitProperties()
        assertTrue(defaults.enabled)
        assertEquals(60, defaults.requestsPerMinute)
    }
}
