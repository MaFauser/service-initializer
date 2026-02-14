package com.mafauser.service.example

import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.stereotype.Controller
import java.util.UUID

/**
 * GraphQL controller for the Example domain. Exposes queries and mutations for [Example] via
 * [graphql/example/schema.graphqls]. ID arguments are accepted as String (GraphQL ID) and parsed to
 * UUID for a clear error when invalid.
 */
@Controller
class ExampleGraphQLController(
    private val exampleService: ExampleService,
) {
    @QueryMapping fun ping(): String = "pong"

    @QueryMapping fun examples(): List<Example> = exampleService.findAll()

    @QueryMapping
    fun example(
        @Argument id: String,
    ): Example? = parseUuidOrNull(id)?.let { exampleService.findById(it) } ?: null

    @MutationMapping
    fun createExample(
        @Argument input: CreateExampleInput,
    ): Example = exampleService.create(input)

    @MutationMapping
    fun updateExample(
        @Argument id: String,
        @Argument input: UpdateExampleInput,
    ): Example = exampleService.update(parseUuid(id, "updateExample"), input)

    @MutationMapping
    fun deleteExample(
        @Argument id: String,
    ): Boolean = exampleService.delete(parseUuid(id, "deleteExample"))

    private fun parseUuidOrNull(id: String): UUID? =
        try {
            UUID.fromString(id)
        } catch (_: IllegalArgumentException) {
            null
        }

    private fun parseUuid(
        id: String,
        field: String,
    ): UUID =
        parseUuidOrNull(id)
            ?: throw IllegalArgumentException("$field id must be a valid UUID string, got: $id")
}
