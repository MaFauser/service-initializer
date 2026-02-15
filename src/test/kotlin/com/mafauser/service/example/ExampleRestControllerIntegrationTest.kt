package com.mafauser.service.example

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.mafauser.service.TestcontainersConfiguration
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.json.JsonCompareMode
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.util.UUID

@Import(TestcontainersConfiguration::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@DisplayName("Example REST API (integration)")
class ExampleRestControllerIntegrationTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    private val objectMapper = jacksonObjectMapper()

    @Test
    fun `GET examples returns 200 and list`() {
        mockMvc
            .perform(get("/examples"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
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
        val id = created.id!!

        mockMvc
            .perform(get("/examples/$id"))
            .andExpect(status().isOk)
            .andExpect(content().json("""{"id":"$id","name":"For Get By Id"}""", JsonCompareMode.LENIENT))
    }

    @Test
    fun `GET examples by id returns 404 when not found`() {
        val id = UUID.randomUUID()
        mockMvc
            .perform(get("/examples/$id"))
            .andExpect(status().isNotFound)
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
        val id = created.id!!

        val updateBody = """{"name":"Updated REST","description":"Updated desc"}"""
        mockMvc
            .perform(
                put("/examples/$id")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(updateBody),
            ).andExpect(status().isOk)
            .andExpect(
                content().json(
                    """{"name":"Updated REST","description":"Updated desc"}""",
                    JsonCompareMode.LENIENT,
                ),
            )

        mockMvc
            .perform(get("/examples/$id"))
            .andExpect(status().isOk)
            .andExpect(
                content().json("""{"name":"Updated REST"}""", JsonCompareMode.LENIENT),
            )
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
        val id = created.id!!

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
    }

    private data class ExampleResponse(
        val id: String? = null,
        val name: String? = null,
        val description: String? = null,
        val version: Long? = null,
        val createdAt: String? = null,
        val updatedAt: String? = null,
    )
}
