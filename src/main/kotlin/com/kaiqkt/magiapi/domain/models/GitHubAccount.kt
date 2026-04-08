package com.kaiqkt.magiapi.domain.models

import com.kaiqkt.magiapi.domain.models.enums.AccountType
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "github_accounts")
class GitHubAccount(
    @Id
    val id: String = UUID.randomUUID().toString(),
    val projectId: String = "",
    @Enumerated(EnumType.STRING)
    val accountType: AccountType = AccountType.USER,
    val username: String = "",
    val profileUrl: String = "",
    val accessToken: String = "",
    val createdAt: LocalDateTime = LocalDateTime.now(),
)
