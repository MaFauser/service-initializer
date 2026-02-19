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
        "bucket4j.enabled=true",
        "bucket4j.filters[0].rate-limits[0].bandwidths[0].capacity=3",
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
        }

        mockMvc
            .perform(get("/examples").with { it.apply { remoteAddr = "10.0.0.1" } })
            .andExpect(status().isTooManyRequests)
            .andExpect(
                content().json(
                    """{"status":429,"error":"Too Many Requests","message":"Rate limit exceeded. Try again later."}""",
                ),
            )
            .andExpect(header().string("Retry-After", "60"))
    }

    @Test
    fun `does not rate limit excluded paths`() {
        repeat(5) {
            mockMvc
                .perform(get("/actuator/health").with { it.apply { remoteAddr = "10.0.0.2" } })
                .andExpect(status().isOk)
        }
    }
}
