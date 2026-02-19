package com.mafauser.service

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@TestPropertySource(
    properties = [
        "rate-limit.enabled=true",
        "rate-limit.requests-per-minute=3",
    ],
)
@DisplayName("Rate Limiting (integration)")
class RateLimitIntegrationTest : BaseIntegrationTest() {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Test
    fun `returns 429 with proper body and headers when rate limit is exceeded`() {
        repeat(3) {
            mockMvc
                .perform(get("/examples").with { it.apply { remoteAddr = "10.0.0.1" } })
                .andExpect(status().isOk)
                .andExpect(header().exists("X-RateLimit-Limit"))
                .andExpect(header().exists("X-RateLimit-Remaining"))
        }

        mockMvc
            .perform(get("/examples").with { it.apply { remoteAddr = "10.0.0.1" } })
            .andExpect(status().isTooManyRequests)
            .andExpect(
                content().json(
                    """{"status":429,"error":"Too Many Requests","message":"Rate limit exceeded. Try again later."}""",
                ),
            ).andExpect(header().string("Retry-After", "60"))
            .andExpect(header().string("X-RateLimit-Remaining", "0"))
    }

    @Test
    fun `does not rate limit excluded paths`() {
        repeat(5) {
            mockMvc
                .perform(get("/actuator/health").with { it.apply { remoteAddr = "10.0.0.2" } })
                .andExpect(status().isOk)
        }
    }

    @Test
    fun `rate limits independently per IP address`() {
        repeat(3) {
            mockMvc
                .perform(get("/examples").with { it.apply { remoteAddr = "10.0.0.3" } })
                .andExpect(status().isOk)
        }
        mockMvc
            .perform(get("/examples").with { it.apply { remoteAddr = "10.0.0.3" } })
            .andExpect(status().isTooManyRequests)

        mockMvc
            .perform(get("/examples").with { it.apply { remoteAddr = "10.0.0.4" } })
            .andExpect(status().isOk)
    }
}
