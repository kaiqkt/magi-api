package com.kaiqkt.magiapi.domain.dtos

sealed class GitContentDto {
    data class Create(
        val owner: String,
        val repo: String,
        val encodedContent: String,
        val accessToken: String,
    ) : GitContentDto()

    object Success : GitContentDto()
    object InvalidAccessToken : GitContentDto()
}