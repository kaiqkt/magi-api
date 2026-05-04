package com.kaiqkt.magiapi.domain.result

sealed class GitWorkflowResult {
    data class Success(
        val id: Long,
        val url: String,
    ) : GitWorkflowResult()
    object ReferenceNotFound : GitWorkflowResult()
    object InvalidAccessToken : GitWorkflowResult()
}