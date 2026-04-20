package com.kaiqkt.magiapi.domain.models

import com.github.f4b6a3.ulid.UlidCreator
import com.kaiqkt.magiapi.domain.models.enums.MemberRole
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "project_memberships")
class ProjectMembership(
    @Id
    val id: String = UlidCreator.getMonotonicUlid().toString(),
    val userId: String = "",
    val projectId: String = "",
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val role: MemberRole = MemberRole.MEMBER,
    val createdAt: LocalDateTime = LocalDateTime.now(),
)