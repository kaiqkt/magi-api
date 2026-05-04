package com.kaiqkt.magiapi.domain.services

import com.kaiqkt.magiapi.domain.dtos.GitDto
import com.kaiqkt.magiapi.domain.exceptions.DomainException
import com.kaiqkt.magiapi.domain.exceptions.ErrorType
import com.kaiqkt.magiapi.domain.gateways.GitGateway
import com.kaiqkt.magiapi.domain.models.GitAccount
import com.kaiqkt.magiapi.domain.repositories.GitAccountRepository
import com.kaiqkt.magiapi.domain.result.GitContentResult
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
    fun createAccount(accessToken: String, projectId: String) {
        val userDto = gitGateway.getUser(accessToken) ?: run {
            metrics.counter(GIT_ACCOUNT, STATUS, "invalid_access_token")
            throw DomainException(ErrorType.INVALID_GIT_ACCESS_TOKEN)
        }

        val account = gitAccountRepository.findByProjectId(projectId)?.apply {
            this.username = userDto.username
            this.profileUrl = userDto.profileUrl
            this.accessToken = accessToken
        } ?: GitAccount(
            projectId = projectId,
            username = userDto.username,
            profileUrl = userDto.profileUrl,
            //encryptar o access token
            accessToken = accessToken
        )

        gitAccountRepository.save(account)
        metrics.counter(GIT_ACCOUNT, STATUS, CREATED)
    }

    fun createRepository(name: String, projectId: String): String {
        val account = findAccount(projectId)
        val repositoryUrl = gitGateway.createRepository(name, account.accessToken) ?: run {
            metrics.counter(GIT_REPOSITORY, STATUS, "invalid_access_token")
            throw DomainException(ErrorType.INVALID_GIT_ACCESS_TOKEN)
        }

        metrics.counter(GIT_REPOSITORY, STATUS, CREATED)
        return repositoryUrl
    }

    fun uploadContent(
        repository: String,
        projectId: String,
        content: String,
        path: String
    ): GitContentResult {
        val account = findAccount(projectId)

        val gitDto = GitDto(
            owner = account.username,
            repository = repository,
            accessToken = account.accessToken
        )

        val response = gitGateway.uploadContent(
            gitDto = gitDto,
            path = path,
            content = Base64.getEncoder().encodeToString(content.toByteArray()),
        )

        if (response is GitContentResult.InvalidAccessToken) {
            metrics.counter(GIT_CI_PROVISION, STATUS, "invalid_access_token")
            throw DomainException(ErrorType.INVALID_GIT_ACCESS_TOKEN)
        }

        metrics.counter(GIT_CI_PROVISION, STATUS, CREATED)
    }

    //trigger
        //buscar conta
        //enviar
        //retornar o id e a url

    fun findAccount(projectId: String): GitAccount =
        gitAccountRepository.findByProjectId(projectId) ?: throw DomainException(ErrorType.GIT_ACCOUNT_NOT_FOUND)

    companion object {
        private const val GIT_ACCOUNT = "git_account"
        private const val GIT_REPOSITORY = "git_repository"
        private const val GIT_CI_PROVISION = "git_ci_provision"
    }
}