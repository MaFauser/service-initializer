package com.mafauser.service.config

import io.micrometer.core.instrument.Meter
import io.micrometer.core.instrument.Tags
import io.micrometer.core.instrument.config.MeterFilterReply
import io.micrometer.observation.Observation
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.http.server.observation.ServerRequestObservationContext
import kotlin.test.assertEquals

@DisplayName("TracingConfiguration")
class TracingConfigurationTest {
    private val config = TracingConfiguration()

    @Nested
    @DisplayName("actuatorExclusionPredicate")
    inner class ActuatorExclusion {
        private val predicate = config.actuatorExclusionPredicate()

        @Test
        fun `allows non-HTTP observations through`() {
            assertTrue(predicate.test("custom.metric", Observation.Context()))
        }

        @Test
        fun `allows HTTP requests to application endpoints`() {
            val ctx = serverContext("/examples")
            assertTrue(predicate.test("http.server.requests", ctx))
        }

        @Test
        fun `excludes HTTP requests to actuator endpoints`() {
            val ctx = serverContext("/actuator/health")
            assertFalse(predicate.test("http.server.requests", ctx))
        }

        @Test
        fun `allows HTTP observation with non-server context through`() {
            assertTrue(predicate.test("http.server.requests", Observation.Context()))
        }

        @Test
        fun `allows through when carrier is null`() {
            val ctx = mock<ServerRequestObservationContext>()
            whenever(ctx.carrier).thenReturn(null)
            assertTrue(predicate.test("http.server.requests", ctx))
        }
    }

    @Nested
    @DisplayName("excludeNoisyUrisFromMetrics")
    inner class MetricsExclusion {
        private val filter = config.excludeNoisyUrisFromMetrics()

        @Test
        fun `denies metrics for actuator URIs`() {
            val id = meterId(Tags.of("uri", "/actuator/prometheus"))
            assertEquals(MeterFilterReply.DENY, filter.accept(id))
        }

        @Test
        fun `allows metrics for application URIs`() {
            val id = meterId(Tags.of("uri", "/examples"))
            assertEquals(MeterFilterReply.NEUTRAL, filter.accept(id))
        }

        @Test
        fun `allows metrics when uri tag is absent`() {
            val id = meterId(Tags.empty())
            assertEquals(MeterFilterReply.NEUTRAL, filter.accept(id))
        }

        @Test
        fun `allows metrics for non-HTTP meters`() {
            val id = Meter.Id("jvm.memory.used", Tags.empty(), null, null, Meter.Type.GAUGE)
            assertEquals(MeterFilterReply.NEUTRAL, filter.accept(id))
        }

        private fun meterId(tags: Tags): Meter.Id = Meter.Id("http.server.requests", tags, null, null, Meter.Type.TIMER)
    }

    private fun serverContext(uri: String): ServerRequestObservationContext {
        val request = mock<HttpServletRequest>()
        whenever(request.requestURI).thenReturn(uri)
        val response = mock<HttpServletResponse>()
        return ServerRequestObservationContext(request, response)
    }
}
