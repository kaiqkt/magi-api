package com.kaiqkt.magiapi.integration

import com.kaiqkt.magiapi.application.web.responses.ErrorResponse
import com.kaiqkt.magiapi.application.web.responses.ServerResponse
import com.kaiqkt.magiapi.domain.models.Project
import com.kaiqkt.magiapi.domain.models.ProjectMembership
import com.kaiqkt.magiapi.domain.models.Server
import com.kaiqkt.magiapi.domain.models.User
import com.kaiqkt.magiapi.domain.models.enums.Environment
import com.kaiqkt.magiapi.domain.models.enums.MemberRole
import com.kaiqkt.magiapi.domain.models.enums.MemberStatus
import com.kaiqkt.magiapi.domain.models.enums.ServerStatus
import io.restassured.RestAssured.given
import org.apache.http.HttpStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Nested
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import kotlin.test.Test

class ServerIntegrationTest : IntegrationTest() {

    @Nested
    inner class ServerCreation {

        @Nested
        inner class Auth {

            @Test
            fun `given an unauthenticated request when creating server then return 401 unauthorized`() {
                given()
                    .header("Host", "my-project.localhost.com")
                    .post("/v1/servers?env=DEV")
                    .then()
                    .statusCode(HttpStatus.SC_UNAUTHORIZED)
            }
        }

        @Nested
        inner class RequestValidation {

            @Test
            fun `given a request without tenant when creating server then return 400 bad request`() {
                val owner = userRepository.save(User(email = "owner@example.com", passwordHash = "hash", name = "Owner"))

                given()
                    .header("Authorization", "Bearer ${generateToken(owner.id)}")
                    .post("/v1/servers?env=DEV")
                    .then()
                    .statusCode(HttpStatus.SC_BAD_REQUEST)
            }

            @Test
            fun `given a request with invalid environment when creating server then return 400 bad request`() {
                val owner = userRepository.save(User(email = "owner@example.com", passwordHash = "hash", name = "Owner"))

                given()
                    .header("Authorization", "Bearer ${generateToken(owner.id)}")
                    .header("Host", "my-project.localhost.com")
                    .post("/v1/servers?env=INVALID")
                    .then()
                    .statusCode(HttpStatus.SC_BAD_REQUEST)
            }
        }

        @Nested
        inner class BusinessRules {

            @Test
            fun `given a non-existent project when creating server then return 404 not found`() {
                val owner = userRepository.save(User(email = "owner@example.com", passwordHash = "hash", name = "Owner"))

                val response = given()
                    .header("Authorization", "Bearer ${generateToken(owner.id)}")
                    .header("Host", "unknown-project.localhost.com")
                    .post("/v1/servers?env=DEV")
                    .then()
                    .statusCode(HttpStatus.SC_NOT_FOUND)
                    .extract()
                    .`as`(ErrorResponse::class.java)

                assertEquals("project not found", response.message)
            }

            @Test
            fun `given a requester without membership when creating server then return 404 not found`() {
                val owner = userRepository.save(User(email = "owner@example.com", passwordHash = "hash", name = "Owner"))
                val requester = userRepository.save(User(email = "requester@example.com", passwordHash = "hash", name = "Requester"))
                projectRepository.save(Project(name = "My Project", createdBy = owner.id))

                val response = given()
                    .header("Authorization", "Bearer ${generateToken(requester.id)}")
                    .header("Host", "my-project.localhost.com")
                    .post("/v1/servers?env=DEV")
                    .then()
                    .statusCode(HttpStatus.SC_NOT_FOUND)
                    .extract()
                    .`as`(ErrorResponse::class.java)

                assertEquals("membership not found", response.message)
            }

            @Test
            fun `given a requester with member role when creating server then return 403 forbidden`() {
                val owner = userRepository.save(User(email = "owner@example.com", passwordHash = "hash", name = "Owner"))
                val member = userRepository.save(User(email = "member@example.com", passwordHash = "hash", name = "Member"))
                val project = projectRepository.save(Project(name = "My Project", createdBy = owner.id))
                membershipRepository.save(
                    ProjectMembership(userId = member.id, projectId = project.id, role = MemberRole.MEMBER, status = MemberStatus.ACTIVE)
                )

                val response = given()
                    .header("Authorization", "Bearer ${generateToken(member.id)}")
                    .header("Host", "my-project.localhost.com")
                    .post("/v1/servers?env=DEV")
                    .then()
                    .statusCode(HttpStatus.SC_FORBIDDEN)
                    .extract()
                    .`as`(ErrorResponse::class.java)

                assertEquals("insufficient permission", response.message)
            }

            @Test
            fun `given a server already exists for the environment when creating server then return 409 conflict`() {
                val owner = userRepository.save(User(email = "owner@example.com", passwordHash = "hash", name = "Owner"))
                val project = projectRepository.save(Project(name = "My Project", createdBy = owner.id))
                membershipRepository.save(
                    ProjectMembership(userId = owner.id, projectId = project.id, role = MemberRole.OWNER, status = MemberStatus.ACTIVE)
                )
                serverRepository.save(Server(projectId = project.id, environment = Environment.DEV, agentToken = "existing-token"))

                val response = given()
                    .header("Authorization", "Bearer ${generateToken(owner.id)}")
                    .header("Host", "my-project.localhost.com")
                    .post("/v1/servers?env=DEV")
                    .then()
                    .statusCode(HttpStatus.SC_CONFLICT)
                    .extract()
                    .`as`(ErrorResponse::class.java)

                assertEquals("server already exists", response.message)
            }
        }

        @Nested
        inner class HappyPath {

            @ParameterizedTest
            @EnumSource(MemberRole::class, names = ["OWNER", "ADMIN"])
            fun `given a valid request when creating server then return 200 and persist server`(role: MemberRole) {
                val user = userRepository.save(User(email = "user@example.com", passwordHash = "hash", name = "User"))
                val project = projectRepository.save(Project(name = "My Project", createdBy = user.id))
                membershipRepository.save(
                    ProjectMembership(userId = user.id, projectId = project.id, role = role, status = MemberStatus.ACTIVE)
                )

                val response = given()
                    .header("Authorization", "Bearer ${generateToken(user.id)}")
                    .header("Host", "my-project.localhost.com")
                    .post("/v1/servers?env=DEV")
                    .then()
                    .statusCode(HttpStatus.SC_OK)
                    .extract()
                    .`as`(ServerResponse.Created::class.java)

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
