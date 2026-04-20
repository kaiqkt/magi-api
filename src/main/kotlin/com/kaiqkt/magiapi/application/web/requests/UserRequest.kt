package com.kaiqkt.magiapi.application.web.requests

import com.kaiqkt.magiapi.domain.dtos.UserDto
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

sealed class UserRequest {
    data class Create(
        @field:Size(message = "must not exceed 100 characters", max = 100)
        @field:Pattern(regexp = "^[A-Za-z ]+$", message = "must contain only letters and spaces")
        @field:NotBlank(message = "must not be blank")
        val name: String,
        @field:Email(message = "must be a valid email")
        val email: String,
        @field:Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z\\d]).{8,}$",
            message = "must be at least 8 characters long and include uppercase, lowercase, number and special character",
        )
        @field:Size(message = "must not exceed 50 characters", max = 50)
        val password: String,
    ) : UserRequest()
}

fun UserRequest.Create.toDomain(): UserDto = UserDto(
    email = this.email,
    password = this.password,
    name = this.name,
)