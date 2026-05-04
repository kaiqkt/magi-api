package com.kaiqkt.magiapi.resources.github.responses

sealed class GithubWorkflowResponse {
    data class Success(
        val workflowRunId: Long,
        val runUrl: String,
    ) : GithubWorkflowResponse()
    object ReferenceNotFound : GithubWorkflowResponse()
    object InvalidAccessToken : GithubWorkflowResponse()
}