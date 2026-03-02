package com.mafauser.service.config

import tools.jackson.databind.ObjectMapper
import jakarta.servlet.DispatcherType
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@DisplayName("RateLimitFilter")
class RateLimitFilterTest {
    private val filter = RateLimitFilter(RateLimitProperties(), ObjectMapper())

    @Test
    fun `uses fallback key when remoteAddr is null`() {
        val request = mock<HttpServletRequest>()
        whenever(request.remoteAddr).thenReturn(null)
        whenever(request.requestURI).thenReturn("/examples")
        whenever(request.dispatcherType).thenReturn(DispatcherType.REQUEST)
        val response = mock<HttpServletResponse>()
        val chain = mock<FilterChain>()

        filter.doFilter(request, response, chain)

        verify(chain).doFilter(request, response)
    }
}
