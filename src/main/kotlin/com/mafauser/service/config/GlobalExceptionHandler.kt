package com.mafauser.service.config

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {
    @ExceptionHandler(NotFoundException::class)
    fun handleNotFound(ex: NotFoundException): ResponseEntity<Unit> = ResponseEntity.notFound().build()

    @ExceptionHandler(ConflictException::class)
    fun handleConflict(ex: ConflictException): ResponseEntity<Unit> = ResponseEntity.status(HttpStatus.CONFLICT).build()

    @ExceptionHandler(InvalidIdException::class)
    fun handleBadRequest(ex: InvalidIdException): ResponseEntity<Unit> =
        ResponseEntity.status(HttpStatus.BAD_REQUEST).build()
}
