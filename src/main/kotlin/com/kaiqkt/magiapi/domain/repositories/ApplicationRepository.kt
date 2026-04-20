package com.kaiqkt.magiapi.domain.repositories

import com.kaiqkt.magiapi.domain.models.Application
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ApplicationRepository: JpaRepository<Application, String> {
    fun existsByNameAndProjectId(name: String, projectId: String): Boolean
}