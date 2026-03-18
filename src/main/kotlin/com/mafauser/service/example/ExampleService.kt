package com.mafauser.service.example

import com.mafauser.service.exception.ConflictException
import com.mafauser.service.exception.NotFoundException
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class ExampleService(
    private val exampleRepository: ExampleRepository,
) {
    @Transactional(readOnly = true)
    fun findAll(pageable: Pageable): Page<Example> = exampleRepository.findAll(pageable)

    @Transactional(readOnly = true)
    fun findById(id: UUID): Example = exampleRepository.findById(id).orElseThrow { NotFoundException("Example", id) }

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
        input.description?.let { example.description = it.ifBlank { null } }
        return exampleRepository.save(example)
    }

    @Transactional
    fun delete(id: UUID) {
        if (!exampleRepository.existsById(id)) {
            throw NotFoundException("Example", id)
        }
        exampleRepository.deleteById(id)
    }
}
