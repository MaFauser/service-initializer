package com.mafauser.service.exception

import graphql.ErrorClassification
import graphql.GraphQLError
import graphql.schema.DataFetchingEnvironment
import jakarta.validation.ConstraintViolationException
import org.slf4j.LoggerFactory
import org.springframework.graphql.execution.DataFetcherExceptionResolverAdapter
import org.springframework.graphql.execution.ErrorType
import org.springframework.stereotype.Component

@Component
class GraphQLExceptionResolver : DataFetcherExceptionResolverAdapter() {
    private val log = LoggerFactory.getLogger(javaClass)

    private enum class CustomErrorType : ErrorClassification {
        CONFLICT,
    }

    override fun resolveToSingleError(
        ex: Throwable,
        env: DataFetchingEnvironment,
    ): GraphQLError =
        when (ex) {
            is NotFoundException -> {
                toGraphQLError(ex.message, ErrorType.NOT_FOUND)
            }

            is ConflictException -> {
                toGraphQLError(ex.message, CustomErrorType.CONFLICT)
            }

            is InvalidIdException -> {
                toGraphQLError(ex.message, ErrorType.BAD_REQUEST)
            }

            is ConstraintViolationException -> {
                toGraphQLError(
                    ex.constraintViolations.joinToString("; ") { "${it.propertyPath}: ${it.message}" },
                    ErrorType.BAD_REQUEST,
                )
            }

            else -> {
                log.error("Unhandled GraphQL exception on field {}", env.field.name, ex)
                toGraphQLError("An unexpected error occurred", ErrorType.INTERNAL_ERROR)
            }
        }

    private fun toGraphQLError(
        message: String,
        classification: ErrorClassification,
    ): GraphQLError =
        GraphQLError
            .newError()
            .message(message)
            .errorType(classification)
            .build()
}
