package com.mafauser.service.example

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Version
import java.time.Instant
import java.util.UUID

/**
 * Example JPA entity for the Example domain.
 * Uses Kotlin JPA plugin (noarg + allOpen) so the class is a [data class] with a generated
 * no-arg constructor and open for Hibernate proxies. Equals/hashCode are identity-based (id only).
 */
@Entity(name = "examples")
data class Example(
    @Id
    @Column(columnDefinition = "uuid", updatable = false)
    val id: UUID = UUID.randomUUID(),
    @Column(nullable = false, length = 255)
    var name: String,
    @Column(length = 1024)
    var description: String? = null,
    @Version
    @Column(nullable = false)
    var version: Long = 0,
    @Column(nullable = false, updatable = false)
    var createdAt: Instant = Instant.now(),
    @Column(nullable = false)
    var updatedAt: Instant = Instant.now(),
) {
    override fun equals(other: Any?): Boolean = other is Example && id == other.id

    override fun hashCode(): Int = id.hashCode()

    override fun toString(): String = "Example(id=$id, name=$name)"
}
