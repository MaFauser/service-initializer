package com.mafauser.service.config

import io.github.bucket4j.Bandwidth
import io.github.bucket4j.Bucket
import io.github.bucket4j.ConsumptionProbe
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap

@ConfigurationProperties(prefix = "rate-limit")
data class RateLimitProperties(
    val enabled: Boolean = true,
    val requestsPerMinute: Int = 60,
)

@Component
class RateLimitFilter(
    private val properties: RateLimitProperties,
) : OncePerRequestFilter() {
    private val buckets = ConcurrentHashMap<String, Bucket>()

    override fun shouldNotFilter(request: HttpServletRequest): Boolean =
        !properties.enabled || NoiseExclusions.paths.any { request.requestURI.startsWith(it) }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        chain: FilterChain,
    ) {
        val bucket = buckets.computeIfAbsent(request.remoteAddr ?: "unknown") { newBucket() }
        val probe: ConsumptionProbe = bucket.tryConsumeAndReturnRemaining(1)

        response.setIntHeader("X-RateLimit-Limit", properties.requestsPerMinute)
        response.setIntHeader("X-RateLimit-Remaining", probe.remainingTokens.toInt())

        if (probe.isConsumed) {
            chain.doFilter(request, response)
        } else {
            response.status = 429
            response.setHeader("Retry-After", "60")
            response.contentType = "application/json"
            response.writer.write(
                """{"status":429,"error":"Too Many Requests","message":"Rate limit exceeded. Try again later."}""",
            )
        }
    }

    private fun newBucket(): Bucket =
        Bucket
            .builder()
            .addLimit(
                Bandwidth
                    .builder()
                    .capacity(properties.requestsPerMinute.toLong())
                    .refillGreedy(properties.requestsPerMinute.toLong(), Duration.ofMinutes(1))
                    .build(),
            ).build()
}
