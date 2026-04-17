package com.kaiqkt.magiapi.domain.services

import com.kaiqkt.magiapi.domain.exceptions.DomainException
import com.kaiqkt.magiapi.domain.exceptions.ErrorType
import com.kaiqkt.magiapi.domain.gateways.GitGateway
import com.kaiqkt.magiapi.domain.models.GitAccount
import com.kaiqkt.magiapi.domain.repositories.GitAccountRepository
import com.kaiqkt.magiapi.utils.MetricsUtils
import com.kaiqkt.magiapi.utils.MetricsUtils.Companion.CREATED
import com.kaiqkt.magiapi.utils.MetricsUtils.Companion.STATUS
import org.springframework.stereotype.Service

@Service
class GitService(
    private val gitGateway: GitGateway,
    private val gitAccountRepository: GitAccountRepository,
    private val metrics: MetricsUtils
) {

    fun createAccount(accessToken: String, projectId: String) {
        val user = gitGateway.findUser(accessToken)

        if (user == null) {
            metrics.counter(GIT_ACCOUNT, STATUS, "git_user_not_found")
            throw DomainException(ErrorType.INVALID_GIT_ACCESS_TOKEN)
        }

        val account = gitAccountRepository.findByProjectId(projectId) ?: GitAccount(
            projectId = projectId,
            username = user.username,
            profileUrl = user.profileUrl,
            accessToken = accessToken
        )

        gitAccountRepository.save(account)

        metrics.counter(GIT_ACCOUNT, STATUS, CREATED)
    }

    fun createRepository(name: String, projectId: String): String{
        val account = gitAccountRepository.findByProjectId(projectId)

        if (account == null){
            metrics.counter(GIT_REPOSITORY, STATUS, "git_account_not_found")
            throw DomainException(ErrorType.GIT_ACCOUNT_NOT_FOUND)
        }

        val repositoryUrl = gitGateway.createRepository(name, account.accessToken)
        metrics.counter(GIT_REPOSITORY, STATUS, CREATED)

        return repositoryUrl
    }

    companion object {
        private const val GIT_ACCOUNT = "git_account"
        private const val GIT_REPOSITORY = "git_repository"
    }
}