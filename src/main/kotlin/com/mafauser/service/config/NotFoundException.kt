package com.mafauser.service.config

/**
 * Thrown when a resource does not exist. Mapped to 404 Not Found by [GlobalExceptionHandler].
 *
 * @param resource Human-readable resource name (e.g. "Example", "Product")
 * @param identifier The ID or key that was not found
 */
class NotFoundException(
    resource: String,
    identifier: Any,
) : RuntimeException("$resource not found: $identifier")
