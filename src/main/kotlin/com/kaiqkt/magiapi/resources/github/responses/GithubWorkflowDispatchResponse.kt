package com.kaiqkt.magiapi.resources.github.responses

data class GithubWorkflowDispatchResponse(
    val workflowRunId: Long,
    val runUrl: String,
)
