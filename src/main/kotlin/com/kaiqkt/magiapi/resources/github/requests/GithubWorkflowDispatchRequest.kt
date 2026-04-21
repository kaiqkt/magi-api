package com.kaiqkt.magiapi.resources.github.requests

data class GithubWorkflowDispatchRequest(
    val ref: String,
    val returnRunDetails: Boolean = true,
)
