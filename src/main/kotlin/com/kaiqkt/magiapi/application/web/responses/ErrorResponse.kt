package com.kaiqkt.magiapi.application.web.responses

data class ErrorResponse(
    val message: String?,
    val details: Map<String, Any>,
)
