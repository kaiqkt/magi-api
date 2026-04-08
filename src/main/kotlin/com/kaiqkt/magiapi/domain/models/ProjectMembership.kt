package com.kaiqkt.magiapi.domain.models

import com.kaiqkt.magiapi.domain.models.enums.MemberRole
import com.kaiqkt.magiapi.domain.models.enums.MemberStatus
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "project_memberships")
class ProjectMembership(
    @Id
    val id: String = UUID.randomUUID().toString(),
    val userId: String = "",
    val projectId: String = "",
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val role: MemberRole = MemberRole.MEMBER,
    @Enumerated(EnumType.STRING)
    val status: MemberStatus = MemberStatus.INVITED,
    val createdAt: LocalDateTime = LocalDateTime.now(),
)
