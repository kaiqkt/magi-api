package com.kaiqkt.magiapi.domain.models

import com.github.f4b6a3.ulid.UlidCreator
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "applications")
class Application(
    @Id
    val id: String = UlidCreator.getMonotonicUlid().toString(),
    val name: String = "",
    val description: String? = null,
    val repositoryUrl: String = "",
    val projectId: String = "",
    val createdAt: LocalDateTime = LocalDateTime.now(),
)
