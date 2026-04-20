package com.kaiqkt.magiapi.application.web.responses

import com.kaiqkt.magiapi.domain.models.Server
import com.kaiqkt.magiapi.domain.models.enums.Environment
import com.kaiqkt.magiapi.domain.models.enums.ServerStatus
import java.time.LocalDateTime


data class ServerResponse(
    val id: String,
    val environment: Environment,
    val agentToken: String,
    val status: ServerStatus,
    val lastSeenAt: LocalDateTime?
)

fun Server.toResponse(): ServerResponse {
    return ServerResponse(
        id = id,
        environment = environment,
        status = status,
        agentToken = agentToken,
        lastSeenAt = lastSeenAt
    )
}