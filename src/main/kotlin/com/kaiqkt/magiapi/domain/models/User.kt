package com.kaiqkt.magiapi.domain.models

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "users")
class User(
    @Id
    val id: String = UUID.randomUUID().toString(),
    val email: String = "",
    val passwordHash: String = "",
    val name: String = "",
    val nickname: String = "",
    val createdAt: LocalDateTime = LocalDateTime.now(),
)
