package com.kaiqkt.magiapi.application.web.requests

import com.kaiqkt.magiapi.domain.dtos.ApplicationDto
import com.kaiqkt.magiapi.utils.slugify
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

sealed class ApplicationRequest {
    data class Create(
        @field:Size(message = "must not exceed 50 characters", max = 50)
        @field:Pattern(regexp = "^[A-Za-z ]+$", message = "must contain only letters and spaces")
        @field:NotBlank(message = "must not be blank")
        val name: String,
        @field:Size(message = "must not exceed 255 characters", max = 255)
        val description: String?
    ) : ApplicationRequest()
}

fun ApplicationRequest.Create.toDomain() = ApplicationDto.Create(
    name = this.name.slugify(),
    description = this.description
)