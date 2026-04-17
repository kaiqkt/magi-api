package com.kaiqkt.magiapi.application.web.responses

sealed class ServerResponse {
    data class Created(val agentToken: String): ServerResponse()
}