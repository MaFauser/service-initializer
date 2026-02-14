package com.mafauser.service.example

import com.mafauser.service.TestcontainersConfiguration
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.graphql.test.tester.GraphQlTester
import org.springframework.graphql.test.tester.HttpGraphQlTester
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.servlet.client.MockMvcWebTestClient
import org.springframework.web.context.WebApplicationContext

@Import(TestcontainersConfiguration::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
@DisplayName("Example GraphQL API (integration)")
class ExampleControllerIntegrationTest {
    @Autowired
    private lateinit var applicationContext: WebApplicationContext

    private lateinit var graphQlTester: GraphQlTester

    @BeforeEach
    fun setUp() {
        val client: WebTestClient =
            MockMvcWebTestClient
                .bindToApplicationContext(applicationContext)
                .configureClient()
                .baseUrl("/graphql")
                .build()
        graphQlTester = HttpGraphQlTester.create(client)
    }

    @Test
    @Order(1)
    fun `examples query returns a list`() {
        val list =
            graphQlTester
                .documentName("examples-query")
                .execute()
                .path("examples")
                .entityList(ExampleProjection::class.java)
                .get()
        assertNotNull(list)
        // List may be empty or contain data from other tests; we only verify the query works
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
            .path("example")
            .valueIsNull()
    }

    @Test
    fun `example query returns null for unknown id`() {
        graphQlTester
            .documentName("example-by-id-query")
            .variable("id", "00000000-0000-0000-0000-000000000000")
            .execute()
            .path("example")
            .valueIsNull()
    }

    @Test
    fun `example query returns null for invalid UUID string`() {
        graphQlTester
            .documentName("example-by-id-query")
            .variable("id", "not-a-valid-uuid")
            .execute()
            .path("example")
            .valueIsNull()
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
                        val ext = it.extensions?.toString() ?: ""
                        msg.contains("UUID", ignoreCase = true) ||
                            msg.contains("invalid", ignoreCase = true) ||
                            msg.contains("INTERNAL_ERROR") ||
                            ext.contains("IllegalArgumentException")
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
                // GraphQL may return INTERNAL_ERROR when parseUuid throws
                val hasError =
                    errors.any {
                        val msg = it.message ?: ""
                        msg.contains("INTERNAL_ERROR") || msg.contains("UUID", ignoreCase = true)
                    }
                assertTrue(hasError) { "Expected INTERNAL_ERROR or UUID error, got: ${errors.map { it.message }}" }
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
                // Server may return INTERNAL_ERROR or a message containing duplicate/conflict
                val hasRelevantError =
                    errors.any {
                        val msg = it.message ?: ""
                        msg.contains("DuplicateExampleNameException") ||
                            msg.contains("already exists") ||
                            msg.contains("duplicate", ignoreCase = true) ||
                            msg.contains("INTERNAL_ERROR")
                    }
                assertTrue(hasRelevantError) { "Expected error for duplicate name, got: ${errors.map { it.message }}" }
            }
    }

    /**
     * Minimal projection for GraphQL response (id, name, description, version, createdAt,
     * updatedAt).
     */
    data class ExampleProjection(
        val id: String? = null,
        val name: String? = null,
        val description: String? = null,
        val version: Long? = null,
        val createdAt: String? = null,
        val updatedAt: String? = null,
    )
}
