package com.kaiqkt.magiapi.integration

import com.kaiqkt.magiapi.application.web.requests.AuthenticationRequest
import com.kaiqkt.magiapi.application.web.responses.ErrorResponse
import com.kaiqkt.magiapi.application.web.responses.LoginResponse
import com.kaiqkt.magiapi.domain.models.User
import io.restassured.RestAssured.given
import io.restassured.http.ContentType
import org.apache.http.HttpStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import kotlin.test.Test

class AuthenticationIntegrationTest : IntegrationTest() {

    @Nested
    inner class Login {

        @Nested
        inner class BusinessRules {

            @Test
            fun `given a non-existent email when authenticating then return 404 not found`() {
                val request = AuthenticationRequest(
                    email = "nonexistent@example.com",
                    password = "Secret123!",
                )

                val response = given()
                    .contentType(ContentType.JSON)
                    .body(request)
                    .post("/v1/auth")
                    .then()
                    .statusCode(HttpStatus.SC_NOT_FOUND)
                    .extract()
                    .`as`(ErrorResponse::class.java)

                assertEquals("user not found", response.message)
            }

            @Test
            fun `given an incorrect password when authenticating then return 401 unauthorized`() {
                userRepository.save(
                    User(
                        email = "john@example.com",
                        passwordHash = passwordEncoder.encode("Secret123!"),
                        name = "John Doe",
                    )
                )

                val request = AuthenticationRequest(
                    email = "john@example.com",
                    password = "WrongPass123!",
                )

                val response = given()
                    .contentType(ContentType.JSON)
                    .body(request)
                    .post("/v1/auth")
                    .then()
                    .statusCode(HttpStatus.SC_UNAUTHORIZED)
                    .extract()
                    .`as`(ErrorResponse::class.java)

                assertEquals("invalid credentials", response.message)
            }
        }

        @Nested
        inner class HappyPath {

            @Test
            fun `given valid credentials when authenticating then return 200 and access token`() {
                userRepository.save(
                    User(
                        email = "john@example.com",
                        passwordHash = passwordEncoder.encode("Secret123!"),
                        name = "John Doe",
                    )
                )

                val request = AuthenticationRequest(
                    email = "john@example.com",
                    password = "Secret123!",
                )

                val response = given()
                    .contentType(ContentType.JSON)
                    .body(request)
                    .post("/v1/auth")
                    .then()
                    .statusCode(HttpStatus.SC_OK)
                    .extract()
                    .`as`(LoginResponse::class.java)

                assertFalse(response.accessToken.isBlank())
                assertTrue(response.expiresIn > 0)
            }
        }
    }
}
