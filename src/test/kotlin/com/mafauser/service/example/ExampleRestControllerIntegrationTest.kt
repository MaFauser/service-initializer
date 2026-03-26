package com.mafauser.service.example

import com.mafauser.service.BaseIntegrationTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import tools.jackson.module.kotlin.jacksonObjectMapper
import tools.jackson.module.kotlin.readValue
import java.util.UUID

@DisplayName("Example REST API (integration)")
class ExampleRestControllerIntegrationTest : BaseIntegrationTest() {
    @Autowired
    private lateinit var context: WebApplicationContext

    private lateinit var mockMvc: MockMvc

    @BeforeEach
    fun setUp() {
        mockMvc =
            MockMvcBuilders
                .webAppContextSetup(context)
                .apply<DefaultMockMvcBuilder>(SecurityMockMvcConfigurers.springSecurity())
                .build()
    }

    private val objectMapper = jacksonObjectMapper()

    @Test
    fun `GET examples returns 200 and paginated list`() {
        mockMvc
            .perform(
                post("/examples")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"name":"For List"}"""),
            ).andExpect(status().isCreated)

        mockMvc
            .perform(get("/examples"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.content").isArray)
            .andExpect(jsonPath("$.content").isNotEmpty)
            .andExpect(jsonPath("$.page.totalElements").value(1))
    }

    @Test
    fun `POST examples creates and returns 201`() {
        val body = """{"name":"REST One","description":"From REST test"}"""
        val result =
            mockMvc
                .perform(
                    post("/examples")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body),
                ).andExpect(status().isCreated)
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn()
        val created: ExampleResponse = objectMapper.readValue<ExampleResponse>(result.response.contentAsString)
        assertNotNull(created.id)
        assertEquals("REST One", created.name)
        assertEquals("From REST test", created.description)
    }

    @Test
    fun `GET examples by id returns 200 when found`() {
        val createBody = """{"name":"For Get By Id"}"""
        val createResult =
            mockMvc
                .perform(
                    post("/examples")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody),
                ).andExpect(status().isCreated)
                .andReturn()
        val created: ExampleResponse = objectMapper.readValue<ExampleResponse>(createResult.response.contentAsString)
        val id = created.id

        mockMvc
            .perform(get("/examples/$id"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(id.toString()))
            .andExpect(jsonPath("$.name").value("For Get By Id"))
    }

    @Test
    fun `GET examples by id returns 404 when not found`() {
        val id = UUID.randomUUID()
        mockMvc
            .perform(get("/examples/$id"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.title").value("Not Found"))
    }

    @Test
    fun `PUT examples by id updates and returns 200`() {
        val createBody = """{"name":"To Update REST"}"""
        val createResult =
            mockMvc
                .perform(
                    post("/examples")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody),
                ).andExpect(status().isCreated)
                .andReturn()
        val created: ExampleResponse = objectMapper.readValue<ExampleResponse>(createResult.response.contentAsString)
        val id = created.id

        val updateBody = """{"name":"Updated REST","description":"Updated desc"}"""
        mockMvc
            .perform(
                put("/examples/$id")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(updateBody),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.name").value("Updated REST"))
            .andExpect(jsonPath("$.description").value("Updated desc"))

        mockMvc
            .perform(get("/examples/$id"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.name").value("Updated REST"))
    }

    @Test
    fun `PUT examples by id returns 404 when not found`() {
        val id = UUID.randomUUID()
        val updateBody = """{"name":"Any"}"""
        mockMvc
            .perform(
                put("/examples/$id")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(updateBody),
            ).andExpect(status().isNotFound)
    }

    @Test
    fun `DELETE examples by id returns 204 when exists`() {
        val createBody = """{"name":"To Delete REST"}"""
        val createResult =
            mockMvc
                .perform(
                    post("/examples")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody),
                ).andExpect(status().isCreated)
                .andReturn()
        val created: ExampleResponse = objectMapper.readValue<ExampleResponse>(createResult.response.contentAsString)
        val id = created.id

        mockMvc
            .perform(delete("/examples/$id"))
            .andExpect(status().isNoContent)

        mockMvc
            .perform(get("/examples/$id"))
            .andExpect(status().isNotFound)
    }

    @Test
    fun `DELETE examples by id returns 404 when not found`() {
        val id = UUID.randomUUID()
        mockMvc
            .perform(delete("/examples/$id"))
            .andExpect(status().isNotFound)
    }

    @Test
    fun `POST examples with blank name returns 400`() {
        mockMvc
            .perform(
                post("/examples")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"name":"","description":"Test"}"""),
            ).andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.title").value("Bad Request"))
    }

    @Test
    fun `POST examples with missing name returns 400`() {
        mockMvc
            .perform(
                post("/examples")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{}"""),
            ).andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.title").value("Bad Request"))
    }

    @Test
    fun `GET examples with invalid UUID returns 400`() {
        mockMvc
            .perform(get("/examples/not-a-uuid"))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.title").value("Bad Request"))
    }

    @Test
    fun `PUT examples with invalid UUID returns 400`() {
        mockMvc
            .perform(
                put("/examples/not-a-uuid")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"name":"Any"}"""),
            ).andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.title").value("Bad Request"))
    }

    @Test
    fun `DELETE examples with invalid UUID returns 400`() {
        mockMvc
            .perform(delete("/examples/not-a-uuid"))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.title").value("Bad Request"))
    }

    @Test
    fun `POST examples with duplicate name returns 409`() {
        val body = """{"name":"Duplicate REST Name"}"""
        mockMvc
            .perform(
                post("/examples")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body),
            ).andExpect(status().isCreated)

        mockMvc
            .perform(
                post("/examples")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body),
            ).andExpect(status().isConflict)
            .andExpect(jsonPath("$.title").value("Conflict"))
    }

    @Test
    fun `PUT with empty description clears the description`() {
        val createBody = """{"name":"Clear Desc Test","description":"Has description"}"""
        val createResult =
            mockMvc
                .perform(
                    post("/examples")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody),
                ).andExpect(status().isCreated)
                .andReturn()
        val created: ExampleResponse = objectMapper.readValue<ExampleResponse>(createResult.response.contentAsString)
        val id = created.id

        mockMvc
            .perform(
                put("/examples/$id")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"description":""}"""),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.description").doesNotExist())
    }

    private data class ExampleResponse(
        val id: UUID? = null,
        val name: String? = null,
        val description: String? = null,
        val version: Long? = null,
        val createdAt: String? = null,
        val updatedAt: String? = null,
    )
}
