package com.mafauser.service

import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import

/**
 * Base for integration tests. Uses Testcontainers (Postgres, Redis, Kafka) and MOCK web environment.
 */
@Import(TestcontainersConfiguration::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
abstract class BaseIntegrationTest
