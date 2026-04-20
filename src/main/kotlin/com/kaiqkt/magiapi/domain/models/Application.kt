package com.kaiqkt.magiapi.domain.models

import com.github.f4b6a3.ulid.UlidCreator
import com.kaiqkt.magiapi.domain.models.enums.ApplicationStatus
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
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
    val repositoryUrl: String? = null,
    @Enumerated(EnumType.STRING)
    var status: ApplicationStatus = ApplicationStatus.PENDING_CI_PROVISIONING,
    val projectId: String = "",
    val createdAt: LocalDateTime = LocalDateTime.now(),
)
