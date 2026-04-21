package com.kaiqkt.magiapi.application.web.controllers

import com.kaiqkt.magiapi.application.web.security.SecurityContext
import com.kaiqkt.magiapi.application.web.requests.ApplicationRequest
import com.kaiqkt.magiapi.application.web.requests.toDomain
import com.kaiqkt.magiapi.domain.services.ApplicationService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class ApplicationController(
    private val applicationService: ApplicationService
) {

    @PostMapping("/v1/projects/{project_id}/applications")
    fun create(
        @PathVariable("project_id") projectId: String,
        @Valid @RequestBody request: ApplicationRequest.Create
    ): ResponseEntity<Unit> {
        val userId = SecurityContext.getUserId()

        applicationService.create(request.toDomain(), userId, projectId)

        return ResponseEntity(HttpStatus.CREATED)
    }

    @PutMapping("/v1/projects/{project_id}/applications/{application_id}/ci")
    fun provisionCiWorkflow(
        @PathVariable("project_id") projectId: String,
        @PathVariable("application_id") applicationId: String): ResponseEntity<Unit> {
        val userId = SecurityContext.getUserId()

        applicationService.provisionCiWorkflow(applicationId, userId, projectId)

        return ResponseEntity.ok().build()
    }
}