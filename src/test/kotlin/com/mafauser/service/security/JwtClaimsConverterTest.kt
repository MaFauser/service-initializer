package com.mafauser.service.security

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.jwt.Jwt
import java.time.Instant

@DisplayName("JwtClaimsConverter")
class JwtClaimsConverterTest {
    private val converter = JwtClaimsConverter()

    private fun jwt(vararg claims: Pair<String, Any>): Jwt =
        Jwt
            .withTokenValue("token")
            .header("alg", "RS256")
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(3600))
            .apply { claims.forEach { (k, v) -> claim(k, v) } }
            .build()

    @Nested
    @DisplayName("roles extraction")
    inner class Roles {
        @Test
        fun `extracts top-level roles claim`() {
            val authorities = converter.convert(jwt("roles" to listOf("admin", "user")))

            assertTrue(authorities.contains(SimpleGrantedAuthority("ROLE_ADMIN")))
            assertTrue(authorities.contains(SimpleGrantedAuthority("ROLE_USER")))
        }

        @Test
        fun `extracts Keycloak realm_access roles`() {
            val authorities = converter.convert(jwt("realm_access" to mapOf("roles" to listOf("manager"))))

            assertTrue(authorities.contains(SimpleGrantedAuthority("ROLE_MANAGER")))
        }

        @Test
        fun `extracts Keycloak resource_access roles`() {
            val resourceAccess = mapOf("my-client" to mapOf("roles" to listOf("editor")))
            val authorities = converter.convert(jwt("resource_access" to resourceAccess))

            assertTrue(authorities.contains(SimpleGrantedAuthority("ROLE_EDITOR")))
        }

        @Test
        fun `uppercases role names`() {
            val authorities = converter.convert(jwt("roles" to listOf("mixed_Case")))

            assertTrue(authorities.contains(SimpleGrantedAuthority("ROLE_MIXED_CASE")))
        }

        @Test
        fun `deduplicates roles from multiple sources`() {
            val jwt =
                jwt(
                    "roles" to listOf("admin"),
                    "realm_access" to mapOf("roles" to listOf("admin")),
                )
            val authorities = converter.convert(jwt)

            assertEquals(1, authorities.count { it.authority == "ROLE_ADMIN" })
        }
    }

    @Nested
    @DisplayName("permissions extraction")
    inner class Permissions {
        @Test
        fun `extracts Auth0 permissions claim`() {
            val authorities = converter.convert(jwt("permissions" to listOf("example:read", "example:write")))

            assertTrue(authorities.contains(SimpleGrantedAuthority("example:read")))
            assertTrue(authorities.contains(SimpleGrantedAuthority("example:write")))
        }

        @Test
        fun `extracts scope claim as SCOPE_ prefixed authorities`() {
            val authorities = converter.convert(jwt("scope" to "openid profile email"))

            assertTrue(authorities.contains(SimpleGrantedAuthority("SCOPE_openid")))
            assertTrue(authorities.contains(SimpleGrantedAuthority("SCOPE_profile")))
            assertTrue(authorities.contains(SimpleGrantedAuthority("SCOPE_email")))
        }

        @Test
        fun `ignores blank scope segments`() {
            val authorities = converter.convert(jwt("scope" to "read  write"))

            assertEquals(2, authorities.filter { it.authority?.startsWith("SCOPE_") == true }.size)
        }
    }

    @Nested
    @DisplayName("edge cases")
    inner class EdgeCases {
        @Test
        fun `returns empty authorities when no claims present`() {
            val authorities = converter.convert(jwt())

            assertTrue(authorities.isEmpty())
        }

        @Test
        fun `handles realm_access without roles key`() {
            val authorities = converter.convert(jwt("realm_access" to mapOf("other" to "value")))

            assertTrue(authorities.isEmpty())
        }

        @Test
        fun `handles resource_access with non-map value`() {
            val authorities = converter.convert(jwt("resource_access" to mapOf("client" to "not-a-map")))

            assertTrue(authorities.isEmpty())
        }
    }

    @Test
    fun `authenticationConverter returns configured JwtAuthenticationConverter`() {
        val authConverter = JwtClaimsConverter.authenticationConverter()

        val jwt = jwt("roles" to listOf("admin"))
        val authentication = authConverter.convert(jwt)

        assertTrue(authentication!!.authorities.contains(SimpleGrantedAuthority("ROLE_ADMIN")))
    }
}
