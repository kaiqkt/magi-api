package com.kaiqkt.magiapi.resources.github.clients

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.isSuccessful
import com.kaiqkt.magiapi.resources.exceptions.UnexpectedResourceException
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
    private val apiUrl: String
) {
    fun getUser(accessToken: String): GithubUserResponse? {
        val (_, response, result) =
            metricsUtils.request("github_auth") {
                Fuel
                    .post("$apiUrl/user")
                    .header(
                        mapOf(
                            "Accept" to MediaType.APPLICATION_JSON,
                            "X-GitHub-Api-Version" to "2022-11-28",
                            HttpHeaders.AUTHORIZATION to "Bearer $accessToken",
                        ),
                    )
                    .response()
            }

        return when {
            response.isSuccessful -> mapper.readValue(result.get(), GithubUserResponse::class.java)
            response.statusCode == 401 || response.statusCode == 403 -> null
            else -> throw UnexpectedResourceException("Fail to get user ${response.responseMessage}")
        }
    }
}