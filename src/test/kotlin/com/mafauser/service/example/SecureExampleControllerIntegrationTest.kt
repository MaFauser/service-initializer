package com.mafauser.service.example

import com.mafauser.service.BaseIntegrationTest
import com.mafauser.service.security.Roles
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import tools.jackson.module.kotlin.jacksonObjectMapper
import tools.jackson.module.kotlin.readValue
import java.util.UUID

@DisplayName("Secure Example REST API (integration)")
class SecureExampleControllerIntegrationTest : BaseIntegrationTest() {
    @Autowired
    private lateinit var context: WebApplicationContext

    private lateinit var mockMvc: MockMvc

    private val objectMapper = jacksonObjectMapper()

    @BeforeEach
    fun setUp() {
        mockMvc =
            MockMvcBuilders
                .webAppContextSetup(context)
                .apply<DefaultMockMvcBuilder>(SecurityMockMvcConfigurers.springSecurity())
                .build()
    }

    @Test
    fun `unauthenticated request returns 403`() {
        mockMvc
            .perform(get("/secure/examples"))
            .andExpect(status().isForbidden)
    }

    @Test
    @WithMockUser(roles = [Roles.USER])
    fun `authenticated USER can list examples`() {
        mockMvc
            .perform(get("/secure/examples"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.content").isArray)
    }

    @Test
    @WithMockUser(roles = [Roles.USER])
    fun `authenticated USER can get example by id`() {
        val id = createExample()

        mockMvc
            .perform(get("/secure/examples/$id"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(id.toString()))
    }

    @Test
    @WithMockUser(roles = [Roles.USER])
    fun `USER cannot delete — returns 403`() {
        val id = createExample()

        mockMvc
            .perform(delete("/secure/examples/$id"))
            .andExpect(status().isForbidden)
    }

    @Test
    @WithMockUser(roles = [Roles.ADMIN])
    fun `ADMIN can delete — returns 204`() {
        val id = createExample()

        mockMvc
            .perform(delete("/secure/examples/$id"))
            .andExpect(status().isNoContent)
    }

    private fun createExample(): UUID {
        val result =
            mockMvc
                .perform(
                    post("/examples")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""{"name":"Secure-${UUID.randomUUID()}"}"""),
                ).andExpect(status().isCreated)
                .andReturn()
        return objectMapper.readValue<IdHolder>(result.response.contentAsString).id!!
    }

    private data class IdHolder(
        val id: UUID? = null,
    )
}
