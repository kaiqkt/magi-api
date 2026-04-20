package com.kaiqkt.magiapi.integration

import com.kaiqkt.magiapi.application.web.requests.ProjectRequest
import com.kaiqkt.magiapi.application.web.responses.ErrorResponse
import com.kaiqkt.magiapi.domain.models.Project
import com.kaiqkt.magiapi.domain.models.ProjectMembership
import com.kaiqkt.magiapi.domain.models.User
import com.kaiqkt.magiapi.domain.models.enums.MemberRole
import com.kaiqkt.magiapi.integration.resources.GithubHelper
import com.kaiqkt.magiapi.resources.github.responses.GithubUserResponse
import io.restassured.RestAssured.given
import io.restassured.http.ContentType
import org.apache.http.HttpStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Nested
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import kotlin.test.Test

class ProjectIntegrationTest : IntegrationTest() {

    @Nested
    inner class ProjectCreation {

        @Nested
        inner class RequestValidation {

            @Test
            fun `given a request with name exceeding max length when creating project then return 400 bad request`() {
                val user = userRepository.save(User(email = "john@example.com", passwordHash = "hash", name = "John Doe"))

                val request = ProjectRequest.Create(name = "a".repeat(51))

                val response = given()
                    .header("Authorization", "Bearer ${generateToken(user.id)}")
                    .contentType(ContentType.JSON)
                    .body(request)
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
                val user = userRepository.save(User(email = "john@example.com", passwordHash = "hash", name = "John Doe"))

                val request = ProjectRequest.Create(name = "   ")

                val response = given()
                    .header("Authorization", "Bearer ${generateToken(user.id)}")
                    .contentType(ContentType.JSON)
                    .body(request)
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
                val user = userRepository.save(User(email = "john@example.com", passwordHash = "hash", name = "John Doe"))

                val request = ProjectRequest.Create(name = "My Project!")

                val response = given()
                    .header("Authorization", "Bearer ${generateToken(user.id)}")
                    .contentType(ContentType.JSON)
                    .body(request)
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
                val request = ProjectRequest.Create(name = "My Project")

                given()
                    .contentType(ContentType.JSON)
                    .body(request)
                    .post("/v1/projects")
                    .then()
                    .statusCode(HttpStatus.SC_UNAUTHORIZED)
            }
        }

        @Nested
        inner class BusinessRules {

            @Test
            fun `given a tenant already in use when creating project then return 409 conflict`() {
                val user = userRepository.save(User(email = "john@example.com", passwordHash = "hash", name = "John Doe"))
                projectRepository.save(Project(name = "My Project", createdBy = user.id))

                val request = ProjectRequest.Create(name = "My Project")

                val response = given()
                    .header("Authorization", "Bearer ${generateToken(user.id)}")
                    .contentType(ContentType.JSON)
                    .body(request)
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
            fun `given a valid request when creating project then return 204 and persist project with owner membership`() {
                val user = userRepository.save(User(email = "john@example.com", passwordHash = "hash", name = "John Doe"))

                val request = ProjectRequest.Create(name = "My Project")

                given()
                    .header("Authorization", "Bearer ${generateToken(user.id)}")
                    .contentType(ContentType.JSON)
                    .body(request)
                    .post("/v1/projects")
                    .then()
                    .statusCode(HttpStatus.SC_CREATED)

                val projects = projectRepository.findAll()
                assertEquals(1, projects.size)
                val project = projects.first()
                assertEquals("My Project", project.name)
                assertEquals("my-project", project.tenantId)
                assertEquals(user.id, project.createdBy)

                val memberships = membershipRepository.findAll()
                assertEquals(1, memberships.size)
                val membership = memberships.first()
                assertEquals(user.id, membership.userId)
                assertEquals(project.id, membership.projectId)
                assertEquals(MemberRole.OWNER, membership.role)
            }
        }
    }

    @Nested
    inner class ProjectMembership {

        @Nested
        inner class Auth {

            @Test
            fun `given an unauthenticated request when creating membership then return 401 unauthorized`() {
                val guest = userRepository.save(User(email = "guest@example.com", passwordHash = "hash", name = "Guest"))

                given()
                    .header("Host", "my-project.localhost.com")
                    .post("/v1/projects/member/${guest.id}")
                    .then()
                    .statusCode(HttpStatus.SC_UNAUTHORIZED)
            }
        }

        @Nested
        inner class RequestValidation {

            @Test
            fun `given a request without tenant when creating membership then return 400 bad request`() {
                val owner = userRepository.save(User(email = "owner@example.com", passwordHash = "hash", name = "Owner"))
                val guest = userRepository.save(User(email = "guest@example.com", passwordHash = "hash", name = "Guest"))

                given()
                    .header("Authorization", "Bearer ${generateToken(owner.id)}")
                    .post("/v1/projects/member/${guest.id}")
                    .then()
                    .statusCode(HttpStatus.SC_BAD_REQUEST)
            }
        }

        @Nested
        inner class BusinessRules {

            @Test
            fun `given a non-existent project when creating membership then return 404 not found`() {
                val owner = userRepository.save(User(email = "owner@example.com", passwordHash = "hash", name = "Owner"))
                val guest = userRepository.save(User(email = "guest@example.com", passwordHash = "hash", name = "Guest"))

                val response = given()
                    .header("Authorization", "Bearer ${generateToken(owner.id)}")
                    .header("Host", "unknown-project.localhost.com")
                    .post("/v1/projects/member/${guest.id}")
                    .then()
                    .statusCode(HttpStatus.SC_NOT_FOUND)
                    .extract()
                    .`as`(ErrorResponse::class.java)

                assertEquals("project not found", response.message)
            }

            @Test
            fun `given a requester without membership when creating membership then return 404 not found`() {
                val owner = userRepository.save(User(email = "owner@example.com", passwordHash = "hash", name = "Owner"))
                val requester = userRepository.save(User(email = "requester@example.com", passwordHash = "hash", name = "Requester"))
                val guest = userRepository.save(User(email = "guest@example.com", passwordHash = "hash", name = "Guest"))
                projectRepository.save(Project(name = "My Project", createdBy = owner.id))

                val response = given()
                    .header("Authorization", "Bearer ${generateToken(requester.id)}")
                    .header("Host", "my-project.localhost.com")
                    .post("/v1/projects/member/${guest.id}")
                    .then()
                    .statusCode(HttpStatus.SC_NOT_FOUND)
                    .extract()
                    .`as`(ErrorResponse::class.java)

                assertEquals("membership not found", response.message)
            }

            @Test
            fun `given a requester with member role when creating membership then return 403 forbidden`() {
                val owner = userRepository.save(User(email = "owner@example.com", passwordHash = "hash", name = "Owner"))
                val member = userRepository.save(User(email = "member@example.com", passwordHash = "hash", name = "Member"))
                val guest = userRepository.save(User(email = "guest@example.com", passwordHash = "hash", name = "Guest"))
                val project = projectRepository.save(Project(name = "My Project", createdBy = owner.id))
                membershipRepository.save(
                    ProjectMembership(userId = member.id, projectId = project.id, role = MemberRole.MEMBER)
                )

                val response = given()
                    .header("Authorization", "Bearer ${generateToken(member.id)}")
                    .header("Host", "my-project.localhost.com")
                    .post("/v1/projects/member/${guest.id}")
                    .then()
                    .statusCode(HttpStatus.SC_FORBIDDEN)
                    .extract()
                    .`as`(ErrorResponse::class.java)

                assertEquals("insufficient permission", response.message)
            }
        }

        @Nested
        inner class HappyPath {

            @ParameterizedTest
            @EnumSource(MemberRole::class, names = ["OWNER", "ADMIN"])
            fun `given a valid requester when creating membership then return 204 and persist membership`(role: MemberRole) {
                val requester = userRepository.save(User(email = "requester@example.com", passwordHash = "hash", name = "Requester"))
                val guest = userRepository.save(User(email = "guest@example.com", passwordHash = "hash", name = "Guest"))
                val project = projectRepository.save(Project(name = "My Project", createdBy = requester.id))
                membershipRepository.save(
                    ProjectMembership(userId = requester.id, projectId = project.id, role = role)
                )

                given()
                    .header("Authorization", "Bearer ${generateToken(requester.id)}")
                    .header("Host", "my-project.localhost.com")
                    .post("/v1/projects/member/${guest.id}")
                    .then()
                    .statusCode(HttpStatus.SC_CREATED)

                val guestMembership = membershipRepository.findByUserIdAndProjectId(guest.id, project.id)
                assertEquals(guest.id, guestMembership?.userId)
                assertEquals(project.id, guestMembership?.projectId)
                assertEquals(MemberRole.MEMBER, guestMembership?.role)
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
                    .header("Host", "my-project.localhost.com")
                    .put("/v1/projects/git?access_token=some-token")
                    .then()
                    .statusCode(HttpStatus.SC_UNAUTHORIZED)
            }
        }

        @Nested
        inner class RequestValidation {

            @Test
            fun `given a request without tenant when creating git account then return 400 bad request`() {
                val owner = userRepository.save(User(email = "owner@example.com", passwordHash = "hash", name = "Owner"))

                given()
                    .header("Authorization", "Bearer ${generateToken(owner.id)}")
                    .put("/v1/projects/git?access_token=some-token")
                    .then()
                    .statusCode(HttpStatus.SC_BAD_REQUEST)
            }
        }

        @Nested
        inner class BusinessRules {

            @Test
            fun `given a non-existent project when creating git account then return 404 not found`() {
                val owner = userRepository.save(User(email = "owner@example.com", passwordHash = "hash", name = "Owner"))

                val response = given()
                    .header("Authorization", "Bearer ${generateToken(owner.id)}")
                    .header("Host", "unknown-project.localhost.com")
                    .put("/v1/projects/git?access_token=some-token")
                    .then()
                    .statusCode(HttpStatus.SC_NOT_FOUND)
                    .extract()
                    .`as`(ErrorResponse::class.java)

                assertEquals("project not found", response.message)
            }

            @Test
            fun `given a requester without membership when creating git account then return 404 not found`() {
                val owner = userRepository.save(User(email = "owner@example.com", passwordHash = "hash", name = "Owner"))
                val requester = userRepository.save(User(email = "requester@example.com", passwordHash = "hash", name = "Requester"))
                projectRepository.save(Project(name = "My Project", createdBy = owner.id))

                val response = given()
                    .header("Authorization", "Bearer ${generateToken(requester.id)}")
                    .header("Host", "my-project.localhost.com")
                    .put("/v1/projects/git?access_token=some-token")
                    .then()
                    .statusCode(HttpStatus.SC_NOT_FOUND)
                    .extract()
                    .`as`(ErrorResponse::class.java)

                assertEquals("membership not found", response.message)
            }

            @Test
            fun `given a requester with member role when creating git account then return 403 forbidden`() {
                val owner = userRepository.save(User(email = "owner@example.com", passwordHash = "hash", name = "Owner"))
                val member = userRepository.save(User(email = "member@example.com", passwordHash = "hash", name = "Member"))
                val project = projectRepository.save(Project(name = "My Project", createdBy = owner.id))
                membershipRepository.save(
                    ProjectMembership(userId = member.id, projectId = project.id, role = MemberRole.MEMBER)
                )

                val response = given()
                    .header("Authorization", "Bearer ${generateToken(member.id)}")
                    .header("Host", "my-project.localhost.com")
                    .put("/v1/projects/git?access_token=some-token")
                    .then()
                    .statusCode(HttpStatus.SC_FORBIDDEN)
                    .extract()
                    .`as`(ErrorResponse::class.java)

                assertEquals("insufficient permission", response.message)
            }

            @Test
            fun `given an invalid github access token when creating git account then return 401 unauthorized`() {
                val owner = userRepository.save(User(email = "owner@example.com", passwordHash = "hash", name = "Owner"))
                val project = projectRepository.save(Project(name = "My Project", createdBy = owner.id))
                membershipRepository.save(
                    ProjectMembership(userId = owner.id, projectId = project.id, role = MemberRole.OWNER)
                )
                GithubHelper.mockGetUserUnauthorized()

                val response = given()
                    .header("Authorization", "Bearer ${generateToken(owner.id)}")
                    .header("Host", "my-project.localhost.com")
                    .put("/v1/projects/git?access_token=invalid-token")
                    .then()
                    .statusCode(HttpStatus.SC_UNAUTHORIZED)
                    .extract()
                    .`as`(ErrorResponse::class.java)

                assertEquals("invalid git access token", response.message)
            }
        }

        @Nested
        inner class HappyPath {

            @ParameterizedTest
            @EnumSource(MemberRole::class, names = ["OWNER", "ADMIN"])
            fun `given a valid github access token when creating git account then return 204 and persist git account`(role: MemberRole) {
                val user = userRepository.save(User(email = "user@example.com", passwordHash = "hash", name = "User"))
                val project = projectRepository.save(Project(name = "My Project", createdBy = user.id))
                membershipRepository.save(
                    ProjectMembership(userId = user.id, projectId = project.id, role = role)
                )
                val githubUser = GithubUserResponse(login = "octocat", htmlUrl = "https://github.com/octocat")
                GithubHelper.mockGetUserSuccessfully(githubUser)

                given()
                    .header("Authorization", "Bearer ${generateToken(user.id)}")
                    .header("Host", "my-project.localhost.com")
                    .put("/v1/projects/git?access_token=valid-token")
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
