package com.mafauser.service.config

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

/**
 * Logs HTTP requests: method, path, status, and duration.
 * Logs appear in stdout and can be viewed via `kubectl logs` or any log aggregator (OpenSearch, CloudWatch, etc.).
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
class RequestLoggingFilter : OncePerRequestFilter() {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val start = System.currentTimeMillis()
        try {
            filterChain.doFilter(request, response)
        } finally {
            val duration = System.currentTimeMillis() - start
            val method = request.method
            val path = request.requestURI
            val status = response.status
            val query = request.queryString?.let { "?$it" } ?: ""
            log.info("{} {}{} {} {}ms", method, path, query, status, duration)
        }
    }
}
