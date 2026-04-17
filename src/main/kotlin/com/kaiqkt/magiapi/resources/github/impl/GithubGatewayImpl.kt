package com.kaiqkt.magiapi.resources.github.impl

import com.kaiqkt.magiapi.domain.dtos.GitUserDto
import com.kaiqkt.magiapi.domain.gateways.GitGateway
import com.kaiqkt.magiapi.resources.github.clients.GithubClient
import com.kaiqkt.magiapi.resources.github.requests.GithubRepositoryRequest
import org.springframework.stereotype.Component

@Component
class GithubGatewayImpl(
    private val githubClient: GithubClient,
): GitGateway {
    override fun findUser(accessToken: String): GitUserDto? {
        val response = githubClient.getUser(accessToken) ?: return null

        return GitUserDto(
            username = response.login,
            profileUrl = response.htmlUrl
        )
    }

    override fun createRepository(name: String, accessToken: String): String {
        val request = GithubRepositoryRequest(name = name)
        val response = githubClient.createRepository(request, accessToken)

        return response.htmlUrl
    }
}