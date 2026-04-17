package com.kaiqkt.magiapi.integration

import com.kaiqkt.magiapi.application.web.requests.ApplicationRequest
import com.kaiqkt.magiapi.application.web.responses.ErrorResponse
import com.kaiqkt.magiapi.domain.models.GitAccount
import com.kaiqkt.magiapi.domain.models.Project
import com.kaiqkt.magiapi.domain.models.ProjectMembership
import com.kaiqkt.magiapi.domain.models.User
import com.kaiqkt.magiapi.domain.models.enums.MemberRole
import com.kaiqkt.magiapi.domain.models.enums.MemberStatus
import com.kaiqkt.magiapi.integration.resources.GithubHelper
import com.kaiqkt.magiapi.resources.github.responses.GithubRepositoryResponse
import io.restassured.RestAssured.given
import io.restassured.http.ContentType
import org.apache.http.HttpStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Nested
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import kotlin.test.Test

class ApplicationIntegrationTest : IntegrationTest() {

    @Nested
    inner class ApplicationCreation {

        @Nested
        inner class Auth {

            @Test
            fun `given an unauthenticated request when creating application then return 401 unauthorized`() {
                given()
                    .contentType(ContentType.JSON)
                    .header("Host", "my-project.localhost.com")
                    .body(ApplicationRequest.Create(name = "My App", description = null))
                    .post("/v1/applications")
                    .then()
                    .statusCode(HttpStatus.SC_UNAUTHORIZED)
            }
        }

        @Nested
        inner class RequestValidation {

            @Test
            fun `given a request with name exceeding max length when creating application then return 400 bad request`() {
                val user = userRepository.save(User(email = "user@example.com", passwordHash = "hash", name = "User"))

                val response = given()
                    .contentType(ContentType.JSON)
                    .header("Authorization", "Bearer ${generateToken(user.id)}")
                    .header("Host", "my-project.localhost.com")
                    .body(ApplicationRequest.Create(name = "A".repeat(51), description = null))
                    .post("/v1/applications")
                    .then()
                    .statusCode(HttpStatus.SC_BAD_REQUEST)
                    .extract()
                    .`as`(ErrorResponse::class.java)

                assertEquals("Invalid method arguments", response.message)
                assertEquals("must not exceed 50 characters", response.details["name"])
            }

            @Test
            fun `given a request with name containing invalid characters when creating application then return 400 bad request`() {
                val user = userRepository.save(User(email = "user@example.com", passwordHash = "hash", name = "User"))

                val response = given()
                    .contentType(ContentType.JSON)
                    .header("Authorization", "Bearer ${generateToken(user.id)}")
                    .header("Host", "my-project.localhost.com")
                    .body(ApplicationRequest.Create(name = "app-123!", description = null))
                    .post("/v1/applications")
                    .then()
                    .statusCode(HttpStatus.SC_BAD_REQUEST)
                    .extract()
                    .`as`(ErrorResponse::class.java)

                assertEquals("Invalid method arguments", response.message)
                assertEquals("must contain only letters and spaces", response.details["name"])
            }

            @Test
            fun `given a request with description exceeding max length when creating application then return 400 bad request`() {
                val user = userRepository.save(User(email = "user@example.com", passwordHash = "hash", name = "User"))

                val response = given()
                    .contentType(ContentType.JSON)
                    .header("Authorization", "Bearer ${generateToken(user.id)}")
                    .header("Host", "my-project.localhost.com")
                    .body(ApplicationRequest.Create(name = "My App", description = "D".repeat(256)))
                    .post("/v1/applications")
                    .then()
                    .statusCode(HttpStatus.SC_BAD_REQUEST)
                    .extract()
                    .`as`(ErrorResponse::class.java)

                assertEquals("Invalid method arguments", response.message)
                assertEquals("must not exceed 255 characters", response.details["description"])
            }

            @Test
            fun `given a request without tenant when creating application then return 400 bad request`() {
                val user = userRepository.save(User(email = "user@example.com", passwordHash = "hash", name = "User"))

                given()
                    .contentType(ContentType.JSON)
                    .header("Authorization", "Bearer ${generateToken(user.id)}")
                    .body(ApplicationRequest.Create(name = "My App", description = null))
                    .post("/v1/applications")
                    .then()
                    .statusCode(HttpStatus.SC_BAD_REQUEST)
            }
        }

        @Nested
        inner class BusinessRules {

            @Test
            fun `given a non-existent project when creating application then return 404 not found`() {
                val user = userRepository.save(User(email = "user@example.com", passwordHash = "hash", name = "User"))

                val response = given()
                    .contentType(ContentType.JSON)
                    .header("Authorization", "Bearer ${generateToken(user.id)}")
                    .header("Host", "unknown-project.localhost.com")
                    .body(ApplicationRequest.Create(name = "My App", description = null))
                    .post("/v1/applications")
                    .then()
                    .statusCode(HttpStatus.SC_NOT_FOUND)
                    .extract()
                    .`as`(ErrorResponse::class.java)

                assertEquals("project not found", response.message)
            }

            @Test
            fun `given a requester without membership when creating application then return 404 not found`() {
                val owner = userRepository.save(User(email = "owner@example.com", passwordHash = "hash", name = "Owner"))
                val requester = userRepository.save(User(email = "requester@example.com", passwordHash = "hash", name = "Requester"))
                projectRepository.save(Project(name = "My Project", createdBy = owner.id))

                val response = given()
                    .contentType(ContentType.JSON)
                    .header("Authorization", "Bearer ${generateToken(requester.id)}")
                    .header("Host", "my-project.localhost.com")
                    .body(ApplicationRequest.Create(name = "My App", description = null))
                    .post("/v1/applications")
                    .then()
                    .statusCode(HttpStatus.SC_NOT_FOUND)
                    .extract()
                    .`as`(ErrorResponse::class.java)

                assertEquals("membership not found", response.message)
            }

            @Test
            fun `given a requester with member role when creating application then return 403 forbidden`() {
                val owner = userRepository.save(User(email = "owner@example.com", passwordHash = "hash", name = "Owner"))
                val member = userRepository.save(User(email = "member@example.com", passwordHash = "hash", name = "Member"))
                val project = projectRepository.save(Project(name = "My Project", createdBy = owner.id))
                membershipRepository.save(
                    ProjectMembership(userId = member.id, projectId = project.id, role = MemberRole.MEMBER, status = MemberStatus.ACTIVE)
                )

                val response = given()
                    .contentType(ContentType.JSON)
                    .header("Authorization", "Bearer ${generateToken(member.id)}")
                    .header("Host", "my-project.localhost.com")
                    .body(ApplicationRequest.Create(name = "My App", description = null))
                    .post("/v1/applications")
                    .then()
                    .statusCode(HttpStatus.SC_FORBIDDEN)
                    .extract()
                    .`as`(ErrorResponse::class.java)

                assertEquals("insufficient permission", response.message)
            }

            @Test
            fun `given a project without git account when creating application then return 404 not found`() {
                val user = userRepository.save(User(email = "user@example.com", passwordHash = "hash", name = "User"))
                val project = projectRepository.save(Project(name = "My Project", createdBy = user.id))
                membershipRepository.save(
                    ProjectMembership(userId = user.id, projectId = project.id, role = MemberRole.OWNER, status = MemberStatus.ACTIVE)
                )

                val response = given()
                    .contentType(ContentType.JSON)
                    .header("Authorization", "Bearer ${generateToken(user.id)}")
                    .header("Host", "my-project.localhost.com")
                    .body(ApplicationRequest.Create(name = "My App", description = null))
                    .post("/v1/applications")
                    .then()
                    .statusCode(HttpStatus.SC_NOT_FOUND)
                    .extract()
                    .`as`(ErrorResponse::class.java)

                assertEquals("git account not found", response.message)
            }
        }

        @Nested
        inner class HappyPath {

            @ParameterizedTest
            @EnumSource(MemberRole::class, names = ["OWNER", "ADMIN"])
            fun `given a valid request when creating application then return 204 and persist application`(role: MemberRole) {
                val user = userRepository.save(User(email = "user@example.com", passwordHash = "hash", name = "User"))
                val project = projectRepository.save(Project(name = "My Project", createdBy = user.id))
                membershipRepository.save(
                    ProjectMembership(userId = user.id, projectId = project.id, role = role, status = MemberStatus.ACTIVE)
                )
                gitAccountRepository.save(
                    GitAccount(projectId = project.id, username = "github-user", accessToken = "valid-token")
                )
                GithubHelper.mockCreateRepositorySuccessfully(
                    GithubRepositoryResponse(htmlUrl = "https://github.com/github-user/my-app")
                )

                given()
                    .contentType(ContentType.JSON)
                    .header("Authorization", "Bearer ${generateToken(user.id)}")
                    .header("Host", "my-project.localhost.com")
                    .body(ApplicationRequest.Create(name = "My App", description = "My application description"))
                    .post("/v1/applications")
                    .then()
                    .statusCode(HttpStatus.SC_NO_CONTENT)

                val applications = applicationRepository.findAll()
                assertEquals(1, applications.size)
                val application = applications.first()
                assertEquals("My App", application.name)
                assertEquals("My application description", application.description)
                assertEquals("https://github.com/github-user/my-app", application.repositoryUrl)
                assertEquals(project.id, application.projectId)
            }
        }
    }
}
