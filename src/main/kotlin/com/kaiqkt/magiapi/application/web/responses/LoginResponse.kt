package com.kaiqkt.magiapi.application.web.responses

import com.kaiqkt.magiapi.domain.dtos.AuthenticationDto

data class LoginResponse(
    val accessToken: String,
    val expiresIn: Int,
)

fun AuthenticationDto.toResponse() = LoginResponse(
    accessToken = this.accessToken,
    expiresIn = this.expiresIn
)