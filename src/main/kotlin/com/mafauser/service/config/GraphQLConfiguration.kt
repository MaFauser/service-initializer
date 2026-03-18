package com.mafauser.service.config

import graphql.analysis.MaxQueryComplexityInstrumentation
import graphql.analysis.MaxQueryDepthInstrumentation
import graphql.execution.instrumentation.Instrumentation
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class GraphQLConfiguration {
    @Bean
    fun maxQueryDepthInstrumentation(): Instrumentation = MaxQueryDepthInstrumentation(10)

    @Bean
    fun maxQueryComplexityInstrumentation(): Instrumentation = MaxQueryComplexityInstrumentation(100)
}
