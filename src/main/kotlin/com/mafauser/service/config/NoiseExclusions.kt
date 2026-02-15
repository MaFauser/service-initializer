package com.mafauser.service.config

/**
 * Paths excluded from request logging, tracing, and metrics (health probes, favicon, etc.).
 */
object NoiseExclusions {
    val paths = setOf("/actuator", "/favicon.ico", "/error")
}
