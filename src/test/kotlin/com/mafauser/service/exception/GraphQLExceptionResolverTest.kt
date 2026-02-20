package com.mafauser.service.exception

import graphql.language.Field
import graphql.schema.DataFetchingEnvironment
import jakarta.validation.ConstraintViolation
import jakarta.validation.ConstraintViolationException
import jakarta.validation.Path
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.graphql.execution.ErrorType

@DisplayName("GraphQLExceptionResolver")
class GraphQLExceptionResolverTest {
    private val resolver = GraphQLExceptionResolver()

    private val env =
        mock<DataFetchingEnvironment>().also {
            whenever(it.field).thenReturn(Field.newField("testField").build())
        }

    @Test
    fun `resolves ConstraintViolationException to BAD_REQUEST`() {
        val path = mock<Path>()
        whenever(path.toString()).thenReturn("name")
        val violation = mock<ConstraintViolation<*>>()
        whenever(violation.propertyPath).thenReturn(path)
        whenever(violation.message).thenReturn("must not be blank")

        val errors = resolver.resolveException(ConstraintViolationException(setOf(violation)), env).block()

        assertNotNull(errors)
        assertEquals(1, errors!!.size)
        assertEquals(ErrorType.BAD_REQUEST, errors[0].errorType)
        assertEquals("name: must not be blank", errors[0].message)
    }

    @Test
    fun `resolves unexpected exception to INTERNAL_ERROR without leaking details`() {
        val errors = resolver.resolveException(IllegalStateException("db crashed"), env).block()

        assertNotNull(errors)
        assertEquals(1, errors!!.size)
        assertEquals(ErrorType.INTERNAL_ERROR, errors[0].errorType)
        assertEquals("An unexpected error occurred", errors[0].message)
    }
}
