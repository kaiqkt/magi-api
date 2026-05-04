package com.kaiqkt.magiapi.integration

import com.kaiqkt.magiapi.application.web.requests.ProjectRequest
import com.kaiqkt.magiapi.application.web.responses.ErrorResponse
import com.kaiqkt.magiapi.domain.models.Project
import com.kaiqkt.magiapi.domain.models.User
import com.kaiqkt.magiapi.resources.GithubHelper
import com.kaiqkt.magiapi.resources.github.responses.GithubUserResponse
import io.restassured.RestAssured.given
import io.restassured.http.ContentType
import org.apache.http.HttpStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Nested
import kotlin.test.Test

class ProjectIntegrationTest : IntegrationTest() {

    @Nested
    inner class ProjectCreation {

        @Nested
        inner class RequestValidation {

            @Test
            fun `given a request with name exceeding max length when creating project then return 400 bad request`() {
                val response = given()
                    .header("Authorization", "Bearer ${generateToken("any-user-id")}")
                    .contentType(ContentType.JSON)
                    .body(ProjectRequest.Create(name = "a".repeat(51)))
                    .post("/v1/projects")
                    .then()
                    .statusCode(HttpStatus.SC_BAD_REQUEST)
                    .extract()
                    .`as`(ErrorResponse::class.java)

                assertEquals("Invalid method arguments", response.message)
                assertEquals("must not exceed 50 characters", response.details["name"])
            }

            @Test
            fun `given a request with blank name when creating project then return 400 bad request`() {
                val response = given()
                    .header("Authorization", "Bearer ${generateToken("any-user-id")}")
                    .contentType(ContentType.JSON)
                    .body(ProjectRequest.Create(name = "   "))
                    .post("/v1/projects")
                    .then()
                    .statusCode(HttpStatus.SC_BAD_REQUEST)
                    .extract()
                    .`as`(ErrorResponse::class.java)

                assertEquals("Invalid method arguments", response.message)
                assertEquals("must not be blank", response.details["name"])
            }

            @Test
            fun `given a request with special characters in name when creating project then return 400 bad request`() {
                val response = given()
                    .header("Authorization", "Bearer ${generateToken("any-user-id")}")
                    .contentType(ContentType.JSON)
                    .body(ProjectRequest.Create(name = "My Project!"))
                    .post("/v1/projects")
                    .then()
                    .statusCode(HttpStatus.SC_BAD_REQUEST)
                    .extract()
                    .`as`(ErrorResponse::class.java)

                assertEquals("Invalid method arguments", response.message)
                assertEquals("must contain only letters and spaces", response.details["name"])
            }
        }

        @Nested
        inner class Auth {

            @Test
            fun `given an unauthenticated request when creating project then return 401 unauthorized`() {
                given()
                    .contentType(ContentType.JSON)
                    .body(ProjectRequest.Create(name = "My Project"))
                    .post("/v1/projects")
                    .then()
                    .statusCode(HttpStatus.SC_UNAUTHORIZED)
            }
        }

        @Nested
        inner class BusinessRules {

            @Test
            fun `given a project with the same name already exists when creating project then return 409 conflict`() {
                val user = userRepository.save(User(email = "john@example.com", passwordHash = "hash", name = "John Doe"))
                projectRepository.save(Project(name = "My Project", userId = user.id))

                val response = given()
                    .header("Authorization", "Bearer ${generateToken(user.id)}")
                    .contentType(ContentType.JSON)
                    .body(ProjectRequest.Create(name = "My Project"))
                    .post("/v1/projects")
                    .then()
                    .statusCode(HttpStatus.SC_CONFLICT)
                    .extract()
                    .`as`(ErrorResponse::class.java)

                assertEquals("project already exist", response.message)
            }
        }

        @Nested
        inner class HappyPath {

            @Test
            fun `given a valid request when creating project then return 201 and persist project`() {
                val user = userRepository.save(User(email = "john@example.com", passwordHash = "hash", name = "John Doe"))

                given()
                    .header("Authorization", "Bearer ${generateToken(user.id)}")
                    .contentType(ContentType.JSON)
                    .body(ProjectRequest.Create(name = "My Project"))
                    .post("/v1/projects")
                    .then()
                    .statusCode(HttpStatus.SC_CREATED)

                val projects = projectRepository.findAll()
                assertEquals(1, projects.size)
                val project = projects.first()
                assertEquals("My Project", project.name)
                assertEquals(user.id, project.userId)
            }
        }
    }

    @Nested
    inner class GitAccountCreation {

        @Nested
        inner class Auth {

            @Test
            fun `given an unauthenticated request when creating git account then return 401 unauthorized`() {
                given()
                    .put("/v1/projects/any-id/git?access_token=some-token")
                    .then()
                    .statusCode(HttpStatus.SC_UNAUTHORIZED)
            }
        }

        @Nested
        inner class BusinessRules {

            @Test
            fun `given a non-existent project when creating git account then return 404 not found`() {
                val user = userRepository.save(User(email = "user@example.com", passwordHash = "hash", name = "User"))

                val response = given()
                    .header("Authorization", "Bearer ${generateToken(user.id)}")
                    .put("/v1/projects/non-existent-id/git?access_token=some-token")
                    .then()
                    .statusCode(HttpStatus.SC_NOT_FOUND)
                    .extract()
                    .`as`(ErrorResponse::class.java)

                assertEquals("project not found", response.message)
            }

            @Test
            fun `given an invalid github access token when creating git account then return 401 unauthorized`() {
                val user = userRepository.save(User(email = "user@example.com", passwordHash = "hash", name = "User"))
                val project = projectRepository.save(Project(name = "My Project", userId = user.id))
                GithubHelper.mockGetUserUnauthorized()

                val response = given()
                    .header("Authorization", "Bearer ${generateToken(user.id)}")
                    .put("/v1/projects/${project.id}/git?access_token=invalid-token")
                    .then()
                    .statusCode(HttpStatus.SC_UNAUTHORIZED)
                    .extract()
                    .`as`(ErrorResponse::class.java)

                assertEquals("invalid git access token", response.message)
            }
        }

        @Nested
        inner class HappyPath {

            @Test
            fun `given a valid github access token when creating git account then return 201 and persist git account`() {
                val user = userRepository.save(User(email = "user@example.com", passwordHash = "hash", name = "User"))
                val project = projectRepository.save(Project(name = "My Project", userId = user.id))
                GithubHelper.mockGetUserSuccessfully(GithubUserResponse(login = "octocat", htmlUrl = "https://github.com/octocat"))

                given()
                    .header("Authorization", "Bearer ${generateToken(user.id)}")
                    .put("/v1/projects/${project.id}/git?access_token=valid-token")
                    .then()
                    .statusCode(HttpStatus.SC_CREATED)

                val gitAccount = gitAccountRepository.findByProjectId(project.id)
                assertEquals(project.id, gitAccount?.projectId)
                assertEquals("octocat", gitAccount?.username)
                assertEquals("https://github.com/octocat", gitAccount?.profileUrl)
                assertEquals("valid-token", gitAccount?.accessToken)
            }
        }
    }
}
