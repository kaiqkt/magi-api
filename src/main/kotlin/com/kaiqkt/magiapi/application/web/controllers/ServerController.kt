package com.kaiqkt.magiapi.application.web.controllers

import com.kaiqkt.magiapi.application.web.security.SecurityContext
import com.kaiqkt.magiapi.application.web.responses.ServerResponse
import com.kaiqkt.magiapi.application.web.responses.toResponse
import com.kaiqkt.magiapi.domain.models.enums.Environment
import com.kaiqkt.magiapi.domain.services.ServerService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
class ServerController(
    private val serverService: ServerService,
) {
    @PostMapping("/v1/projects/{project_id}/servers")
    fun create(
        @PathVariable("project_id") projectId: String,
        @RequestParam("env") environment: Environment
    ): ResponseEntity<ServerResponse> {
        val userId = SecurityContext.getUserId()

        val server = serverService.create(userId, projectId, environment)

        return ResponseEntity.ok(server.toResponse())
    }
}