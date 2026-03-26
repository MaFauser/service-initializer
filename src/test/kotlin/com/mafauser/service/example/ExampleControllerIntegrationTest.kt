package com.mafauser.service.example

import com.mafauser.service.BaseIntegrationTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.graphql.test.tester.GraphQlTester
import org.springframework.graphql.test.tester.HttpGraphQlTester
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.servlet.client.MockMvcWebTestClient
import org.springframework.web.context.WebApplicationContext

@DisplayName("Example GraphQL API (integration)")
class ExampleControllerIntegrationTest : BaseIntegrationTest() {
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
    fun `examples query returns a list`() {
        graphQlTester
            .document("""mutation { createExample(input: { name: "For List" }) { id } }""")
            .execute()
            .path("createExample.id")
            .entity(String::class.java)
            .get()

        val list =
            graphQlTester
                .document("""{ examples(page: 0, size: 10) { id name } }""")
                .execute()
                .path("examples")
                .entityList(ExampleProjection::class.java)
                .get()
        assertTrue(list.isNotEmpty())
    }

    @Test
    fun `createExample mutation creates and returns example`() {
        val document =
            """
            mutation {
                createExample(input: { name: "Integration One", description: "From test" }) {
                    id
                    name
                    description
                    version
                    createdAt
                    updatedAt
                }
            }
            """.trimIndent()

        val created =
            graphQlTester
                .document(document)
                .execute()
                .path("createExample")
                .entity(ExampleProjection::class.java)
                .get()

        assertNotNull(created.id)
        assertEquals("Integration One", created.name)
        assertEquals("From test", created.description)
        assertEquals(0L, created.version)
        assertNotNull(created.createdAt)
        assertNotNull(created.updatedAt)
    }

    @Test
    fun `examples and example query return created data`() {
        val createDoc =
            """
            mutation {
                createExample(input: { name: "For List Test" }) {
                    id
                    name
                }
            }
            """.trimIndent()

        val created =
            graphQlTester
                .document(createDoc)
                .execute()
                .path("createExample")
                .entity(ExampleProjection::class.java)
                .get()

        val id = created.id!!

        graphQlTester
            .documentName("example-by-id-query")
            .variable("id", id)
            .execute()
            .path("example")
            .entity(ExampleProjection::class.java)
            .satisfies { ex: ExampleProjection ->
                assertEquals(id, ex.id)
                assertEquals("For List Test", ex.name)
            }

        val list: List<ExampleProjection> =
            graphQlTester
                .documentName("examples-query")
                .execute()
                .path("examples")
                .entityList(ExampleProjection::class.java)
                .get()
        assertTrue(list.any { ex -> ex.id == id && ex.name == "For List Test" })
    }

    @Test
    fun `updateExample mutation updates and returns example`() {
        val createDoc =
            """
            mutation {
                createExample(input: { name: "To Update", description: "Original" }) {
                    id
                }
            }
            """.trimIndent()

        val id =
            graphQlTester
                .document(createDoc)
                .execute()
                .path("createExample.id")
                .entity(String::class.java)
                .get()

        val updateDoc =
            """
            mutation {
                updateExample(id: "%s", input: { name: "Updated Name", description: "Updated Desc" }) {
                    id
                    name
                    description
                }
            }
            """.trimIndent()
                .format(id)

        val updated =
            graphQlTester
                .document(updateDoc)
                .execute()
                .path("updateExample")
                .entity(ExampleProjection::class.java)
                .get()
        assertEquals(id, updated.id)
        assertEquals("Updated Name", updated.name)
        assertEquals("Updated Desc", updated.description)
    }

    @Test
    fun `deleteExample mutation returns true and removes example`() {
        val createDoc =
            """
            mutation {
                createExample(input: { name: "To Delete" }) {
                    id
                }
            }
            """.trimIndent()

        val id =
            graphQlTester
                .document(createDoc)
                .execute()
                .path("createExample.id")
                .entity(String::class.java)
                .get()

        val deleted: Boolean =
            graphQlTester
                .documentName("delete-example-mutation")
                .variable("id", id)
                .execute()
                .path("deleteExample")
                .entity(Boolean::class.java)
                .get()

        assertTrue(deleted)

        graphQlTester
            .documentName("example-by-id-query")
            .variable("id", id)
            .execute()
            .errors()
            .satisfy { errors ->
                assertTrue(errors.isNotEmpty())
                assertTrue(errors.any { it.message?.contains("not found", ignoreCase = true) == true })
            }
    }

