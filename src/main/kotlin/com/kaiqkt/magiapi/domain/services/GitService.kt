package com.kaiqkt.magiapi.domain.services

import com.kaiqkt.magiapi.domain.exceptions.DomainException
import com.kaiqkt.magiapi.domain.exceptions.ErrorType
import com.kaiqkt.magiapi.domain.gateways.GitGateway
import com.kaiqkt.magiapi.domain.models.GitAccount
import com.kaiqkt.magiapi.domain.repositories.GitAccountRepository
import org.springframework.stereotype.Service

@Service
class GitService(
    private val gitGateway: GitGateway,
    private val gitAccountRepository: GitAccountRepository
) {

    fun create(accessToken: String, projectId: String) {
        val user = gitGateway.findUser(accessToken)
            ?: throw DomainException(ErrorType.INVALID_GIT_ACCESS_TOKEN)

        val account = gitAccountRepository.findByProjectId(projectId) ?: GitAccount(
            projectId = projectId,
            username = user.username,
            profileUrl = user.profileUrl,
            accessToken = accessToken
        )

        gitAccountRepository.save(account)
    }
}