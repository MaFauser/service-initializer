package com.mafauser.service.exception

class UpstreamRateLimitException(
    message: String = "External service rate limit exceeded. Try again later.",
) : RuntimeException(message) {
    override val message: String get() = super.message!!
}
