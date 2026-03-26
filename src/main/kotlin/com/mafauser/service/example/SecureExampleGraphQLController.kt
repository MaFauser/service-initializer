package com.mafauser.service.example

import com.mafauser.service.exception.InvalidIdException
import com.mafauser.service.security.IsAuthenticated
import com.mafauser.service.security.Roles
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.data.domain.PageRequest
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.security.access.annotation.Secured
import org.springframework.stereotype.Controller
import java.util.UUID

@Controller
@IsAuthenticated
class SecureExampleGraphQLController(
    private val exampleService: ExampleService,
) {
    private val log = KotlinLogging.logger {}

    @QueryMapping
    fun secureExamples(
        @Argument page: Int?,
        @Argument size: Int?,
    ): List<ExampleResponse> {
        log.debug { "GraphQL query: secureExamples" }
        val pageable = PageRequest.of(page ?: 0, (size ?: 20).coerceIn(1, 100))
        return exampleService.findAll(pageable).content.map { it.toResponse() }
    }

    @QueryMapping
    @Secured(Roles.ROLE_USER, Roles.ROLE_ADMIN)
    fun secureExample(
        @Argument id: String,
    ): ExampleResponse {
        log.debug { "GraphQL query: secureExample id=$id" }
        return exampleService.findById(parseUuid(id)).toResponse()
    }

    @MutationMapping
    @Secured(Roles.ROLE_ADMIN)
    fun secureDeleteExample(
        @Argument id: String,
    ): Boolean {
        log.debug { "GraphQL mutation: secureDeleteExample id=$id" }
        exampleService.delete(parseUuid(id))
        return true
    }

    private fun parseUuid(id: String): UUID =
        try {
            UUID.fromString(id)
        } catch (_: IllegalArgumentException) {
            throw InvalidIdException("id must be a valid UUID string, got: $id")
        }
}
