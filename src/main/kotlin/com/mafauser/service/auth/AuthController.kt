package com.mafauser.service.auth

import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/auth")
class AuthController(
    private val keycloakAuthService: KeycloakAuthService,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @PostMapping("/login")
    fun login(
        @RequestBody @Valid request: LoginRequest,
    ): TokenResponse {
        log.debug("Login requested for user={}", request.username)
        return keycloakAuthService.authenticate(request.username, request.password)
    }

    @PostMapping("/userinfo")
    fun userInfo(
        @RequestBody @Valid request: LoginRequest,
    ): UserInfo {
        log.debug("Userinfo requested for user={}", request.username)
        val token = keycloakAuthService.authenticate(request.username, request.password)
        return keycloakAuthService.getUserInfo(token.accessToken)
    }
}
