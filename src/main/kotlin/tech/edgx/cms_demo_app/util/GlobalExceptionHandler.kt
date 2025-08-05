package tech.edgx.cms_demo_app.util

import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.authentication.InsufficientAuthenticationException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.util.NoSuchElementException

@RestControllerAdvice
class GlobalExceptionHandler {

    private val logger = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    @ExceptionHandler(InsufficientAuthenticationException::class)
    fun handleAuthenticationException(ex: InsufficientAuthenticationException): ResponseEntity<Map<String, Any>> {
        logger.debug("Authentication exception: ${ex.message}")
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(mapOf("error" to "Unauthorized", "message" to (ex.message ?: "Authentication required")))
    }

    @ExceptionHandler(NoSuchElementException::class)
    fun handleNoSuchElementException(ex: NoSuchElementException): ResponseEntity<Map<String, Any>> {
        logger.debug("Resource not found: ${ex.message}")
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(mapOf("error" to "Not Found", "message" to (ex.message ?: "Resource not found")))
    }

    @ExceptionHandler(Exception::class)
    fun handleAllExceptions(ex: Exception): ResponseEntity<Map<String, Any>> {
        logger.error("Unhandled exception", ex)
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(mapOf("error" to "Internal Server Error", "message" to (ex.message ?: "Unknown error")))
    }
}