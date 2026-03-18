package com.mafauser.service.example

import com.mafauser.service.exception.NotFoundException
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.validation.Valid
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/examples")
class ExampleController(
    private val exampleService: ExampleService,
) {
    private val log = KotlinLogging.logger {}

    @GetMapping
    fun list(
        @PageableDefault(size = 20) pageable: Pageable,
    ): Page<ExampleResponse> {
        log.debug { "Examples list requested, page=$pageable" }
        return exampleService.findAll(pageable).map { it.toResponse() }
    }

    @GetMapping("/{id}")
    fun get(
        @PathVariable id: UUID,
    ): ExampleResponse {
        log.debug { "Example get requested: id=$id" }
        return (exampleService.findById(id) ?: throw NotFoundException("Example", id)).toResponse()
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(
        @RequestBody @Valid input: CreateExampleInput,
    ): ExampleResponse {
        log.debug { "Example create requested: name=${input.name}" }
        return exampleService.create(input).toResponse()
    }

    @PutMapping("/{id}")
    fun update(
        @PathVariable id: UUID,
        @RequestBody @Valid input: UpdateExampleInput,
    ): ExampleResponse {
        log.debug { "Example update requested: id=$id" }
        return exampleService.update(id, input).toResponse()
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(
        @PathVariable id: UUID,
    ) {
        log.debug { "Example delete requested: id=$id" }
        if (!exampleService.delete(id)) {
            throw NotFoundException("Example", id)
        }
    }
}
