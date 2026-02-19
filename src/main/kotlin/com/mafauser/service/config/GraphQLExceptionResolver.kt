package com.mafauser.service.config

import graphql.ErrorClassification
import graphql.GraphQLError
import graphql.schema.DataFetchingEnvironment
import org.springframework.graphql.execution.DataFetcherExceptionResolverAdapter
import org.springframework.graphql.execution.ErrorType
import org.springframework.stereotype.Component

@Component
class GraphQLExceptionResolver : DataFetcherExceptionResolverAdapter() {
    private enum class CustomErrorType : ErrorClassification {
        CONFLICT,
    }

    override fun resolveToSingleError(
        ex: Throwable,
        env: DataFetchingEnvironment,
    ): GraphQLError? =
        when (ex) {
            is NotFoundException -> toGraphQLError(ex.message, ErrorType.NOT_FOUND)
            is ConflictException -> toGraphQLError(ex.message, CustomErrorType.CONFLICT)
            is InvalidIdException -> toGraphQLError(ex.message, ErrorType.BAD_REQUEST)
            else -> null
        }

    private fun toGraphQLError(
        message: String?,
        classification: ErrorClassification,
    ): GraphQLError =
        GraphQLError
            .newError()
            .message(message ?: "Unknown error")
            .errorType(classification)
            .build()
}
