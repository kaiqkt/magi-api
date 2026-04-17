package com.kaiqkt.magiapi.application.web.controllers

import com.kaiqkt.magiapi.application.security.SecurityContext
import com.kaiqkt.magiapi.application.web.interceptors.TenantContext
import com.kaiqkt.magiapi.application.web.requests.ApplicationRequest
import com.kaiqkt.magiapi.application.web.requests.toDomain
import com.kaiqkt.magiapi.domain.services.ApplicationService
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class ApplicationController(
    private val applicationService: ApplicationService
) {

    @PostMapping("/v1/applications")
    fun create(@Valid @RequestBody request: ApplicationRequest.Create): ResponseEntity<Unit> {
        val userId = SecurityContext.getUserId()
        val tenantId = TenantContext.getTenant() ?: return ResponseEntity.badRequest().build()

        applicationService.create(request.toDomain(), userId, tenantId)

        return ResponseEntity.noContent().build()
    }
}