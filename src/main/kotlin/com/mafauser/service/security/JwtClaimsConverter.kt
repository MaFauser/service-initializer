package com.mafauser.service.security

import org.springframework.core.convert.converter.Converter
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter

class JwtClaimsConverter : Converter<Jwt, Collection<GrantedAuthority>> {
    override fun convert(jwt: Jwt): Collection<GrantedAuthority> {
        val authorities = mutableSetOf<GrantedAuthority>()
        authorities += extractRoles(jwt).map { SimpleGrantedAuthority("${Roles.ROLE_PREFIX}${it.uppercase()}") }
        authorities += extractPermissions(jwt).map { SimpleGrantedAuthority(it) }
        return authorities
    }

    private fun extractRoles(jwt: Jwt): List<String> {
        val roles = mutableListOf<String>()

        // Generic: top-level "roles" claim
        jwt.getClaimAsStringList("roles")?.let { roles += it }

        // Keycloak: realm_access.roles
        jwt.getClaimAsMap("realm_access")?.let { realmAccess ->
            @Suppress("UNCHECKED_CAST")
            (realmAccess["roles"] as? List<String>)?.let { roles += it }
        }

        // Keycloak: resource_access.<client>.roles
        jwt.getClaimAsMap("resource_access")?.values?.forEach { resource ->
            @Suppress("UNCHECKED_CAST")
            ((resource as? Map<String, Any>)?.get("roles") as? List<String>)?.let { roles += it }
        }

        return roles
    }

    private fun extractPermissions(jwt: Jwt): List<String> {
        val permissions = mutableListOf<String>()

        // Auth0 / generic: "permissions" claim
        jwt.getClaimAsStringList("permissions")?.let { permissions += it }

        // OAuth2 "scope" claim (space-delimited)
        jwt
            .getClaimAsString("scope")
            ?.split(" ")
            ?.filter { it.isNotBlank() }
            ?.let { permissions += it.map { s -> "SCOPE_$s" } }

        return permissions
    }

    companion object {
        fun authenticationConverter(): JwtAuthenticationConverter =
            JwtAuthenticationConverter().apply {
                setJwtGrantedAuthoritiesConverter(JwtClaimsConverter())
            }
    }
}
