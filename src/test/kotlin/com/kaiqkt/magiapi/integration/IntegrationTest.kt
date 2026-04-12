package com.kaiqkt.magiapi.integration

import com.fasterxml.jackson.databind.ObjectMapper
import com.kaiqkt.magiapi.domain.repositories.UserRepository
import io.restassured.RestAssured
import io.restassured.config.ObjectMapperConfig
import io.restassured.mapper.ObjectMapperType
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.testcontainers.junit.jupiter.Testcontainers

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class IntegrationTest {
    @LocalServerPort
    var port: Int = 0

    @Autowired
    lateinit var mapper: ObjectMapper

    @Autowired
    lateinit var userRepository: UserRepository

    @BeforeAll
    fun before() {
        RestAssured.config =
            RestAssured
                .config()
                .objectMapperConfig(
                    ObjectMapperConfig(ObjectMapperType.JACKSON_2)
                        .jackson2ObjectMapperFactory { _, _ -> mapper },
                )
        RestAssured.baseURI = "http://localhost:$port"
    }

    @BeforeEach
    fun beforeEach() {
        userRepository.deleteAll()
    }
}
