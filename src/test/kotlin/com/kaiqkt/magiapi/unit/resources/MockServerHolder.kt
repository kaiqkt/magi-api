package com.kaiqkt.magiapi.unit.resources

import com.kaiqkt.magiapi.application.config.ObjectMapperConfig
import io.restassured.http.Method
import org.mockserver.client.MockServerClient
import org.mockserver.integration.ClientAndServer
import org.mockserver.model.HttpRequest
import org.mockserver.verify.VerificationTimes

abstract class MockServerHolder {
    companion object {
        private const val PORT: Int = 8081
        private val mockServer: ClientAndServer = ClientAndServer.startClientAndServer(PORT)
        private val baseUrl = "http://127.0.0.1:${mockServer.localPort}"
    }

    val objectMapper = ObjectMapperConfig().objectMapper()

    protected abstract fun domainPath(): String

    fun baseUrl() = "$baseUrl${domainPath()}"

    protected fun mockServer(): ClientAndServer = mockServer

    fun reset(): MockServerClient = mockServer.clear(HttpRequest.request().withPath("${domainPath()}.*"))

    protected fun verifyRequest(
        method: Method,
        path: String,
        times: Int,
    ) {
        val request =
            HttpRequest
                .request()
                .withMethod(method.name)
                .withPath(path)

        mockServer.verify(
            request,
            VerificationTimes.exactly(times),
        )
    }
}