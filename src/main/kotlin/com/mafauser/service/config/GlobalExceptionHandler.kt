package com.mafauser.service.config

import com.mafauser.service.example.DuplicateExampleNameException
import com.mafauser.service.example.ExampleNotFoundException
import com.mafauser.service.example.InvalidIdException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {
    @ExceptionHandler(ExampleNotFoundException::class)
    fun handleNotFound(ex: ExampleNotFoundException): ResponseEntity<Unit> = ResponseEntity.notFound().build()

    @ExceptionHandler(DuplicateExampleNameException::class)
    fun handleConflict(ex: DuplicateExampleNameException): ResponseEntity<Unit> =
        ResponseEntity.status(HttpStatus.CONFLICT).build()

    @ExceptionHandler(InvalidIdException::class)
    fun handleBadRequest(ex: InvalidIdException): ResponseEntity<Unit> =
        ResponseEntity.status(HttpStatus.BAD_REQUEST).build()
}
