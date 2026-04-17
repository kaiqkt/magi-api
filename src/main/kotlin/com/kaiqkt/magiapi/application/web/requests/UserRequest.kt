package com.kaiqkt.magiapi.application.web.requests

import com.kaiqkt.magiapi.domain.models.User
import com.kaiqkt.magiapi.utils.PasswordEncrypt
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

sealed class UserRequest {
    data class Create(
        @field:Size(message = "must not exceed 100 characters", max = 100)
        @field:Pattern(regexp = "^[A-Za-z ]+$", message = "must contain only letters and spaces")
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

fun UserRequest.Create.toDomain(): User = User(
    email = this.email,
    passwordHash = PasswordEncrypt.encoder.encode(this.password),
    name = this.name,
)