package com.mafauser.service.example

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

/**
 * Spring Data JPA repository for [Example].
 * Add custom query methods here; they are implemented automatically by Spring.
 */
interface ExampleRepository : JpaRepository<Example, UUID> {
    fun existsByName(name: String): Boolean
}