    @Test
    fun `example query returns NOT_FOUND for unknown id`() {
        graphQlTester
            .documentName("example-by-id-query")
            .variable("id", "00000000-0000-0000-0000-000000000000")
            .execute()
            .errors()
            .satisfy { errors ->
                assertTrue(errors.isNotEmpty())
                assertTrue(errors.any { it.message?.contains("not found", ignoreCase = true) == true })
            }
    }

    @Test
    fun `example query returns error for invalid UUID string`() {
        graphQlTester
            .documentName("example-by-id-query")
            .variable("id", "not-a-valid-uuid")
            .execute()
            .errors()
            .satisfy { errors ->
                assertTrue(errors.isNotEmpty())
                assertTrue(errors.any { it.message?.contains("UUID", ignoreCase = true) == true })
            }
    }

    @Test
    fun `updateExample with invalid UUID returns error`() {
        graphQlTester
            .document(
                """
                mutation {
                    updateExample(id: "invalid-uuid", input: { name: "Any" }) {
                        id
                    }
                }
                """.trimIndent(),
            ).execute()
            .errors()
            .satisfy { errors ->
                assertTrue(errors.isNotEmpty()) { "Expected error for invalid UUID, got: $errors" }
                val hasRelevantError =
                    errors.any {
                        val msg = it.message ?: ""
                        msg.contains("UUID", ignoreCase = true) ||
                            msg.contains("invalid", ignoreCase = true) ||
                            msg.contains("BAD_REQUEST", ignoreCase = true)
                    }
                assertTrue(hasRelevantError) { "Expected error for invalid UUID, got: ${errors.map { it.message }}" }
            }
    }

    @Test
    fun `deleteExample with invalid UUID returns error`() {
        graphQlTester
            .documentName("delete-example-mutation")
            .variable("id", "bad-uuid")
            .execute()
            .errors()
            .satisfy { errors ->
                assertTrue(errors.isNotEmpty()) { "Expected error for invalid UUID, got: $errors" }
                val hasError =
                    errors.any {
                        val msg = it.message ?: ""
                        msg.contains("BAD_REQUEST", ignoreCase = true) ||
                            msg.contains("UUID", ignoreCase = true)
                    }
                assertTrue(hasError) { "Expected BAD_REQUEST or UUID error, got: ${errors.map { it.message }}" }
            }
    }

    @Test
    fun `updateExample with non-existent UUID returns NOT_FOUND error`() {
        graphQlTester
            .document(
                """
                mutation {
                    updateExample(id: "00000000-0000-0000-0000-000000000000", input: { name: "Ghost" }) {
                        id
                    }
                }
                """.trimIndent(),
            ).execute()
            .errors()
            .satisfy { errors ->
                assertTrue(errors.isNotEmpty())
                assertTrue(errors.any { it.message?.contains("not found", ignoreCase = true) == true })
            }
    }

    @Test
    fun `createExample with duplicate name returns error`() {
        val createDoc =
            """
            mutation {
                createExample(input: { name: "Duplicate Name" }) {
                    id
                }
            }
            """.trimIndent()

        graphQlTester
            .document(createDoc)
            .execute()
            .path("createExample.id")
            .entity(String::class.java)
            .get()

        graphQlTester
            .document(createDoc)
            .execute()
            .errors()
            .satisfy { errors ->
                assertTrue(errors.isNotEmpty()) { "Expected at least one error for duplicate name, got: $errors" }
                val hasRelevantError =
                    errors.any {
                        val msg = it.message ?: ""
                        msg.contains("already exists", ignoreCase = true) ||
                            msg.contains("CONFLICT", ignoreCase = true)
                    }
                assertTrue(hasRelevantError) { "Expected error for duplicate name, got: ${errors.map { it.message }}" }
            }
    }

    data class ExampleProjection(
        val id: String? = null,
        val name: String? = null,
        val description: String? = null,
        val version: Long? = null,
        val createdAt: String? = null,
        val updatedAt: String? = null,
    )
}
