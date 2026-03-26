package com.mafauser.service.example

import com.mafauser.service.security.IsAuthenticated
import com.mafauser.service.security.Roles
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpStatus
import org.springframework.security.access.annotation.Secured
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/secure/examples")
@IsAuthenticated
class SecureExampleController(
    private val exampleService: ExampleService,
) {
    private val log = KotlinLogging.logger {}

    @GetMapping
    fun list(
        @PageableDefault(size = 20) pageable: Pageable,
    ): Page<ExampleResponse> {
        log.debug { "Secure examples list requested" }
        return exampleService.findAll(pageable).map { it.toResponse() }
    }

    @GetMapping("/{id}")
    @Secured(Roles.ROLE_USER, Roles.ROLE_ADMIN)
    fun get(
        @PathVariable id: UUID,
    ): ExampleResponse {
        log.debug { "Secure example get: id=$id" }
        return exampleService.findById(id).toResponse()
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Secured(Roles.ROLE_ADMIN)
    fun delete(
        @PathVariable id: UUID,
    ) {
        log.debug { "Secure example delete: id=$id" }
        exampleService.delete(id)
    }
}
