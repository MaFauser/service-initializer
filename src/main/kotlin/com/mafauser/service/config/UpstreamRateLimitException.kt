package com.mafauser.service.config

class UpstreamRateLimitException(
    message: String = "External service rate limit exceeded. Try again later.",
) : RuntimeException(message)
