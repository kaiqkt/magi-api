package com.kaiqkt.magiapi.domain.repositories

import com.kaiqkt.magiapi.domain.models.ProjectMembership
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ProjectMemberShipRepository : JpaRepository<ProjectMembership, String> {
    fun findByUserIdAndProjectId(
        userId: String,
        projectId: String
    ): ProjectMembership?
}