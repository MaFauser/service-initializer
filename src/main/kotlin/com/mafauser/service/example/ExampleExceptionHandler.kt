package com.mafauser.service.example

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class ExampleExceptionHandler {
    @ExceptionHandler(ExampleNotFoundException::class)
    @Suppress("UNUSED_PARAMETER")
    fun handleNotFound(ex: ExampleNotFoundException): ResponseEntity<Unit> = ResponseEntity.notFound().build()

    @ExceptionHandler(DuplicateExampleNameException::class)
    @Suppress("UNUSED_PARAMETER")
    fun handleConflict(ex: DuplicateExampleNameException): ResponseEntity<Unit> =
        ResponseEntity.status(HttpStatus.CONFLICT).build()
}
