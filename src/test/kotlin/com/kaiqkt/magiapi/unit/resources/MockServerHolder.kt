package com.kaiqkt.magiapi.unit.resources

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
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

    val objectMapper: ObjectMapper = ObjectMapper().apply {
        registerKotlinModule()
        disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        enable(DeserializationFeature.READ_ENUMS_USING_TO_STRING)
        disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        propertyNamingStrategy = PropertyNamingStrategies.SNAKE_CASE
    }

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