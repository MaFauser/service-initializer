package com.mafauser.service.example

import com.mafauser.service.config.InvalidIdException
import org.slf4j.LoggerFactory
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
    private val log = LoggerFactory.getLogger(javaClass)

    @QueryMapping
    fun examples(): List<Example> {
        log.info("GraphQL query: examples")
        return exampleService.findAll()
    }

    @QueryMapping
    fun example(
        @Argument id: String,
    ): Example? {
        log.info("GraphQL query: example id={}", id)
        return parseUuidOrNull(id)?.let { exampleService.findById(it) }
    }

    @MutationMapping
    fun createExample(
        @Argument input: CreateExampleInput,
    ): Example {
        log.info("GraphQL mutation: createExample name={}", input.name)
        return exampleService.create(input)
    }

    @MutationMapping
    fun updateExample(
        @Argument id: String,
        @Argument input: UpdateExampleInput,
    ): Example {
        log.info("GraphQL mutation: updateExample id={}", id)
        return exampleService.update(parseUuid(id, "updateExample"), input)
    }

    @MutationMapping
    fun deleteExample(
        @Argument id: String,
    ): Boolean {
        log.info("GraphQL mutation: deleteExample id={}", id)
        return exampleService.delete(parseUuid(id, "deleteExample"))
    }

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
            ?: throw InvalidIdException("$field id must be a valid UUID string, got: $id")
}
