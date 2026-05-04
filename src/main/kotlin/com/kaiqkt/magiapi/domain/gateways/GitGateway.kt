package com.kaiqkt.magiapi.domain.gateways

import com.kaiqkt.magiapi.domain.dtos.GitDto
import com.kaiqkt.magiapi.domain.dtos.GitUserDto
import com.kaiqkt.magiapi.domain.result.GitContentResult

interface GitGateway {
    fun getUser(accessToken: String): GitUserDto?
    fun createRepository(name: String, accessToken: String): String?
    fun uploadContent(
        gitDto: GitDto,
        content: String,
        path: String
    ): GitContentResult

//    fun triggerWorkflow(gitDto: GitDto, reference: String): GitWorkflowResult
}

sealed class GitContentError {
    object InvalidAccessToken : GitContentError()
    object NotFound : GitContentError()

    data class Unknown(
        val cause: Throwable
    ) : GitContentError()
}

fun fetchContent(): Result<String> {
    return try {
        val content = "arquivo do git"
        Result.success(content)
    } catch (e: Exception) {
        Result.failure(GitContentException(GitContentError.Unknown(e)))
    }
}