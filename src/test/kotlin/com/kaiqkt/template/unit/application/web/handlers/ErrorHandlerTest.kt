package ${package}.unit.application.web.handlers

import ${package}.application.exceptions.InvalidRequestException
import ${package}.application.web.handlers.ErrorHandler
import ${package}.application.web.responses.ErrorResponse
import ${package}.domain.exceptions.DomainException
import ${package}.domain.exceptions.ErrorType
import io.mockk.every
import io.mockk.mockk
import jakarta.validation.ConstraintViolation
import jakarta.validation.ConstraintViolationException
import org.hibernate.validator.internal.engine.path.PathImpl
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.MissingRequestHeaderException
import org.springframework.web.context.request.WebRequest
import kotlin.test.assertEquals

class ErrorHandlerTest {
    private val webRequest: WebRequest = mockk()
    private val errorHandler = ErrorHandler()

    @Test
    fun `given an DomainException when is DEFAULT should return the message based on the error type`() {
        val domainException = DomainException(ErrorType.DEFAULT)

        val response = errorHandler.handleDomainException(domainException)

        assertEquals(HttpStatus.NOT_IMPLEMENTED, response.statusCode)
        assertEquals("DEFAULT", response.body?.message)
    }

    @Test
    fun `given an InvalidRequestException when handling should return all fields errors with his associated message`() {
        val invalidRequestException = InvalidRequestException(mapOf("field" to "invalid"))

        val response = errorHandler.handleInvalidRequestException(invalidRequestException)

        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        assertEquals("invalid", response.body?.details?.get("field"))
        assertEquals("Invalid request", response.body?.message)
    }

    @Suppress("UNCHECKED_CAST")
    @Test
    fun `given an MethodArgumentNotValid when handling should return all fields errors with his associated message`() {
        val methodArgumentNotValidException = mockk<MethodArgumentNotValidException>()
        val fieldError = mockk<FieldError>()

        every { fieldError.field } returns "field"
        every { fieldError.defaultMessage } returns "defaultMessage"
        every { methodArgumentNotValidException.bindingResult.fieldErrors } returns listOf(fieldError)

        val response =
            errorHandler.handleMethodArgumentNotValid(
                methodArgumentNotValidException,
                HttpHeaders(),
                HttpStatus.BAD_REQUEST,
                webRequest,
            ) as ResponseEntity<ErrorResponse>

        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        assertEquals("defaultMessage", response.body?.details?.get("field"))
        assertEquals("Invalid method arguments", response.body?.message)
    }

    @Suppress("UNCHECKED_CAST")
    @Test
    fun `given an MethodArgumentNotValid when field error message is null should return all fields errors with invalid message`() {
        val methodArgumentNotValidException = mockk<MethodArgumentNotValidException>()
        val fieldError = mockk<FieldError>()

        every { fieldError.field } returns "field"
        every { fieldError.defaultMessage } returns null
        every { methodArgumentNotValidException.bindingResult.fieldErrors } returns listOf(fieldError)

        val response =
            errorHandler.handleMethodArgumentNotValid(
                methodArgumentNotValidException,
                HttpHeaders(),
                HttpStatus.BAD_REQUEST,
                webRequest,
            ) as ResponseEntity<ErrorResponse>

        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        assertEquals("invalid", response.body?.details?.get("field"))
        assertEquals("Invalid method arguments", response.body?.message)
    }

    @Test
    fun `given an ConstraintViolationException should return the constraint violations`() {
        val path = PathImpl.createPathFromString("object.field")

        val violation = mockk<ConstraintViolation<Any>>()
        every { violation.propertyPath } returns path
        every { violation.message } returns "message"

        val ex = mockk<ConstraintViolationException>()
        every { ex.constraintViolations } returns setOf(violation)

        val response = errorHandler.handleConstraintViolationException(ex)

        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        assertEquals("message", response.body?.details?.get("object.field"))
        assertEquals("Constraint violation", response.body?.message)
    }

    @Test
    fun `given an MissingRequestHeaderException should return the missing headers`() {
        val ex = mockk<MissingRequestHeaderException>()

        every { ex.headerName } returns "header_name"

        val response = errorHandler.handleMissingRequestHeaderException(ex)

        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        assertEquals("required header", response.body?.details?.get("header_name"))
        assertEquals("Missing header", response.body?.message)
    }
}