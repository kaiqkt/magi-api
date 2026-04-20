package com.kaiqkt.magiapi.domain.dtos

sealed class GitUserDto {
    data class Success(
        val username: String,
        val profileUrl: String,
    ): GitUserDto()
    object InvalidAccessToken: GitUserDto()
}