package com.kaiqkt.magiapi.resources.github.impl

import com.kaiqkt.magiapi.domain.dtos.GitContentDto
import com.kaiqkt.magiapi.domain.dtos.GitRepositoryDto
import com.kaiqkt.magiapi.domain.dtos.GitUserDto
import com.kaiqkt.magiapi.domain.gateways.GitGateway
import com.kaiqkt.magiapi.resources.github.clients.GithubClient
import com.kaiqkt.magiapi.resources.github.requests.GithubContentRequest
import com.kaiqkt.magiapi.resources.github.requests.GithubRepositoryRequest
import org.springframework.stereotype.Component

@Component
class GithubGatewayImpl(
    private val githubClient: GithubClient,
) : GitGateway {
    override fun findUser(accessToken: String): GitUserDto {
        val response = githubClient.getUser(accessToken) ?: return GitUserDto.InvalidAccessToken

        return GitUserDto.Success(
            username = response.login,
            profileUrl = response.htmlUrl
        )
    }

    override fun createRepository(name: String, accessToken: String): GitRepositoryDto {
        val request = GithubRepositoryRequest(name = name)
        val response = githubClient.createRepository(request, accessToken) ?: return GitRepositoryDto.InvalidAccessToken

        return GitRepositoryDto.Created(response.htmlUrl)
    }

    override fun uploadContent(content: GitContentDto.Create): GitContentDto {
        val request = GithubContentRequest(
            message = "Magi upload content",
            content = content.encodedContent,
        )

        githubClient.uploadContent(content.owner, content.repo, content.accessToken, request)
            ?: return GitContentDto.InvalidAccessToken

        return GitContentDto.Success
    }

}