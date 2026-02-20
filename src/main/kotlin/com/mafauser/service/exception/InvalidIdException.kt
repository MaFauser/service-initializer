package com.mafauser.service.exception

/**
 * Thrown when a GraphQL ID argument cannot be parsed (e.g. invalid UUID).
 */
class InvalidIdException(
    message: String,
) : RuntimeException(message) {
    override val message: String get() = super.message!!
}
