package com.mafauser.service.config

import io.micrometer.observation.ObservationPredicate
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.server.observation.ServerRequestObservationContext

/**
 * Skips tracing for actuator endpoints (health, prometheus, metrics, etc.) to avoid
 * spamming Grafana/Tempo with scrape-induced traces.
 */
@Configuration
class TracingConfiguration {

    @Bean
    fun actuatorExclusionPredicate(): ObservationPredicate =
        ObservationPredicate { name, context ->
            if (name == "http.server.requests" && context is ServerRequestObservationContext) {
                val path = context.carrier?.requestURI ?: return@ObservationPredicate true
                !path.startsWith("/actuator")
            } else {
                true
            }
        }
}
