package com.mafauser.service.example

import com.mafauser.service.exception.ConflictException
import com.mafauser.service.exception.NotFoundException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import java.util.Optional.empty
import java.util.Optional.of
import java.util.UUID

@ExtendWith(MockitoExtension::class)
@DisplayName("ExampleService")
class ExampleServiceTest {
    @Mock
    private lateinit var exampleRepository: ExampleRepository

    @InjectMocks
    private lateinit var exampleService: ExampleService

    @Nested
    @DisplayName("findAll")
    inner class FindAll {
        @Test
        fun `returns empty page when repository is empty`() {
            whenever(exampleRepository.findAll(any<Pageable>())).thenReturn(Page.empty())

            val result = exampleService.findAll(Pageable.unpaged())

            assertTrue(result.isEmpty)
            verify(exampleRepository).findAll(any<Pageable>())
        }

        @Test
        fun `returns all examples from repository`() {
            val examples =
                listOf(
                    Example(name = "One"),
                    Example(name = "Two"),
                )
            whenever(exampleRepository.findAll(any<Pageable>())).thenReturn(PageImpl(examples))

            val result = exampleService.findAll(Pageable.unpaged())

            assertEquals(2, result.totalElements)
            assertEquals("One", result.content[0].name)
            assertEquals("Two", result.content[1].name)
            verify(exampleRepository).findAll(any<Pageable>())
        }
    }

    @Nested
    @DisplayName("findById")
    inner class FindById {
        @Test
        fun `returns null when not found`() {
            val id = UUID.randomUUID()
            whenever(exampleRepository.findById(id)).thenReturn(empty())

            val result = exampleService.findById(id)

            assertNull(result)
            verify(exampleRepository).findById(id)
        }

        @Test
        fun `returns example when found`() {
            val id = UUID.randomUUID()
            val example = Example(id = id, name = "Found")
            whenever(exampleRepository.findById(id)).thenReturn(of(example))

            val result = exampleService.findById(id)

            assertEquals(example, result)
            assertEquals("Found", result!!.name)
            verify(exampleRepository).findById(id)
        }
    }

    @Nested
    @DisplayName("create")
    inner class Create {
        @Test
        fun `saves and returns new example`() {
            whenever(exampleRepository.existsByName("New")).thenReturn(false)
            whenever(exampleRepository.save(any<Example>())).thenReturn(Example(name = "New", description = "Desc"))

            val input = CreateExampleInput(name = "New", description = "Desc")
            val result = exampleService.create(input)

            assertEquals("New", result.name)
            assertEquals("Desc", result.description)
            verify(exampleRepository).existsByName("New")
            verify(exampleRepository).save(any<Example>())
        }

        @Test
        fun `throws ConflictException when name exists`() {
            whenever(exampleRepository.existsByName("Existing")).thenReturn(true)

            val input = CreateExampleInput(name = "Existing")
            assertThrows<ConflictException> {
                exampleService.create(input)
            }

            verify(exampleRepository).existsByName("Existing")
            verify(exampleRepository, never()).save(anyOrNull())
        }
    }

