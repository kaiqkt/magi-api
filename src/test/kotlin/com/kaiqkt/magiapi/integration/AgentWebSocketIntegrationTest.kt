package com.kaiqkt.magiapi.integration

import com.kaiqkt.magiapi.application.web.handlers.AgentWebSocketHandler
import com.kaiqkt.magiapi.domain.models.Project
import com.kaiqkt.magiapi.domain.models.Server
import com.kaiqkt.magiapi.domain.models.User
import com.kaiqkt.magiapi.domain.models.enums.Environment
import com.kaiqkt.magiapi.domain.models.enums.ServerStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketHttpHeaders
import org.springframework.web.socket.WebSocketSession
import org.springframework.web.socket.client.standard.StandardWebSocketClient
import org.springframework.web.socket.handler.TextWebSocketHandler
import java.net.URI
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import kotlin.test.Test

class AgentWebSocketIntegrationTest : IntegrationTest() {

    @Autowired
    lateinit var agentWebSocketHandler: AgentWebSocketHandler

    private val client = StandardWebSocketClient()

    private fun wsUri() = URI("ws://localhost:$port/ws/agent")

    private fun headersWithToken(token: String?) = WebSocketHttpHeaders().apply {
        token?.let { add("Authorization", "Bearer $it") }
    }

    @Nested
    inner class AgentConnection {

        @Nested
        inner class Auth {

            @Test
            fun `given no authorization header when connecting to agent websocket then reject handshake`() {
                val future = client.execute(TextWebSocketHandler(), headersWithToken(null), wsUri())

                assertThrows<ExecutionException> { future.get(5, TimeUnit.SECONDS) }
            }

            @Test
            fun `given an invalid agent token when connecting to agent websocket then reject handshake`() {
                val future = client.execute(TextWebSocketHandler(), headersWithToken("invalid-token"), wsUri())

                assertThrows<ExecutionException> { future.get(5, TimeUnit.SECONDS) }
            }
        }

        @Nested
        inner class HappyPath {

            @Test
            fun `given a valid agent token when connecting to agent websocket then set server status to active`() {
                val user = userRepository.save(User(email = "owner@example.com", passwordHash = "hash", name = "Owner"))
                val project = projectRepository.save(Project(name = "My Project", createdBy = user.id))
                val server = serverRepository.save(Server(projectId = project.id, environment = Environment.DEV, agentToken = "valid-agent-token"))

                val future = client.execute(TextWebSocketHandler(), headersWithToken("valid-agent-token"), wsUri())
                future.get(5, TimeUnit.SECONDS)
                Thread.sleep(100)

                val persisted = serverRepository.findById(server.id).get()
                assertEquals(ServerStatus.ACTIVE, persisted.status)
            }

            @Test
            fun `given an established connection when closing it then set server status to inactive`() {
                val user = userRepository.save(User(email = "owner@example.com", passwordHash = "hash", name = "Owner"))
                val project = projectRepository.save(Project(name = "My Project", createdBy = user.id))
                val server = serverRepository.save(Server(projectId = project.id, environment = Environment.DEV, agentToken = "valid-agent-token"))

                val future = client.execute(TextWebSocketHandler(), headersWithToken("valid-agent-token"), wsUri())
                val session = future.get(5, TimeUnit.SECONDS)
                Thread.sleep(100)

                session.close()
                Thread.sleep(100)

                val persisted = serverRepository.findById(server.id).get()
                assertEquals(ServerStatus.INACTIVE, persisted.status)
            }

            @Test
            fun `given an established connection when sending an event then client receives the message`() {
                val user = userRepository.save(User(email = "owner@example.com", passwordHash = "hash", name = "Owner"))
                val project = projectRepository.save(Project(name = "My Project", createdBy = user.id))
                val server = serverRepository.save(Server(projectId = project.id, environment = Environment.DEV, agentToken = "valid-agent-token"))

                val received = CountDownLatch(1)
                var receivedPayload: String? = null

                val handler = object : TextWebSocketHandler() {
                    override fun handleTextMessage(session: WebSocketSession, message: TextMessage) {
                        receivedPayload = message.payload
                        received.countDown()
                    }
                }

                val future = client.execute(handler, headersWithToken("valid-agent-token"), wsUri())
                future.get(5, TimeUnit.SECONDS)
                Thread.sleep(100)

                agentWebSocketHandler.sendEvent(server.id, """{"event":"test"}""")
                received.await(5, TimeUnit.SECONDS)

                assertEquals("""{"event":"test"}""", receivedPayload)
            }
        }
    }
}
