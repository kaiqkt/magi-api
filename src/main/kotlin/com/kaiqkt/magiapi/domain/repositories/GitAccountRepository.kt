package com.kaiqkt.magiapi.domain.repositories

import com.kaiqkt.magiapi.domain.models.GitAccount
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface GitAccountRepository: JpaRepository<GitAccount, String> {
    fun findByProjectId(projectId: String): GitAccount?
}