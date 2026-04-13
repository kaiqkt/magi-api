package com.kaiqkt.magiapi.application.web.requests

import com.kaiqkt.magiapi.domain.models.Project
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

sealed class ProjectRequest {
    data class Create(
        @field:Size(message = "must not exceed 100 characters", max = 100)
        @field:Pattern(regexp = "^[A-Za-z ]+$", message = "must contain only letters and spaces")
        val name: String
    )
}

fun ProjectRequest.Create.toDomain(userId: String) = Project(
    name = this.name,
    createdBy = userId
)