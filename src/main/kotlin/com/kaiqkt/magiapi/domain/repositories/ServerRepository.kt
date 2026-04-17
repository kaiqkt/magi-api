package com.kaiqkt.magiapi.domain.repositories

import com.kaiqkt.magiapi.domain.models.Server
import com.kaiqkt.magiapi.domain.models.enums.Environment
import com.kaiqkt.magiapi.domain.models.enums.ServerStatus
import jakarta.transaction.Transactional
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface ServerRepository: JpaRepository<Server, String> {
    fun existsByProjectIdAndEnvironment(projectId: String, environment: Environment): Boolean
    fun findByAgentToken(agentToken: String): Server?

    @Transactional
    @Modifying
    @Query("UPDATE Server s SET s.status = :status, s.lastSeenAt = CURRENT_TIMESTAMP WHERE s.id = :id")
    fun updateStatus(id: String, status: ServerStatus)

}