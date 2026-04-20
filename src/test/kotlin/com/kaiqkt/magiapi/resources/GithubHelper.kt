package com.kaiqkt.magiapi.resources

import com.kaiqkt.magiapi.resources.github.responses.GithubRepositoryResponse
import com.kaiqkt.magiapi.resources.github.responses.GithubUserResponse
import com.kaiqkt.magiapi.resources.MockServerHolder
import org.mockserver.model.HttpRequest
import org.mockserver.model.HttpResponse
import org.mockserver.model.MediaType
import org.springframework.http.HttpStatus

object GithubHelper : MockServerHolder() {
    override fun domainPath(): String = "/github"

    fun mockGetUserSuccessfully(response: GithubUserResponse) {
        mockServer()
            .`when`(
                HttpRequest.request()
                    .withMethod("GET")
                    .withPath("/github/user"),
            )
            .respond(
                HttpResponse.response()
                    .withStatusCode(HttpStatus.OK.value())
                    .withContentType(MediaType.APPLICATION_JSON)
                    .withBody(objectMapper.writeValueAsString(response)),
            )
    }

    fun mockGetUserUnauthorized() {
        mockServer()
            .`when`(
                HttpRequest.request()
                    .withMethod("GET")
                    .withPath("/github/user"),
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
                    .withPath("/github/user/repos"),
            )
            .respond(
                HttpResponse.response()
                    .withStatusCode(HttpStatus.CREATED.value())
                    .withContentType(MediaType.APPLICATION_JSON)
                    .withBody(objectMapper.writeValueAsString(response)),
            )
    }

    fun mockUploadContentSuccessfully() {
        mockServer()
            .`when`(
                HttpRequest.request()
                    .withMethod("PUT")
                    .withPath("/github/repos/.*"),
            )
            .respond(
                HttpResponse.response()
                    .withStatusCode(HttpStatus.CREATED.value()),
            )
    }

    fun mockUploadContentUnauthorized() {
        mockServer()
            .`when`(
                HttpRequest.request()
                    .withMethod("PUT")
                    .withPath("/github/repos/.*"),
            )
            .respond(
                HttpResponse.response()
                    .withStatusCode(HttpStatus.UNAUTHORIZED.value()),
            )
    }
}