package com.kaiqkt.magiapi.application.web.handlers

import com.kaiqkt.magiapi.application.web.interceptors.getServerId
import com.kaiqkt.magiapi.domain.models.enums.ServerStatus
import com.kaiqkt.magiapi.domain.services.ServerService
import org.springframework.stereotype.Component
import org.springframework.web.socket.CloseStatus
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession
import org.springframework.web.socket.handler.TextWebSocketHandler
import java.util.concurrent.ConcurrentHashMap

@Component
class AgentWebSocketHandler(
    private val serverService: ServerService,
): TextWebSocketHandler() {

    private val sessions = ConcurrentHashMap<String, WebSocketSession>()

    override fun afterConnectionEstablished(session: WebSocketSession) {
        val serverId = session.attributes.getServerId() ?: return
        sessions[serverId] = session
        serverService.activate(serverId)
    }

    override fun afterConnectionClosed(session: WebSocketSession, status: CloseStatus) {
        val serverId = session.attributes.getServerId() ?: return

        sessions.remove(serverId)
        serverService.deactivate(serverId)
    }

    fun sendEvent(serverId: String, payload: String) {
        sessions[serverId]?.sendMessage(TextMessage(payload))
    }
}