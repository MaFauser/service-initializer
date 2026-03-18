package com.mafauser.service

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.JwtDecoder
import java.time.Instant

@TestConfiguration(proxyBeanMethods = false)
class TestSecurityConfiguration {
    @Bean
    fun jwtDecoder(): JwtDecoder =
        JwtDecoder {
            Jwt
                .withTokenValue("mock-token")
                .header("alg", "none")
                .claim("sub", "test-user")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build()
        }
}
