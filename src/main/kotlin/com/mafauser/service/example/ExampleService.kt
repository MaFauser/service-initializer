package com.mafauser.service.example

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.Optional
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
    fun findById(id: UUID): Example? {
        val opt: Optional<Example> = exampleRepository.findById(id)
        return opt.orElse(null)
    }

    @Transactional
    fun create(input: CreateExampleInput): Example {
        if (exampleRepository.existsByName(input.name)) {
            throw DuplicateExampleNameException(input.name)
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
        val example = exampleRepository.findById(id).orElseThrow { ExampleNotFoundException(id) }
        input.name?.let { name ->
            if (name != example.name && exampleRepository.existsByName(name)) {
                throw DuplicateExampleNameException(name)
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
    val name: String,
    val description: String? = null,
)

/** Input for updating an existing [Example]. Only non-null fields are applied. */
data class UpdateExampleInput(
    val name: String? = null,
    val description: String? = null,
)

class ExampleNotFoundException(
    id: UUID,
) : RuntimeException("Example not found: $id")

class DuplicateExampleNameException(
    name: String,
) : RuntimeException("Example already exists with name: $name")
