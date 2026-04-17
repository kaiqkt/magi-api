package com.kaiqkt.magiapi.resources.github.requests

data class GithubRepositoryRequest(
    val name: String,
    val private: Boolean = true,
)