    @Nested
    @DisplayName("update")
    inner class Update {
        @Test
        fun `updates and returns example`() {
            val id = UUID.randomUUID()
            val existing = Example(id = id, name = "Old", description = "OldDesc")
            whenever(exampleRepository.findById(id)).thenReturn(of(existing))
            whenever(exampleRepository.existsByName("NewName")).thenReturn(false)
            whenever(exampleRepository.save(any<Example>())).thenReturn(existing)

            val input = UpdateExampleInput(name = "NewName", description = "NewDesc")
            val result = exampleService.update(id, input)

            assertEquals("NewName", result.name)
            assertEquals("NewDesc", result.description)
            verify(exampleRepository).findById(id)
            verify(exampleRepository).existsByName("NewName")
            verify(exampleRepository).save(existing)
        }

        @Test
        fun `throws NotFoundException when not found`() {
            val id = UUID.randomUUID()
            whenever(exampleRepository.findById(id)).thenReturn(empty())

            assertThrows<NotFoundException> {
                exampleService.update(id, UpdateExampleInput(name = "Any"))
            }

            verify(exampleRepository).findById(id)
            verify(exampleRepository, never()).save(anyOrNull())
        }

        @Test
        fun `throws ConflictException when new name already exists`() {
            val id = UUID.randomUUID()
            val existing = Example(id = id, name = "Current")
            whenever(exampleRepository.findById(id)).thenReturn(of(existing))
            whenever(exampleRepository.existsByName("Taken")).thenReturn(true)

            assertThrows<ConflictException> {
                exampleService.update(id, UpdateExampleInput(name = "Taken"))
            }

            verify(exampleRepository).findById(id)
            verify(exampleRepository).existsByName("Taken")
            verify(exampleRepository, never()).save(anyOrNull())
        }

        @Test
        fun `keeps same name without duplicate check`() {
            val id = UUID.randomUUID()
            val existing = Example(id = id, name = "Same")
            whenever(exampleRepository.findById(id)).thenReturn(of(existing))
            whenever(exampleRepository.save(any<Example>())).thenReturn(existing)

            val result = exampleService.update(id, UpdateExampleInput(name = "Same", description = "Updated"))

            assertEquals("Same", result.name)
            assertEquals("Updated", result.description)
            verify(exampleRepository).findById(id)
            verify(exampleRepository, never()).existsByName(any<String>())
            verify(exampleRepository).save(existing)
        }

        @Test
        fun `updates only description when name is null`() {
            val id = UUID.randomUUID()
            val existing = Example(id = id, name = "Keep")
            whenever(exampleRepository.findById(id)).thenReturn(of(existing))
            whenever(exampleRepository.save(any<Example>())).thenReturn(existing)

            val result = exampleService.update(id, UpdateExampleInput(description = "Only desc"))

            assertEquals("Keep", result.name)
            assertEquals("Only desc", result.description)
            verify(exampleRepository).findById(id)
            verify(exampleRepository, never()).existsByName(any<String>())
            verify(exampleRepository).save(existing)
        }

        @Test
        fun `updates only name when description is null in input`() {
            val id = UUID.randomUUID()
            val existing = Example(id = id, name = "Old", description = "Keep desc")
            whenever(exampleRepository.findById(id)).thenReturn(of(existing))
            whenever(exampleRepository.existsByName("NewOnly")).thenReturn(false)
            whenever(exampleRepository.save(any<Example>())).thenReturn(existing)

            val result = exampleService.update(id, UpdateExampleInput(name = "NewOnly"))

            assertEquals("NewOnly", result.name)
            assertEquals("Keep desc", result.description)
            verify(exampleRepository).findById(id)
            verify(exampleRepository).existsByName("NewOnly")
            verify(exampleRepository).save(existing)
        }

        @Test
        fun `clears description when empty string is sent`() {
            val id = UUID.randomUUID()
            val existing = Example(id = id, name = "Test", description = "Old desc")
            whenever(exampleRepository.findById(id)).thenReturn(of(existing))
            whenever(exampleRepository.save(any<Example>())).thenReturn(existing)

            val result = exampleService.update(id, UpdateExampleInput(description = ""))

            assertEquals("Test", result.name)
            assertNull(result.description)
            verify(exampleRepository).save(existing)
        }
    }

    @Nested
    @DisplayName("delete")
    inner class Delete {
        @Test
        fun `returns true when example exists`() {
            val id = UUID.randomUUID()
            whenever(exampleRepository.existsById(id)).thenReturn(true)

            val result = exampleService.delete(id)

            assertTrue(result)
            verify(exampleRepository).existsById(id)
            verify(exampleRepository).deleteById(id)
        }

        @Test
        fun `returns false when example does not exist`() {
            val id = UUID.randomUUID()
            whenever(exampleRepository.existsById(id)).thenReturn(false)

            val result = exampleService.delete(id)

            assertFalse(result)
            verify(exampleRepository).existsById(id)
            verify(exampleRepository, never()).deleteById(any<UUID>())
        }
    }
}
