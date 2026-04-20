package com.kaiqkt.magiapi.resources.github.clients

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.extensions.cUrlString
import com.github.kittinunf.fuel.core.isSuccessful
import com.kaiqkt.magiapi.resources.exceptions.UnexpectedResourceException
import com.kaiqkt.magiapi.resources.github.requests.GithubContentRequest
import com.kaiqkt.magiapi.resources.github.requests.GithubRepositoryRequest
import com.kaiqkt.magiapi.resources.github.responses.GithubContentResponse
import com.kaiqkt.magiapi.resources.github.responses.GithubRepositoryResponse
import com.kaiqkt.magiapi.resources.github.responses.GithubUserResponse
import com.kaiqkt.magiapi.utils.MetricsUtils
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Component

@Component
class GithubClient(
    private val metricsUtils: MetricsUtils,
    private val mapper: ObjectMapper,
    @Value($$"${github.url}")
    private val apiUrl: String,
    @Value($$"${github.content-path}")
    private val contentPath: String
) {
    fun getUser(accessToken: String): GithubUserResponse? {
        val (_, response, result) =
            metricsUtils.request(GITHUB_GET_USER) {
                Fuel
                    .get("$apiUrl/user")
                    .header(
                        mapOf(
                            "Accept" to "application/vnd.github+json",
                            "X-GitHub-Api-Version" to "2026-03-10",
                            HttpHeaders.AUTHORIZATION to "Bearer $accessToken",
                        ),
                    )
                    .response()
            }

        println( "stats: ${response.statusCode}")

        return when {
            response.isSuccessful -> mapper.readValue(result.get(), GithubUserResponse::class.java)
            response.statusCode == 401 || response.statusCode == 403 -> null
            else -> throw UnexpectedResourceException("Fail to get user ${response.responseMessage}")
        }
    }

    fun createRepository(request: GithubRepositoryRequest, accessToken: String): GithubRepositoryResponse? {
        val (_, response, result) =
            metricsUtils.request(GITHUB_CREATE_REPO) {
                Fuel
                    .post("$apiUrl/user/repos")
                    .header(
                        mapOf(
                            "Content-Type" to MediaType.APPLICATION_JSON,
                            "Accept" to "application/vnd.github+json",
                            "X-GitHub-Api-Version" to "2026-03-10",
                            "Authorization" to "Bearer $accessToken",
                        ),
                    ).body(mapper.writeValueAsString(request))
                    .response()
            }

        return when {
            response.isSuccessful -> mapper.readValue(result.get(), GithubRepositoryResponse::class.java)

            response.statusCode == 401 || response.statusCode == 403 -> null

            else -> throw UnexpectedResourceException("Fail to create github repository ${response.responseMessage}")
        }
    }

    fun uploadContent(
        owner: String,
        repo: String,
        accessToken: String,
        request: GithubContentRequest
    ): GithubContentResponse? {
        val (_, response, _) =
            metricsUtils.request(GITHUB_CREATE_REPO) {
                Fuel
                    .put("$apiUrl/repos/$owner/$repo/contents/$contentPath")
                    .header(
                        mapOf(
                            "Accept" to "application/vnd.github+json",
                            "X-GitHub-Api-Version" to "2026-03-10",
                            "Content-Type" to MediaType.APPLICATION_JSON,
                            "Authorization" to "Bearer $accessToken",
                        ),
                    ).body(mapper.writeValueAsString(request))
                    .response()
            }

        return when {
            response.isSuccessful -> GithubContentResponse.Success

            response.statusCode == 401 || response.statusCode == 403 -> null

            else -> throw UnexpectedResourceException("Fail to upload content to github repository ${response.responseMessage}")
        }
    }

    companion object {
        private const val GITHUB_GET_USER = "github_get_user"
        private const val GITHUB_CREATE_REPO = "github_create_repository"
    }
}