package com.kaiqkt.magiapi.integration

import com.kaiqkt.magiapi.application.web.responses.ErrorResponse
import com.kaiqkt.magiapi.application.web.responses.ServerResponse
import com.kaiqkt.magiapi.domain.models.Project
import com.kaiqkt.magiapi.domain.models.Server
import com.kaiqkt.magiapi.domain.models.User
import com.kaiqkt.magiapi.domain.models.enums.Environment
import com.kaiqkt.magiapi.domain.models.enums.ServerStatus
import io.restassured.RestAssured.given
import org.apache.http.HttpStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Nested
import kotlin.test.Test

class ServerIntegrationTest : IntegrationTest() {

    @Nested
    inner class ServerCreation {

        @Nested
        inner class Auth {

            @Test
            fun `given an unauthenticated request when creating server then return 401 unauthorized`() {
                given()
                    .post("/v1/projects/any-id/servers?env=DEV")
                    .then()
                    .statusCode(HttpStatus.SC_UNAUTHORIZED)
            }
        }

        @Nested
        inner class RequestValidation {

            @Test
            fun `given a request with invalid environment when creating server then return 400 bad request`() {
                given()
                    .header("Authorization", "Bearer ${generateToken("any-user-id")}")
                    .post("/v1/projects/any-id/servers?env=INVALID")
                    .then()
                    .statusCode(HttpStatus.SC_BAD_REQUEST)
            }
        }

        @Nested
        inner class BusinessRules {

            @Test
            fun `given a non-existent project when creating server then return 404 not found`() {
                val user = userRepository.save(User(email = "user@example.com", passwordHash = "hash", name = "User"))

                val response = given()
                    .header("Authorization", "Bearer ${generateToken(user.id)}")
                    .post("/v1/projects/non-existent-id/servers?env=DEV")
                    .then()
                    .statusCode(HttpStatus.SC_NOT_FOUND)
                    .extract()
                    .`as`(ErrorResponse::class.java)

                assertEquals("project not found", response.message)
            }

            @Test
            fun `given a server already exists for the environment when creating server then return 409 conflict`() {
                val user = userRepository.save(User(email = "user@example.com", passwordHash = "hash", name = "User"))
                val project = projectRepository.save(Project(name = "My Project", userId = user.id))
                serverRepository.save(Server(projectId = project.id, environment = Environment.DEV, agentToken = "existing-token"))

                val response = given()
                    .header("Authorization", "Bearer ${generateToken(user.id)}")
                    .post("/v1/projects/${project.id}/servers?env=DEV")
                    .then()
                    .statusCode(HttpStatus.SC_CONFLICT)
                    .extract()
                    .`as`(ErrorResponse::class.java)

                assertEquals("server already exists", response.message)
            }
        }

        @Nested
        inner class HappyPath {

            @Test
            fun `given a valid request when creating server then return 200 and persist server`() {
                val user = userRepository.save(User(email = "user@example.com", passwordHash = "hash", name = "User"))
                val project = projectRepository.save(Project(name = "My Project", userId = user.id))

                val response = given()
                    .header("Authorization", "Bearer ${generateToken(user.id)}")
                    .post("/v1/projects/${project.id}/servers?env=DEV")
                    .then()
                    .statusCode(HttpStatus.SC_OK)
                    .extract()
                    .`as`(ServerResponse::class.java)

                assertNotNull(response.agentToken)

                val servers = serverRepository.findAll()
                assertEquals(1, servers.size)
                val server = servers.first()
                assertEquals(project.id, server.projectId)
                assertEquals(Environment.DEV, server.environment)
                assertEquals(ServerStatus.INACTIVE, server.status)
                assertEquals(response.agentToken, server.agentToken)
            }
        }
    }
}
