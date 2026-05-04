package com.kaiqkt.magiapi.resources.github.impl

import com.kaiqkt.magiapi.domain.dtos.GitDto
import com.kaiqkt.magiapi.domain.dtos.GitUserDto
import com.kaiqkt.magiapi.domain.gateways.GitGateway
import com.kaiqkt.magiapi.domain.result.GitContentResult
import com.kaiqkt.magiapi.resources.github.clients.GithubClient
import com.kaiqkt.magiapi.resources.github.requests.GithubContentRequest
import com.kaiqkt.magiapi.resources.github.requests.GithubRepositoryRequest
import org.springframework.stereotype.Component

@Component
class GithubGatewayImpl(
    private val githubClient: GithubClient,
) : GitGateway {
    override fun getUser(accessToken: String): GitUserDto? {
        val response = githubClient.getUser(accessToken) ?: return null

        return GitUserDto(
            username = response.login,
            profileUrl = response.htmlUrl
        )
    }

    override fun createRepository(name: String, accessToken: String): String? {
        val request = GithubRepositoryRequest(name = name)
        val response = githubClient.createRepository(request, accessToken) ?: return null

        return response.htmlUrl
    }

    override fun uploadContent(
        gitDto: GitDto,
        content: String,
        path: String
    ): GitContentResult {
        val request = GithubContentRequest(
            message = UPLOAD_CONTENT_MESSAGE,
            content = content,
        )

        githubClient.uploadContent(gitDto.owner, gitDto.repository, path, gitDto.accessToken, request)
            ?: return GitContentResult.InvalidAccessToken

        return GitContentResult.Success
    }

//    override fun triggerWorkflow(gitDto: GitDto, reference: String): GitWorkflowResult {
//        val response = githubClient.triggerWorkflow(gitDto.owner, gitDto.repository, reference, gitDto.accessToken)
//
//        return when (response) {
//            is GithubWorkflowResponse.Success -> Success(response.workflowRunId, response.runUrl)
//            is GithubWorkflowResponse.InvalidAccessToken -> InvalidAccessToken
//            is GithubWorkflowResponse.ReferenceNotFound -> ReferenceNotFound
//        }
//    }

    companion object {
        private const val UPLOAD_CONTENT_MESSAGE = "Magi upload content"
    }
}