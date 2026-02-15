package com.mafauser.service.config

import io.micrometer.core.instrument.config.MeterFilter
import io.micrometer.observation.ObservationPredicate
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.server.observation.ServerRequestObservationContext

/**
 * Filters out noisy endpoints from tracing, logging, and metrics to avoid
 * spamming Grafana/Tempo/dashboards with health probes and scrape-induced traffic.
 */
@Configuration
class TracingConfiguration {
    @Bean
    fun actuatorExclusionPredicate(): ObservationPredicate =
        ObservationPredicate { name, context ->
            if (name == "http.server.requests" && context is ServerRequestObservationContext) {
                val path = context.carrier?.requestURI ?: return@ObservationPredicate true
                !NoiseExclusions.paths.any { path.startsWith(it) }
            } else {
                true
            }
        }

    @Bean
    fun excludeNoisyUrisFromMetrics(): MeterFilter =
        MeterFilter.deny { id ->
            id.name == "http.server.requests" &&
                id.getTag("uri")?.let { uri ->
                    NoiseExclusions.paths.any { uri.startsWith(it) }
                } ?: false
        }
}
