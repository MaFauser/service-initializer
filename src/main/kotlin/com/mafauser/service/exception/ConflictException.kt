package com.mafauser.service.exception

/**
 * Thrown when a unique constraint would be violated (e.g. duplicate name).
 *
 * @param resource Human-readable resource name (e.g. "Example", "Product")
 * @param field The field that would be duplicated
 * @param value The conflicting value
 */
class ConflictException(
    resource: String,
    field: String,
    value: Any,
) : RuntimeException("$resource already exists with $field: $value") {
    override val message: String get() = super.message!!
}
