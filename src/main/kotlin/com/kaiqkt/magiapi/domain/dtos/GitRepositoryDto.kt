package com.kaiqkt.magiapi.domain.dtos

sealed class GitRepositoryDto {
    data class Created(val repositoryUrl: String): GitRepositoryDto()
    object InvalidAccessToken: GitRepositoryDto()
}