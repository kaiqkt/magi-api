package com.kaiqkt.magiapi.integration

import com.kaiqkt.magiapi.application.web.requests.UserRequest
import com.kaiqkt.magiapi.application.web.responses.ErrorResponse
import com.kaiqkt.magiapi.domain.models.User
import com.kaiqkt.magiapi.domain.models.enums.Role
import io.restassured.RestAssured.given
import io.restassured.http.ContentType
import org.apache.http.HttpStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Nested
import kotlin.test.Test

class UserIntegrationTest : IntegrationTest() {

    @Nested
    inner class UserCreation {

        @Nested
        inner class RequestValidation {

            @Test
            fun `given a request with fields exceeding max length when creating user then return 400 bad request`() {
                val request = UserRequest.Create(
                    name = "a".repeat(101),
                    email = "e@email.co" + "m".repeat(100),
                    password = "Secret123!" + "x".repeat(50),
                )

                val response = given()
                    .contentType(ContentType.JSON)
                    .body(request)
                    .post("/v1/users")
                    .then()
                    .statusCode(HttpStatus.SC_BAD_REQUEST)
                    .extract()
                    .`as`(ErrorResponse::class.java)

                assertEquals("Invalid method arguments", response.message)
                assertEquals("must not exceed 100 characters", response.details["name"])
                assertEquals("must not exceed 100 characters", response.details["email"])
                assertEquals("must not exceed 50 characters", response.details["password"])
            }

            @Test
            fun `given a request with special characters in name when creating user then return 400 bad request`() {
                val request = UserRequest.Create(
                    name = "a-bc!",
                    email = "john@example.com",
                    password = "Secret123!",
                )

                val response = given()
                    .contentType(ContentType.JSON)
                    .body(request)
                    .post("/v1/users")
                    .then()
                    .statusCode(HttpStatus.SC_BAD_REQUEST)
                    .extract()
                    .`as`(ErrorResponse::class.java)

                assertEquals("Invalid method arguments", response.message)
                assertEquals("must contain only letters and spaces", response.details["name"])
            }

            @Test
            fun `given a request with invalid email format when creating user then return 400 bad request`() {
                val request = UserRequest.Create(
                    name = "John Doe",
                    email = "email.com",
                    password = "Secret123!",
                )

                val response = given()
                    .contentType(ContentType.JSON)
                    .body(request)
                    .post("/v1/users")
                    .then()
                    .statusCode(HttpStatus.SC_BAD_REQUEST)
                    .extract()
                    .`as`(ErrorResponse::class.java)

                assertEquals("Invalid method arguments", response.message)
                assertEquals("must be a valid email", response.details["email"])
            }

            @Test
            fun `given a request with invalid password format when creating user then return 400 bad request`() {
                val request = UserRequest.Create(
                    name = "John Doe",
                    email = "john@example.com",
                    password = "password",
                )

                val response = given()
                    .contentType(ContentType.JSON)
                    .body(request)
                    .post("/v1/users")
                    .then()
                    .statusCode(HttpStatus.SC_BAD_REQUEST)
                    .extract()
                    .`as`(ErrorResponse::class.java)

                assertEquals("Invalid method arguments", response.message)
                assertEquals(
                    "must be at least 8 characters long and include uppercase, lowercase, number and special character",
                    response.details["password"],
                )
            }
        }

        @Nested
        inner class BusinessRules {

            @Test
            fun `given an email already in use when creating user then return 409 conflict`() {
                userRepository.save(User(email = "john@example.com", passwordHash = "hash", name = "John Doe"))

                val request = UserRequest.Create(
                    name = "John Doe",
                    email = "john@example.com",
                    password = "Secret123!",
                )

                val response = given()
                    .contentType(ContentType.JSON)
                    .body(request)
                    .post("/v1/users")
                    .then()
                    .statusCode(HttpStatus.SC_CONFLICT)
                    .extract()
                    .`as`(ErrorResponse::class.java)

                assertEquals("email already exists", response.message)
            }
        }

        @Nested
        inner class HappyPath {

            @Test
            fun `given a valid request when creating user then return 204 and persist user`() {
                val request = UserRequest.Create(
                    name = "John Doe",
                    email = "john@example.com",
                    password = "Secret123!",
                )

                given()
                    .contentType(ContentType.JSON)
                    .body(request)
                    .post("/v1/users")
                    .then()
                    .statusCode(HttpStatus.SC_NO_CONTENT)

                val saved = userRepository.findByEmail(request.email)
                assertNotNull(saved)
                assertEquals(request.name, saved!!.name)
                assertEquals(request.email, saved.email)
                assertFalse(saved.passwordHash.isBlank())
                assertEquals(setOf(Role.USER), saved.roles)
                assertNotNull(saved.createdAt)
            }
        }
    }
}
