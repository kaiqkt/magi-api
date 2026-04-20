package com.kaiqkt.magiapi.domain.services

import com.kaiqkt.magiapi.domain.dtos.GitContentDto
import com.kaiqkt.magiapi.domain.dtos.GitRepositoryDto
import com.kaiqkt.magiapi.domain.dtos.GitUserDto
import com.kaiqkt.magiapi.domain.exceptions.DomainException
import com.kaiqkt.magiapi.domain.exceptions.ErrorType
import com.kaiqkt.magiapi.domain.gateways.GitGateway
import com.kaiqkt.magiapi.domain.models.GitAccount
import com.kaiqkt.magiapi.domain.repositories.GitAccountRepository
import com.kaiqkt.magiapi.utils.MetricsUtils
import com.kaiqkt.magiapi.utils.MetricsUtils.Companion.CREATED
import com.kaiqkt.magiapi.utils.MetricsUtils.Companion.STATUS
import org.springframework.stereotype.Service
import java.util.Base64

@Service
class GitService(
    private val gitGateway: GitGateway,
    private val gitAccountRepository: GitAccountRepository,
    private val metrics: MetricsUtils
) {

    private val encodedCiContent: String = requireNotNull(this::class.java.getResourceAsStream("/github-actions/ci.yml"))
        .bufferedReader()
        .use { Base64.getEncoder().encodeToString(it.readText().toByteArray()) }

    fun createAccount(accessToken: String, projectId: String) {
        val userDto = gitGateway.findUser(accessToken)

        if (userDto is GitUserDto.InvalidAccessToken) {
            metrics.counter(GIT_ACCOUNT, STATUS, "invalid_access_token")
            throw DomainException(ErrorType.INVALID_GIT_ACCESS_TOKEN)
        }

        if (userDto is GitUserDto.Success) {
            val account = gitAccountRepository.findByProjectId(projectId) ?: GitAccount(
                projectId = projectId,
                username = userDto.username,
                profileUrl = userDto.profileUrl,
                //encryptar o access token
                accessToken = accessToken
            )

            gitAccountRepository.save(account)
            metrics.counter(GIT_ACCOUNT, STATUS, CREATED)
        }
    }

    fun createRepository(name: String, projectId: String): String {
        val account = findAccount(projectId)

        when (val response = gitGateway.createRepository(name, account.accessToken)) {
            is GitRepositoryDto.Created -> {
                metrics.counter(GIT_REPOSITORY, STATUS, CREATED)
                return response.repositoryUrl
            }

            is GitRepositoryDto.InvalidAccessToken -> {
                metrics.counter(GIT_REPOSITORY, STATUS, "invalid_access_token")
                throw DomainException(ErrorType.INVALID_GIT_ACCESS_TOKEN)
            }
        }
    }

    fun provisionCiWorkflow(repositoryName: String, projectId: String) {
        val account = findAccount(projectId)

        val gitContent = GitContentDto.Create(
            owner = account.username,
            repo = repositoryName,
            encodedContent = encodedCiContent,
            accessToken = account.accessToken
        )

        val response = gitGateway.uploadContent(gitContent)

        if (response is GitContentDto.InvalidAccessToken) {
            metrics.counter(GIT_CI_PROVISION, STATUS, "invalid_access_token")
            throw DomainException(ErrorType.INVALID_GIT_ACCESS_TOKEN)
        }
    }

    private fun findAccount(projectId: String): GitAccount =
        gitAccountRepository.findByProjectId(projectId) ?: throw DomainException(ErrorType.GIT_ACCOUNT_NOT_FOUND)

    companion object {
        private const val GIT_ACCOUNT = "git_account"
        private const val GIT_REPOSITORY = "git_repository"
        private const val GIT_CI_PROVISION = "git_ci_provision"
    }
}