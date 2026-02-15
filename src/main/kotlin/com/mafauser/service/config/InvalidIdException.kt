package com.mafauser.service.config

/**
 * Thrown when a GraphQL ID argument cannot be parsed (e.g. invalid UUID). Mapped to 400 Bad Request
 * by [GlobalExceptionHandler].
 */
class InvalidIdException(
    message: String,
) : RuntimeException(message)
