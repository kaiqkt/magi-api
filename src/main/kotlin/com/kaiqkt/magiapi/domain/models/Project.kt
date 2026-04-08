package com.kaiqkt.magiapi.domain.models

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "projects")
class Project(
    @Id
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val slug: String = "",
    val createdBy: String = "",
    val createdAt: LocalDateTime = LocalDateTime.now(),
)
