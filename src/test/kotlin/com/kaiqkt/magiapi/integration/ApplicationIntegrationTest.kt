package com.kaiqkt.magiapi.integration

import com.kaiqkt.magiapi.application.web.requests.ApplicationRequest
import com.kaiqkt.magiapi.application.web.responses.ErrorResponse
import com.kaiqkt.magiapi.domain.models.Application
import com.kaiqkt.magiapi.domain.models.GitAccount
import com.kaiqkt.magiapi.domain.models.Project
import com.kaiqkt.magiapi.domain.models.User
import com.kaiqkt.magiapi.domain.models.enums.ApplicationStatus
import com.kaiqkt.magiapi.resources.GithubHelper
import com.kaiqkt.magiapi.resources.github.responses.GithubRepositoryResponse
import io.restassured.RestAssured.given
import io.restassured.http.ContentType
import org.apache.http.HttpStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Nested
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
                    .body(ApplicationRequest.Create(name = "My App", description = null))
                    .post("/v1/projects/any-id/applications")
                    .then()
                    .statusCode(HttpStatus.SC_UNAUTHORIZED)
            }
        }

        @Nested
        inner class RequestValidation {

            @Test
            fun `given a request with name exceeding max length when creating application then return 400 bad request`() {
                val response = given()
                    .contentType(ContentType.JSON)
                    .header("Authorization", "Bearer ${generateToken("any-user-id")}")
                    .body(ApplicationRequest.Create(name = "A".repeat(51), description = null))
                    .post("/v1/projects/any-id/applications")
                    .then()
                    .statusCode(HttpStatus.SC_BAD_REQUEST)
                    .extract()
                    .`as`(ErrorResponse::class.java)

                assertEquals("Invalid method arguments", response.message)
                assertEquals("must not exceed 50 characters", response.details["name"])
            }

            @Test
            fun `given a request with blank name when creating application then return 400 bad request`() {
                val response = given()
                    .contentType(ContentType.JSON)
                    .header("Authorization", "Bearer ${generateToken("any-user-id")}")
                    .body(ApplicationRequest.Create(name = "   ", description = null))
                    .post("/v1/projects/any-id/applications")
                    .then()
                    .statusCode(HttpStatus.SC_BAD_REQUEST)
                    .extract()
                    .`as`(ErrorResponse::class.java)

                assertEquals("Invalid method arguments", response.message)
                assertEquals("must not be blank", response.details["name"])
            }

            @Test
            fun `given a request with name containing invalid characters when creating application then return 400 bad request`() {
                val response = given()
                    .contentType(ContentType.JSON)
                    .header("Authorization", "Bearer ${generateToken("any-user-id")}")
                    .body(ApplicationRequest.Create(name = "My App!", description = null))
                    .post("/v1/projects/any-id/applications")
                    .then()
                    .statusCode(HttpStatus.SC_BAD_REQUEST)
                    .extract()
                    .`as`(ErrorResponse::class.java)

                assertEquals("Invalid method arguments", response.message)
                assertEquals("must contain only letters and spaces", response.details["name"])
            }

            @Test
            fun `given a request with description exceeding max length when creating application then return 400 bad request`() {
                val response = given()
                    .contentType(ContentType.JSON)
                    .header("Authorization", "Bearer ${generateToken("any-user-id")}")
                    .body(ApplicationRequest.Create(name = "My App", description = "D".repeat(256)))
                    .post("/v1/projects/any-id/applications")
                    .then()
                    .statusCode(HttpStatus.SC_BAD_REQUEST)
                    .extract()
                    .`as`(ErrorResponse::class.java)

                assertEquals("Invalid method arguments", response.message)
                assertEquals("must not exceed 255 characters", response.details["description"])
            }
        }

        @Nested
        inner class BusinessRules {

            @Test
            fun `given a non-existent project when creating application then return 404 not found`() {
                val response = given()
                    .contentType(ContentType.JSON)
                    .header("Authorization", "Bearer ${generateToken("any-user-id")}")
                    .body(ApplicationRequest.Create(name = "My App", description = null))
                    .post("/v1/projects/non-existent-id/applications")
                    .then()
                    .statusCode(HttpStatus.SC_NOT_FOUND)
                    .extract()
                    .`as`(ErrorResponse::class.java)

                assertEquals("project not found", response.message)
            }

            @Test
            fun `given an application with the same name in the project when creating application then return 409 conflict`() {
                val user = userRepository.save(User(email = "user@example.com", passwordHash = "hash", name = "User"))
                val project = projectRepository.save(Project(name = "My Project", userId = user.id))
                applicationRepository.save(
                    Application(
                        name = "my-app",
                        projectId = project.id,
                        repositoryUrl = "https://github.com/user/my-project_my-app"
                    )
                )

                val response = given()
                    .contentType(ContentType.JSON)
                    .header("Authorization", "Bearer ${generateToken(user.id)}")
                    .body(ApplicationRequest.Create(name = "My App", description = null))
                    .post("/v1/projects/${project.id}/applications")
                    .then()
                    .statusCode(HttpStatus.SC_CONFLICT)
                    .extract()
                    .`as`(ErrorResponse::class.java)

                assertEquals("application already exist", response.message)
            }

            @Test
            fun `given a project without git account when creating application then return 404 not found`() {
                val user = userRepository.save(User(email = "user@example.com", passwordHash = "hash", name = "User"))
                val project = projectRepository.save(Project(name = "My Project", userId = user.id))

                val response = given()
                    .contentType(ContentType.JSON)
                    .header("Authorization", "Bearer ${generateToken(user.id)}")
                    .body(ApplicationRequest.Create(name = "My App", description = null))
                    .post("/v1/projects/${project.id}/applications")
                    .then()
                    .statusCode(HttpStatus.SC_NOT_FOUND)
                    .extract()
                    .`as`(ErrorResponse::class.java)

                assertEquals("git account not found", response.message)
            }
        }

        @Nested
        inner class HappyPath {

            @Test
            fun `given a valid request when creating application then return 201 and persist application`() {
                val user = userRepository.save(User(email = "user@example.com", passwordHash = "hash", name = "User"))
                val project = projectRepository.save(Project(name = "My Project", userId = user.id))
                gitAccountRepository.save(
                    GitAccount(
                        projectId = project.id,
                        username = "github-user",
                        accessToken = "valid-token"
                    )
                )
                GithubHelper.mockCreateRepositorySuccessfully(GithubRepositoryResponse(htmlUrl = "https://github.com/github-user/my-project_my-app"))

                given()
                    .contentType(ContentType.JSON)
                    .header("Authorization", "Bearer ${generateToken(user.id)}")
                    .body(ApplicationRequest.Create(name = "My App", description = "My application description"))
                    .post("/v1/projects/${project.id}/applications")
                    .then()
                    .statusCode(HttpStatus.SC_CREATED)

                val applications = applicationRepository.findAll()
                assertEquals(1, applications.size)
                val application = applications.first()
                assertEquals("my-app", application.name)
                assertEquals("My application description", application.description)
                assertEquals("https://github.com/github-user/my-project_my-app", application.repositoryUrl)
                assertEquals(project.id, application.projectId)
            }
        }
    }

    @Nested
    inner class CiWorkflowProvisioning {

        @Nested
        inner class Auth {

            @Test
            fun `given an unauthenticated request when provisioning ci workflow then return 401 unauthorized`() {
                given()
                    .put("/v1/projects/any-id/applications/any-id/ci")
                    .then()
                    .statusCode(HttpStatus.SC_UNAUTHORIZED)
            }
        }

        @Nested
        inner class BusinessRules {

            @Test
            fun `given a non-existent project when provisioning ci workflow then return 404 not found`() {
                val response = given()
                    .header("Authorization", "Bearer ${generateToken("any-user-id")}")
                    .put("/v1/projects/non-existent-id/applications/any-id/ci")
                    .then()
                    .statusCode(HttpStatus.SC_NOT_FOUND)
                    .extract()
                    .`as`(ErrorResponse::class.java)

                assertEquals("project not found", response.message)
            }

            @Test
            fun `given a non-existent application when provisioning ci workflow then return 404 not found`() {
                val user = userRepository.save(User(email = "user@example.com", passwordHash = "hash", name = "User"))
                val project = projectRepository.save(Project(name = "My Project", userId = user.id))

                val response = given()
                    .header("Authorization", "Bearer ${generateToken(user.id)}")
                    .put("/v1/projects/${project.id}/applications/non-existent-id/ci")
                    .then()
                    .statusCode(HttpStatus.SC_NOT_FOUND)
                    .extract()
                    .`as`(ErrorResponse::class.java)

                assertEquals("application not found", response.message)
            }

            @Test
            fun `given a project without git account when provisioning ci workflow then return 404 not found`() {
                val user = userRepository.save(User(email = "user@example.com", passwordHash = "hash", name = "User"))
                val project = projectRepository.save(Project(name = "My Project", userId = user.id))
                val application = applicationRepository.save(
                    Application(
                        name = "my-app",
                        projectId = project.id,
                        repositoryUrl = "https://github.com/user/my-project_my-app"
                    )
                )

                val response = given()
                    .header("Authorization", "Bearer ${generateToken(user.id)}")
                    .put("/v1/projects/${project.id}/applications/${application.id}/ci")
                    .then()
                    .statusCode(HttpStatus.SC_NOT_FOUND)
                    .extract()
                    .`as`(ErrorResponse::class.java)

                assertEquals("git account not found", response.message)
            }

            @Test
            fun `given an invalid git access token when provisioning ci workflow then return 401 unauthorized`() {
                val user = userRepository.save(User(email = "user@example.com", passwordHash = "hash", name = "User"))
                val project = projectRepository.save(Project(name = "My Project", userId = user.id))
                gitAccountRepository.save(
                    GitAccount(
                        projectId = project.id,
                        username = "github-user",
                        accessToken = "expired-token"
                    )
                )
                val application = applicationRepository.save(
                    Application(
                        name = "my-app",
                        projectId = project.id,
                        repositoryUrl = "https://github.com/user/my-project_my-app"
                    )
                )
                GithubHelper.mockUploadContentUnauthorized()

                val response = given()
                    .header("Authorization", "Bearer ${generateToken(user.id)}")
                    .put("/v1/projects/${project.id}/applications/${application.id}/ci")
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
            fun `given a valid request when provisioning ci workflow then return 200 and update application status`() {
                val user = userRepository.save(User(email = "user@example.com", passwordHash = "hash", name = "User"))
                val project = projectRepository.save(Project(name = "My Project", userId = user.id))
                gitAccountRepository.save(
                    GitAccount(
                        projectId = project.id,
                        username = "github-user",
                        accessToken = "valid-token"
                    )
                )
                val application = applicationRepository.save(
                    Application(
                        name = "my-app",
                        projectId = project.id,
                        repositoryUrl = "https://github.com/user/my-project_my-app"
                    )
                )
                GithubHelper.mockUploadContentSuccessfully()

                given()
                    .header("Authorization", "Bearer ${generateToken(user.id)}")
                    .put("/v1/projects/${project.id}/applications/${application.id}/ci")
                    .then()
                    .statusCode(HttpStatus.SC_OK)

                val saved = applicationRepository.findById(application.id).get()
                assertEquals(ApplicationStatus.CREATED, saved.status)
            }

            @Test
            fun `given an already initialized application when provisioning ci workflow then return 200 without uploading`() {
                val user = userRepository.save(User(email = "user@example.com", passwordHash = "hash", name = "User"))
                val project = projectRepository.save(Project(name = "My Project", userId = user.id))
                gitAccountRepository.save(
                    GitAccount(
                        projectId = project.id,
                        username = "github-user",
                        accessToken = "valid-token"
                    )
                )
                val application = applicationRepository.save(
                    Application(
                        name = "my-app",
                        projectId = project.id,
                        repositoryUrl = "https://github.com/user/my-project_my-app",
                        status = ApplicationStatus.CREATED
                    )
                )

                given()
                    .header("Authorization", "Bearer ${generateToken(user.id)}")
                    .put("/v1/projects/${project.id}/applications/${application.id}/ci")
                    .then()
                    .statusCode(HttpStatus.SC_OK)

                val saved = applicationRepository.findById(application.id).get()
                assertEquals(ApplicationStatus.CREATED, saved.status)
            }
        }
    }
}
