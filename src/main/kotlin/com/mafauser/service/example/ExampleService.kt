package com.mafauser.service.example

import com.mafauser.service.config.ConflictException
import com.mafauser.service.config.NotFoundException
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

/**
 * Application service for the Example domain. Holds business logic and coordinates
 * [ExampleRepository]. Use constructor injection.
 */
@Service
class ExampleService(
    private val exampleRepository: ExampleRepository,
) {
    @Transactional(readOnly = true)
    fun findAll(): List<Example> = exampleRepository.findAll()

    @Transactional(readOnly = true)
    fun findById(id: UUID): Example? = exampleRepository.findById(id).orElse(null)

    @Transactional
    fun create(input: CreateExampleInput): Example {
        if (exampleRepository.existsByName(input.name)) {
            throw ConflictException("Example", "name", input.name)
        }
        val example =
            Example(
                name = input.name,
                description = input.description,
            )
        return exampleRepository.save(example)
    }

    @Transactional
    fun update(
        id: UUID,
        input: UpdateExampleInput,
    ): Example {
        val example = exampleRepository.findById(id).orElseThrow { NotFoundException("Example", id) }
        input.name?.let { name ->
            if (name != example.name && exampleRepository.existsByName(name)) {
                throw ConflictException("Example", "name", name)
            }
            example.name = name
        }
        input.description?.let { example.description = it }
        return exampleRepository.save(example)
    }

    @Transactional
    fun delete(id: UUID): Boolean =
        if (exampleRepository.existsById(id)) {
            exampleRepository.deleteById(id)
            true
        } else {
            false
        }
}

/** Input for creating a new [Example]. */
data class CreateExampleInput(
    @field:NotBlank @field:Size(max = 255) val name: String,
    @field:Size(max = 1024) val description: String? = null,
)

/** Input for updating an existing [Example]. Only non-null fields are applied. */
data class UpdateExampleInput(
    @field:Size(max = 255) val name: String? = null,
    @field:Size(max = 1024) val description: String? = null,
)
