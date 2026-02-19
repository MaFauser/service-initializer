package com.mafauser.service.example

import java.time.Instant
import java.util.UUID

data class ExampleResponse(
    val id: UUID,
    val name: String,
    val description: String?,
    val version: Long,
    val createdAt: Instant,
    val updatedAt: Instant,
)

fun Example.toResponse() =
    ExampleResponse(
        id = id,
        name = name,
        description = description,
        version = version,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
