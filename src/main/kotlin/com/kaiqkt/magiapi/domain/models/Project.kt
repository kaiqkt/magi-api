package com.kaiqkt.magiapi.domain.models

import com.github.f4b6a3.ulid.UlidCreator
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "projects")
class Project(
    @Id
    val id: String = UlidCreator.getMonotonicUlid().toString(),
    val name: String = "",
    val userId: String = "",
    val createdAt: LocalDateTime = LocalDateTime.now(),
)