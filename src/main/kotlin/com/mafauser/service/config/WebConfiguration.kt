package com.mafauser.service.config

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@EnableConfigurationProperties(RateLimitProperties::class)
class WebConfiguration
