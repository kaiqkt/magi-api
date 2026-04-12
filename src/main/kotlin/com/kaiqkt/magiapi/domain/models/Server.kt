package com.kaiqkt.magiapi.domain.models

import com.kaiqkt.magiapi.domain.models.enums.Environment
import com.kaiqkt.magiapi.domain.models.enums.ServerStatus
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import com.github.f4b6a3.ulid.UlidCreator
import java.time.LocalDateTime

@Entity
@Table(name = "servers")
class Server(
    @Id
    val id: String = UlidCreator.getMonotonicUlid().toString(),
    val projectId: String = "",
    @Enumerated(EnumType.STRING)
    val environment: Environment = Environment.DEV,
    val agentToken: String = "",
    @Enumerated(EnumType.STRING)
    val status: ServerStatus = ServerStatus.INACTIVE,
    val lastSeenAt: LocalDateTime? = null,
    val createdAt: LocalDateTime = LocalDateTime.now(),
)
