package com.mafauser.service.example

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

/**
 * REST controller for the Example domain. Exposes CRUD over HTTP.
 */
@RestController
@RequestMapping("/examples")
class ExampleController(
    private val exampleService: ExampleService,
) {
    @GetMapping
    fun list(): List<Example> = exampleService.findAll()

    @GetMapping("/{id}")
    fun get(
        @PathVariable id: UUID,
    ): Example = exampleService.findById(id) ?: throw ExampleNotFoundException(id)

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(
        @RequestBody input: CreateExampleInput,
    ): Example = exampleService.create(input)

    @PutMapping("/{id}")
    fun update(
        @PathVariable id: UUID,
        @RequestBody input: UpdateExampleInput,
    ): Example = exampleService.update(id, input)

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(
        @PathVariable id: UUID,
    ) {
        if (!exampleService.delete(id)) {
            throw ExampleNotFoundException(id)
        }
    }
}
