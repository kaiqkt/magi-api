package com.kaiqkt.magiapi.integration

import com.kaiqkt.magiapi.application.web.requests.ProjectRequest
import com.kaiqkt.magiapi.application.web.responses.ErrorResponse
import com.kaiqkt.magiapi.domain.models.Project
import com.kaiqkt.magiapi.domain.models.ProjectMembership
import com.kaiqkt.magiapi.domain.models.User
import com.kaiqkt.magiapi.domain.models.enums.MemberRole
import com.kaiqkt.magiapi.domain.models.enums.MemberStatus
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
                val user = userRepository.save(User(email = "john@example.com", passwordHash = "hash", name = "John Doe"))

                val request = ProjectRequest.Create(name = "a".repeat(101))

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
                assertEquals("must not exceed 100 characters", response.details["name"])
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
                    .statusCode(HttpStatus.SC_NO_CONTENT)

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
                assertEquals(MemberStatus.ACTIVE, membership.status)
            }
        }
    }

    @Nested
    inner class ProjectInvitation {

        @Nested
        inner class Auth {

            @Test
            fun `given an unauthenticated request when inviting user then return 401 unauthorized`() {
                val guest = userRepository.save(User(email = "guest@example.com", passwordHash = "hash", name = "Guest"))

                given()
                    .header("Host", "my-project.localhost.com")
                    .post("/v1/projects/invite/${guest.id}")
                    .then()
                    .statusCode(HttpStatus.SC_UNAUTHORIZED)
            }
        }

        @Nested
        inner class RequestValidation {

            @Test
            fun `given a request without tenant when inviting user then return 400 bad request`() {
                val owner = userRepository.save(User(email = "owner@example.com", passwordHash = "hash", name = "Owner"))
                val guest = userRepository.save(User(email = "guest@example.com", passwordHash = "hash", name = "Guest"))

                given()
                    .header("Authorization", "Bearer ${generateToken(owner.id)}")
                    .post("/v1/projects/invite/${guest.id}")
                    .then()
                    .statusCode(HttpStatus.SC_BAD_REQUEST)
            }
        }

        @Nested
        inner class BusinessRules {

            @Test
            fun `given a non-existent project when inviting user then return 404 not found`() {
                val owner = userRepository.save(User(email = "owner@example.com", passwordHash = "hash", name = "Owner"))
                val guest = userRepository.save(User(email = "guest@example.com", passwordHash = "hash", name = "Guest"))

                val response = given()
                    .header("Authorization", "Bearer ${generateToken(owner.id)}")
                    .header("Host", "unknown-project.localhost.com")
                    .post("/v1/projects/invite/${guest.id}")
                    .then()
                    .statusCode(HttpStatus.SC_NOT_FOUND)
                    .extract()
                    .`as`(ErrorResponse::class.java)

                assertEquals("project not found", response.message)
            }

            @Test
            fun `given a requester without membership when inviting user then return 404 not found`() {
                val owner = userRepository.save(User(email = "owner@example.com", passwordHash = "hash", name = "Owner"))
                val requester = userRepository.save(User(email = "requester@example.com", passwordHash = "hash", name = "Requester"))
                val guest = userRepository.save(User(email = "guest@example.com", passwordHash = "hash", name = "Guest"))
                projectRepository.save(Project(name = "My Project", createdBy = owner.id))

                val response = given()
                    .header("Authorization", "Bearer ${generateToken(requester.id)}")
                    .header("Host", "my-project.localhost.com")
                    .post("/v1/projects/invite/${guest.id}")
                    .then()
                    .statusCode(HttpStatus.SC_NOT_FOUND)
                    .extract()
                    .`as`(ErrorResponse::class.java)

                assertEquals("user not found", response.message)
            }

            @Test
            fun `given a requester with member role when inviting user then return 403 forbidden`() {
                val owner = userRepository.save(User(email = "owner@example.com", passwordHash = "hash", name = "Owner"))
                val member = userRepository.save(User(email = "member@example.com", passwordHash = "hash", name = "Member"))
                val guest = userRepository.save(User(email = "guest@example.com", passwordHash = "hash", name = "Guest"))
                val project = projectRepository.save(Project(name = "My Project", createdBy = owner.id))
                membershipRepository.save(
                    ProjectMembership(userId = member.id, projectId = project.id, role = MemberRole.MEMBER, status = MemberStatus.ACTIVE)
                )

                val response = given()
                    .header("Authorization", "Bearer ${generateToken(member.id)}")
                    .header("Host", "my-project.localhost.com")
                    .post("/v1/projects/invite/${guest.id}")
                    .then()
                    .statusCode(HttpStatus.SC_FORBIDDEN)
                    .extract()
                    .`as`(ErrorResponse::class.java)

                assertEquals("insufficient permission", response.message)
            }
        }

        @Nested
        inner class HappyPath {

            @Test
            fun `given a requester with owner role when inviting user then return 204 and persist membership`() {
                val owner = userRepository.save(User(email = "owner@example.com", passwordHash = "hash", name = "Owner"))
                val guest = userRepository.save(User(email = "guest@example.com", passwordHash = "hash", name = "Guest"))
                val project = projectRepository.save(Project(name = "My Project", createdBy = owner.id))
                membershipRepository.save(
                    ProjectMembership(userId = owner.id, projectId = project.id, role = MemberRole.OWNER, status = MemberStatus.ACTIVE)
                )

                given()
                    .header("Authorization", "Bearer ${generateToken(owner.id)}")
                    .header("Host", "my-project.localhost.com")
                    .post("/v1/projects/invite/${guest.id}")
                    .then()
                    .statusCode(HttpStatus.SC_NO_CONTENT)

                val guestMembership = membershipRepository.findByUserIdAndProjectId(guest.id, project.id)
                assertEquals(guest.id, guestMembership?.userId)
                assertEquals(project.id, guestMembership?.projectId)
                assertEquals(MemberRole.MEMBER, guestMembership?.role)
                assertEquals(MemberStatus.ACTIVE, guestMembership?.status)
            }
        }
    }
}
