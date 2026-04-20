package com.kaiqkt.magiapi.application.web.controllers

import com.kaiqkt.magiapi.application.security.SecurityContext
import com.kaiqkt.magiapi.application.web.interceptors.TenantContext
import com.kaiqkt.magiapi.application.web.responses.ServerResponse
import com.kaiqkt.magiapi.domain.models.enums.Environment
import com.kaiqkt.magiapi.domain.services.ServerService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
class ServerController(
    private val serverService: ServerService,
) {
    @PostMapping("/v1/servers")
    fun create(@RequestParam("env") environment: Environment): ResponseEntity<ServerResponse.Created> {
        val userId = SecurityContext.getUserId()
        val tenantId = TenantContext.getTenant()

        val server = serverService.create(userId, tenantId, environment)

        return ResponseEntity.ok(ServerResponse.Created(server.agentToken))
    }
}