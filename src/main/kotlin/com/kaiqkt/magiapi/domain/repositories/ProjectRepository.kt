package com.kaiqkt.magiapi.domain.repositories

import com.kaiqkt.magiapi.domain.models.Project
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ProjectRepository : JpaRepository<Project, String> {
    fun findByIdAndUserId(id: String, userId: String): Project?
    fun existsByName(name: String): Boolean
}