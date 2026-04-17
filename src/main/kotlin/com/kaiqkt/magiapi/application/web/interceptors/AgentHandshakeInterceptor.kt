package com.kaiqkt.magiapi.application.web.interceptors

import com.kaiqkt.magiapi.domain.services.ServerService
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.server.ServerHttpRequest
import org.springframework.http.server.ServerHttpResponse
import org.springframework.stereotype.Component
import org.springframework.web.socket.WebSocketHandler
import org.springframework.web.socket.server.HandshakeInterceptor

fun MutableMap<String, Any>.getServerId(): String? = this["serverId"] as? String

@Component
class AgentHandshakeInterceptor(
    private val serverService: ServerService
) : HandshakeInterceptor {

    override fun beforeHandshake(
        request: ServerHttpRequest,
        response: ServerHttpResponse,
        wsHandler: WebSocketHandler,
        attributes: MutableMap<String, Any>
    ): Boolean {
        val token = request.headers.getFirst(HttpHeaders.AUTHORIZATION)
            ?.removePrefix("Bearer ") ?: run {
            response.setStatusCode(HttpStatus.UNAUTHORIZED)
            return false
        }

        val server = serverService.findByAgentToken(token) ?: run {
            response.setStatusCode(HttpStatus.UNAUTHORIZED)
            return false
        }

        attributes["serverId"] = server.id
        return true
    }

    override fun afterHandshake(
        request: ServerHttpRequest,
        response: ServerHttpResponse,
        wsHandler: WebSocketHandler,
        exception: Exception?
    ) {
    }
}