package com.kaiqkt.magiapi.domain.models

import com.kaiqkt.magiapi.domain.models.enums.AccountType
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import com.github.f4b6a3.ulid.UlidCreator
import java.time.LocalDateTime

@Entity
@Table(name = "git_accounts")
class GitAccount(
    @Id
    val id: String = UlidCreator.getMonotonicUlid().toString(),
    val projectId: String = "",
    @Enumerated(EnumType.STRING)
    val accountType: AccountType = AccountType.USER,
    var username: String = "",
    var profileUrl: String = "",
    var accessToken: String = "",
    val createdAt: LocalDateTime = LocalDateTime.now(),
)
