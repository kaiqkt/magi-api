package com.kaiqkt.magiapi.domain.models

import com.kaiqkt.magiapi.domain.models.enums.Role
import jakarta.persistence.CollectionTable
import jakarta.persistence.ElementCollection
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.Table
import com.github.f4b6a3.ulid.UlidCreator
import java.time.LocalDateTime

@Entity
@Table(name = "users")
class User(
    @Id
    val id: String = UlidCreator.getMonotonicUlid().toString(),
    val email: String = "",
    val passwordHash: String = "",
    val name: String = "",
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_roles", joinColumns = [JoinColumn(name = "user_id")])
    @Enumerated(EnumType.STRING)
    val roles: Set<Role> = setOf(Role.USER),
    val createdAt: LocalDateTime = LocalDateTime.now(),
)
