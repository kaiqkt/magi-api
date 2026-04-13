package com.kaiqkt.magiapi.application.web.controllers

import com.kaiqkt.magiapi.application.security.SecurityContext
import com.kaiqkt.magiapi.application.web.interceptors.TenantContext
import com.kaiqkt.magiapi.application.web.requests.ProjectRequest
import com.kaiqkt.magiapi.application.web.requests.toDomain
import com.kaiqkt.magiapi.domain.services.ProjectService
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController

@RestController
class ProjectController(
    private val projectService: ProjectService
) {
    @PostMapping("/v1/projects")
    fun create(@Valid @RequestBody request: ProjectRequest.Create): ResponseEntity<Unit> {
        val userId = SecurityContext.getUserId()
        projectService.create(request.toDomain(userId))

        return ResponseEntity.noContent().build()
    }

    @PostMapping("/v1/projects/invite/{user_id}")
    fun invite(@PathVariable("user_id") guestId: String): ResponseEntity<Unit> {
        val userId = SecurityContext.getUserId()
        val tenantId = TenantContext.getTenant() ?: return ResponseEntity.badRequest().build()

        projectService.invite(userId, tenantId, guestId)

        return ResponseEntity.noContent().build()
    }
}