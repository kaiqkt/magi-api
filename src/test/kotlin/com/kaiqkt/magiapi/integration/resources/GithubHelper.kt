package com.kaiqkt.magiapi.integration.resources

import com.kaiqkt.magiapi.resources.github.responses.GithubRepositoryResponse
import com.kaiqkt.magiapi.resources.github.responses.GithubUserResponse
import com.kaiqkt.magiapi.unit.resources.MockServerHolder
import org.mockserver.model.HttpRequest
import org.mockserver.model.HttpResponse
import org.springframework.http.HttpStatus

object GithubHelper : MockServerHolder() {
    override fun domainPath(): String = "/user"

    fun mockGetUserSuccessfully(response: GithubUserResponse) {
        mockServer()
            .`when`(
                HttpRequest.request()
                    .withMethod("POST")
                    .withPath("/user"),
            )
            .respond(
                HttpResponse.response()
                    .withStatusCode(HttpStatus.OK.value())
                    .withContentType(org.mockserver.model.MediaType.APPLICATION_JSON)
                    .withBody(objectMapper.writeValueAsString(response)),
            )
    }

    fun mockGetUserUnauthorized() {
        mockServer()
            .`when`(
                HttpRequest.request()
                    .withMethod("POST")
                    .withPath("/user"),
            )
            .respond(
                HttpResponse.response()
                    .withStatusCode(HttpStatus.UNAUTHORIZED.value()),
            )
    }

    fun mockCreateRepositorySuccessfully(response: GithubRepositoryResponse) {
        mockServer()
            .`when`(
                HttpRequest.request()
                    .withMethod("POST")
                    .withPath("/user/repos"),
            )
            .respond(
                HttpResponse.response()
                    .withStatusCode(HttpStatus.CREATED.value())
                    .withContentType(org.mockserver.model.MediaType.APPLICATION_JSON)
                    .withBody(objectMapper.writeValueAsString(response)),
            )
    }
}
