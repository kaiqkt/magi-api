package com.kaiqkt.magiapi.application.web.controllers

import com.kaiqkt.magiapi.application.web.requests.UserRequest
import com.kaiqkt.magiapi.application.web.requests.toDomain
import com.kaiqkt.magiapi.domain.services.UserService
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController


@RestController
class UserController(
    private val userService: UserService
) {
    @PostMapping("/v1/users")
    fun create(
        @Valid @RequestBody request: UserRequest.Create,
    ): ResponseEntity<Unit> {
        userService.create(request.toDomain())

        return ResponseEntity.noContent().build()
    }
}
