package com.kaiqkt.magiapi.domain.models

import com.github.f4b6a3.ulid.UlidCreator
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "git_accounts")
class GitAccount(
    @Id
    val id: String = UlidCreator.getMonotonicUlid().toString(),
    val projectId: String = "",
    var username: String = "",
    var profileUrl: String = "",
    var accessToken: String = "",
    val createdAt: LocalDateTime = LocalDateTime.now(),
)
