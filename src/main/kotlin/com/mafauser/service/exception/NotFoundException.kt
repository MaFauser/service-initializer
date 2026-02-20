package com.mafauser.service.exception

/**
 * Thrown when a resource does not exist.
 *
 * @param resource Human-readable resource name (e.g. "Example", "Product")
 * @param identifier The ID or key that was not found
 */
class NotFoundException(
    resource: String,
    identifier: Any,
) : RuntimeException("$resource not found: $identifier") {
    override val message: String get() = super.message!!
}
