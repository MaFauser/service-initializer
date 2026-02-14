package com.mafauser.service

import jakarta.persistence.Column
import jakarta.persistence.Id
import jakarta.persistence.MappedSuperclass
import java.time.Instant
import java.util.UUID
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp

/**
 * Base for JPA entities: default [id], [createdAt], [updatedAt].
 * Hibernate sets timestamps automatically via [CreationTimestamp] / [UpdateTimestamp].
 */
@MappedSuperclass
abstract class BaseEntity(
    @Id
    @Column(columnDefinition = "uuid", updatable = false)
    open val id: UUID = UUID.randomUUID(),
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    open var createdAt: Instant = Instant.EPOCH,
    @UpdateTimestamp
    @Column(nullable = false)
    open var updatedAt: Instant = Instant.EPOCH,
)
