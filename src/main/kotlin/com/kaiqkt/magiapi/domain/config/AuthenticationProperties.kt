package com.kaiqkt.magiapi.domain.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "authentication")
data class AuthenticationProperties(
    val accessTokenTtl: Long,
    val accessTokenSecret: String,
)