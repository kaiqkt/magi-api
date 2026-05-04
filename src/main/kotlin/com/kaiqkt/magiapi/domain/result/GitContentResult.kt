package com.kaiqkt.magiapi.domain.result

sealed class GitContentResult {
    object Success : GitContentResult()
    object InvalidAccessToken : GitContentResult()
}