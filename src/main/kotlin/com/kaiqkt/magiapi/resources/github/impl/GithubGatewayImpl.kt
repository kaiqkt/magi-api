package com.kaiqkt.magiapi.resources.github.impl

import com.kaiqkt.magiapi.domain.dtos.GitUserDto
import com.kaiqkt.magiapi.domain.gateways.GitGateway
import com.kaiqkt.magiapi.resources.github.clients.GithubClient
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
}