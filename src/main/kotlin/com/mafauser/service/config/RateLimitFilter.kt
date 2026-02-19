package com.mafauser.service.config

import com.github.benmanes.caffeine.cache.Caffeine
import io.github.bucket4j.Bandwidth
import io.github.bucket4j.Bucket
import io.github.bucket4j.ConsumptionProbe
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.time.Duration

@Component
class RateLimitFilter(
    private val properties: RateLimitProperties,
) : OncePerRequestFilter() {
    private val buckets =
        Caffeine
            .newBuilder()
            .maximumSize(10_000)
            .expireAfterAccess(Duration.ofMinutes(5))
            .build<String, Bucket>()

    override fun shouldNotFilter(request: HttpServletRequest): Boolean =
        !properties.enabled || NoiseExclusions.paths.any { request.requestURI.startsWith(it) }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        chain: FilterChain,
    ) {
        val bucket = buckets.get(request.remoteAddr ?: "unknown") { newBucket() }
        val probe: ConsumptionProbe = bucket.tryConsumeAndReturnRemaining(1)

        response.setIntHeader("X-RateLimit-Limit", properties.requestsPerMinute)
        response.setIntHeader("X-RateLimit-Remaining", probe.remainingTokens.toInt())

        if (probe.isConsumed) {
            chain.doFilter(request, response)
        } else {
            val retryAfterSeconds = (probe.nanosToWaitForRefill / 1_000_000_000).coerceAtLeast(1)
            response.status = 429
            response.setHeader("Retry-After", retryAfterSeconds.toString())
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
