package com.mafauser.service.config

import com.github.benmanes.caffeine.cache.Caffeine
import io.github.bucket4j.Bandwidth
import io.github.bucket4j.Bucket
import io.github.bucket4j.ConsumptionProbe
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.time.Duration

@Component
class RateLimitFilter(
    private val properties: RateLimitProperties,
) : OncePerRequestFilter() {
    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        private const val RESPONSE_BODY =
            """{"type":"about:blank","title":"Too Many Requests","status":429,"detail":"Rate limit exceeded. Try again later."}"""
        private const val RESPONSE_STATUS = 429
        private const val RESPONSE_LIMIT_HEADER = "X-RateLimit-Limit"
        private const val RESPONSE_REMAINING_HEADER = "X-RateLimit-Remaining"
        private const val RESPONSE_RETRY_AFTER_HEADER = "Retry-After"
        private const val RESPONSE_CONTENT_TYPE = "application/problem+json"
        private const val RESPONSE_CHARACTER_ENCODING = "UTF-8"
    }

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

        response.setIntHeader(RESPONSE_LIMIT_HEADER, properties.requestsPerMinute)
        response.setIntHeader(RESPONSE_REMAINING_HEADER, probe.remainingTokens.toInt())

        if (probe.isConsumed) {
            chain.doFilter(request, response)
        } else {
            log.debug("Rate limit exceeded for {}", request.remoteAddr)
            val retryAfterSeconds = (probe.nanosToWaitForRefill / 1_000_000_000).coerceAtLeast(1)
            val bytes = RESPONSE_BODY.toByteArray(Charsets.UTF_8)
            response.status = RESPONSE_STATUS
            response.setHeader(RESPONSE_RETRY_AFTER_HEADER, retryAfterSeconds.toString())
            response.contentType = RESPONSE_CONTENT_TYPE
            response.characterEncoding = RESPONSE_CHARACTER_ENCODING
            response.setContentLength(bytes.size)
            response.outputStream.write(bytes)
            response.flushBuffer()
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
