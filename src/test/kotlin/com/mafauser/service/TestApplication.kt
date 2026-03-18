package com.mafauser.service

import org.springframework.boot.fromApplication
import org.springframework.boot.with

/**
 * Alternative entry point for IDE runs: uses Testcontainers (Postgres, Redis, Kafka) instead of
 * external Docker. Use when debugging against the full stack without `docker compose up`.
 */
fun main(args: Array<String>) {
    fromApplication<Application>().with(TestcontainersConfiguration::class).run(*args)
}
