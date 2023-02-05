package com.wafflestudio.webgam.global.common.exception

import jakarta.validation.ConstraintViolationException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class WebgamControllerAdvice {
    private val logger: Logger = LoggerFactory.getLogger(this::class.java)

    @ExceptionHandler(WebgamException.BadRequest::class)
    fun badRequest(webgamException: WebgamException): ResponseEntity<ErrorResponse> {
        return ResponseEntity(ErrorResponse(webgamException), HttpStatus.BAD_REQUEST)
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun invalidFields(e: MethodArgumentNotValidException): ResponseEntity<ErrorResponse> {
        return ResponseEntity(ErrorResponse(
            ErrorType.BadRequest.INVALID_FIELD.code(),
            "Invalid request parameter or request body",
            e.fieldErrors.joinToString(separator = " ") { it.field + " " + it.defaultMessage + "." }
        ), HttpStatus.BAD_REQUEST)
    }

    @ExceptionHandler(ConstraintViolationException::class)
    fun constraintViolation(e: ConstraintViolationException): ResponseEntity<ErrorResponse?> {
        return ResponseEntity(
            ErrorResponse(
            ErrorType.BadRequest.CONSTRAINT_VIOLATION.code(),
            "Constraint violations",
            e.constraintViolations.joinToString(separator = " ") {
                it.propertyPath.toString().split('.').last() + " " + it.message + ", but " +
                        it.invalidValue + "."
            }
        ), HttpStatus.BAD_REQUEST)
    }

    @ExceptionHandler(WebgamException.Unauthorized::class)
    fun unauthorized(webgamException: WebgamException): ResponseEntity<ErrorResponse> {
        return ResponseEntity(ErrorResponse(webgamException), HttpStatus.UNAUTHORIZED)
    }

    @ExceptionHandler(WebgamException.Forbidden::class)
    fun forbidden(webgamException: WebgamException): ResponseEntity<ErrorResponse> {
        return ResponseEntity(ErrorResponse(webgamException), HttpStatus.FORBIDDEN)
    }

    @ExceptionHandler(WebgamException.NotFound::class)
    fun notFound(webgamException: WebgamException): ResponseEntity<ErrorResponse> {
        return ResponseEntity(ErrorResponse(webgamException), HttpStatus.NOT_FOUND)
    }

    @ExceptionHandler(WebgamException.Conflict::class)
    fun conflict(webgamException: WebgamException): ResponseEntity<ErrorResponse> {
        return ResponseEntity(ErrorResponse(webgamException), HttpStatus.CONFLICT)
    }

    @ExceptionHandler(WebgamException.ServerError::class)
    fun serverError(webgamException: WebgamException): ResponseEntity<ErrorResponse> {
        return ResponseEntity(ErrorResponse(webgamException), HttpStatus.INTERNAL_SERVER_ERROR)
    }
}