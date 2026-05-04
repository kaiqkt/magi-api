package com.kaiqkt.magiapi.domain.dtos

data class GitDto(
    val owner: String,
    val repository: String,
    val accessToken: String,
)
