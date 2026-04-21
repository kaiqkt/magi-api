package com.kaiqkt.magiapi.domain.services

import com.kaiqkt.magiapi.domain.exceptions.DomainException
import com.kaiqkt.magiapi.domain.exceptions.ErrorType
import com.kaiqkt.magiapi.domain.models.Server
import com.kaiqkt.magiapi.domain.models.enums.Environment
import com.kaiqkt.magiapi.domain.models.enums.ServerStatus
import com.kaiqkt.magiapi.domain.repositories.ServerRepository
import com.kaiqkt.magiapi.utils.MetricsUtils
import com.kaiqkt.magiapi.utils.MetricsUtils.Companion.CREATED
import com.kaiqkt.magiapi.utils.MetricsUtils.Companion.STATUS
import jakarta.transaction.Transactional
import org.springframework.stereotype.Service

@Service
class ServerService(
    private val projectService: ProjectService,
    private val serverRepository: ServerRepository,
    private val metricsUtils: MetricsUtils,
    private val tokenService: TokenService
) {
    fun create(userId: String, projectId: String, environment: Environment): Server {
        val project = projectService.findByIdAndUserId(projectId, userId)

        if (serverRepository.existsByProjectIdAndEnvironment(project.id, environment)) {
            throw DomainException(ErrorType.SERVER_ALREADY_EXISTS)
        }

        val server = Server(
            projectId = project.id,
            environment = environment,
            agentToken = tokenService.opaqueToken()
        )

        serverRepository.save(server)

        metricsUtils.counter(SERVER, STATUS, CREATED, "environment", environment.name)

        return server
    }

    fun findByAgentToken(agentToken: String): Server? {
        return serverRepository.findByAgentToken(agentToken)
    }

    @Transactional
    fun activate(serverId: String) {
        serverRepository.updateStatus(serverId, ServerStatus.ACTIVE)
        metricsUtils.counter(SERVER_CONNECTION, STATUS, "active")
    }

    @Transactional
    fun deactivate(serverId: String) {
        serverRepository.updateStatus(serverId, ServerStatus.INACTIVE)
        metricsUtils.counter(SERVER_CONNECTION, STATUS, "inactive")
    }

    companion object {
        private const val SERVER = "server"
        private const val SERVER_CONNECTION = "server_connection"
    }
}