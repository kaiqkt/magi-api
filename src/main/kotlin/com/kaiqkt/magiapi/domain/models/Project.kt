package com.kaiqkt.magiapi.domain.models

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import com.github.f4b6a3.ulid.UlidCreator
import java.time.LocalDateTime

@Entity
@Table(name = "projects")
class Project(
    @Id
    val id: String = UlidCreator.getMonotonicUlid().toString(),
    val name: String = "",
    val createdBy: String = "",
    val createdAt: LocalDateTime = LocalDateTime.now(),
) {
    val tenantId: String = name.replace(" ", "-").lowercase()
}
