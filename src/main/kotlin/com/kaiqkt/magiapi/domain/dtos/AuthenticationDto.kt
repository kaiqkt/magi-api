package com.kaiqkt.magiapi.domain.dtos

data class AuthenticationDto(
    val accessToken: String,
    val expiresIn: Int,
)
