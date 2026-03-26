package com.mafauser.service.example

import com.mafauser.service.BaseIntegrationTest
import com.mafauser.service.security.Roles
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.graphql.test.tester.GraphQlTester
import org.springframework.graphql.test.tester.HttpGraphQlTester
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.servlet.client.MockMvcWebTestClient
import org.springframework.web.context.WebApplicationContext

@DisplayName("Secure Example GraphQL API (integration)")
class SecureExampleGraphQLControllerIntegrationTest : BaseIntegrationTest() {
    @Autowired
    private lateinit var applicationContext: WebApplicationContext

    private lateinit var graphQlTester: GraphQlTester

    @BeforeEach
    fun setUp() {
        val client: WebTestClient =
            MockMvcWebTestClient
                .bindToApplicationContext(applicationContext)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .configureClient()
                .baseUrl("/graphql")
                .build()
        graphQlTester = HttpGraphQlTester.create(client)
    }

    @Test
    fun `unauthenticated secureExamples query returns error`() {
        graphQlTester
            .document("{ secureExamples { id } }")
            .execute()
            .errors()
            .satisfy { errors ->
                assertTrue(errors.isNotEmpty())
            }
    }

    @Test
    @WithMockUser(roles = [Roles.USER])
    fun `authenticated USER can query secureExamples`() {
        graphQlTester
            .document("{ secureExamples { id name } }")
            .execute()
            .path("secureExamples")
            .entityList(Any::class.java)
            .get()
    }

    @Test
    @WithMockUser(roles = [Roles.USER])
    fun `authenticated USER can query secureExample by id`() {
        val id = createExample()

        graphQlTester
            .document("""{ secureExample(id: "$id") { id name } }""")
            .execute()
            .path("secureExample.id")
            .entity(String::class.java)
            .isEqualTo(id)
    }

    @Test
    @WithMockUser(roles = [Roles.USER])
    fun `USER cannot secureDeleteExample — returns error`() {
        val id = createExample()

        graphQlTester
            .document("""mutation { secureDeleteExample(id: "$id") }""")
            .execute()
            .errors()
            .satisfy { errors ->
                assertTrue(errors.isNotEmpty())
                assertTrue(errors.any { it.message?.contains("Access denied", ignoreCase = true) == true })
            }
    }

    @Test
    @WithMockUser(roles = [Roles.ADMIN])
    fun `ADMIN can secureDeleteExample`() {
        val id = createExample()

        graphQlTester
            .document("""mutation { secureDeleteExample(id: "$id") }""")
            .execute()
            .path("secureDeleteExample")
            .entity(Boolean::class.java)
            .isEqualTo(true)
    }

    @WithMockUser(roles = [Roles.USER])
    private fun createExample(): String =
        graphQlTester
            .document("""mutation { createExample(input: { name: "Secure-${System.nanoTime()}" }) { id } }""")
            .execute()
            .path("createExample.id")
            .entity(String::class.java)
            .get()
}
