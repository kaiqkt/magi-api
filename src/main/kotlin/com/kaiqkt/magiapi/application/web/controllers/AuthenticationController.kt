package com.kaiqkt.magiapi.application.web.controllers

import com.kaiqkt.magiapi.application.web.requests.AuthenticationRequest
import com.kaiqkt.magiapi.application.web.responses.LoginResponse
import com.kaiqkt.magiapi.application.web.responses.toResponse
import com.kaiqkt.magiapi.domain.services.AuthenticationService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class AuthenticationController(
    private val authenticationService: AuthenticationService
) {

    @PostMapping("/v1/auth")
    fun login(@RequestBody request: AuthenticationRequest): ResponseEntity<LoginResponse> {
        val authentication = authenticationService.authenticate(request.email, request.password)

        return ResponseEntity.ok(authentication.toResponse())
    }
}