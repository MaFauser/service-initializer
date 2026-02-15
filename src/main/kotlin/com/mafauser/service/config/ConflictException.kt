package com.mafauser.service.config

/**
 * Thrown when a unique constraint would be violated (e.g. duplicate name). Mapped to 409 Conflict
 * by [GlobalExceptionHandler].
 *
 * @param resource Human-readable resource name (e.g. "Example", "Product")
 * @param field The field that would be duplicated
 * @param value The conflicting value
 */
class ConflictException(
    resource: String,
    field: String,
    value: Any,
) : RuntimeException("$resource already exists with $field: $value")
