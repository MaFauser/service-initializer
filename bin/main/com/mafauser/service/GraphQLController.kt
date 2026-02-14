package com.mafauser.service

import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.stereotype.Controller

@Controller
class GraphQLController {
    @QueryMapping
    fun hello(
        @Argument name: String?,
    ): String = "Hello, ${name ?: "World"}!"

    @QueryMapping
    fun ping(): String = "pong"

    @MutationMapping
    fun echo(
        @Argument message: String,
    ): String = message
}
