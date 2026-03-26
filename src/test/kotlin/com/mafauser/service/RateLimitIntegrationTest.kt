package com.mafauser.service

import com.mafauser.service.config.RateLimitFilter
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext

@TestPropertySource(
    properties = [
        "rate-limit.enabled=true",
        "rate-limit.requests-per-minute=3",
    ],
)
@DisplayName("Rate Limiting (integration)")
class RateLimitIntegrationTest : BaseIntegrationTest() {
    @Autowired
    private lateinit var context: WebApplicationContext

    @Autowired
    private lateinit var rateLimitFilter: RateLimitFilter

    private lateinit var mockMvc: MockMvc

    @BeforeEach
    fun setUp() {
        mockMvc =
            MockMvcBuilders
                .webAppContextSetup(context)
                .apply<DefaultMockMvcBuilder>(SecurityMockMvcConfigurers.springSecurity())
                .addFilter<DefaultMockMvcBuilder>(rateLimitFilter)
                .build()
    }

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
                    """{"title":"Too Many Requests","status":429,"detail":"Rate limit exceeded. Try again later."}""",
                ),
            ).andExpect(header().exists("Retry-After"))
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
