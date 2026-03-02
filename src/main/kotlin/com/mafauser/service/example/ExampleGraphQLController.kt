package com.mafauser.service.example

import com.mafauser.service.exception.InvalidIdException
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.validation.Valid
import org.springframework.data.domain.PageRequest
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.stereotype.Controller
import org.springframework.validation.annotation.Validated
import java.util.UUID

@Controller
@Validated
class ExampleGraphQLController(
    private val exampleService: ExampleService,
) {
    private val log = KotlinLogging.logger {}

    @QueryMapping
    fun examples(
        @Argument page: Int?,
        @Argument size: Int?,
    ): List<ExampleResponse> {
        log.debug { "GraphQL query: examples page=$page size=$size" }
        val pageable = PageRequest.of(page ?: 0, (size ?: 20).coerceIn(1, 100))
        return exampleService.findAll(pageable).content.map { it.toResponse() }
    }

    @QueryMapping
    fun example(
        @Argument id: String,
    ): ExampleResponse? {
        log.debug { "GraphQL query: example id=$id" }
        return parseUuidOrNull(id)?.let { exampleService.findById(it)?.toResponse() }
    }

    @MutationMapping
    fun createExample(
        @Argument @Valid input: CreateExampleInput,
    ): ExampleResponse {
        log.debug { "GraphQL mutation: createExample name=${input.name}" }
        return exampleService.create(input).toResponse()
    }

    @MutationMapping
    fun updateExample(
        @Argument id: String,
        @Argument @Valid input: UpdateExampleInput,
    ): ExampleResponse {
        log.debug { "GraphQL mutation: updateExample id=$id" }
        return exampleService.update(parseUuid(id, "updateExample"), input).toResponse()
    }

    @MutationMapping
    fun deleteExample(
        @Argument id: String,
    ): Boolean {
        log.debug { "GraphQL mutation: deleteExample id=$id" }
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
