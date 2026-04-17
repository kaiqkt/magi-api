package com.kaiqkt.magiapi.utils

import com.kaiqkt.magiapi.domain.dtos.AuthenticationDto
import com.kaiqkt.magiapi.domain.exceptions.DomainException
import com.kaiqkt.magiapi.domain.exceptions.ErrorType
import com.kaiqkt.magiapi.domain.models.enums.Role
import com.nimbusds.jose.JOSEObjectType
import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.crypto.MACSigner
import com.nimbusds.jose.crypto.MACVerifier
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import java.security.SecureRandom
import java.text.ParseException
import java.time.Instant
import java.util.Base64
import java.util.Date


object TokenUtils {
    private const val APPLICATION = "magi"
    private val secureRandom = SecureRandom()

    fun issueTokens(
        subject: String,
        ttl: Long,
        roles: Set<Role>,
        secret: String,
    ): AuthenticationDto {
        val accessToken = signJWT(subject, ttl, roles, secret)

        return AuthenticationDto(
            accessToken = accessToken,
            expiresIn = ttl.toInt(),
        )
    }

    fun signJWT(
        subject: String,
        ttl: Long,
        roles: Set<Role>,
        secret: String,
    ): String {
        val now = Instant.now()

        val claims =
            JWTClaimsSet
                .Builder()
                .issuer(APPLICATION)
                .subject(subject)
                .audience(APPLICATION)
                .issueTime(Date.from(now))
                .expirationTime(Date.from(now.plusSeconds(ttl)))
                .claim("roles", roles.map(Role::name))
                .build()

        val header =
            JWSHeader
                .Builder(JWSAlgorithm.HS256)
                .type(JOSEObjectType.JWT)
                .build()

        val signedJWT =
            SignedJWT(header, claims).apply {
                sign(MACSigner(secret.toByteArray()))
            }

        return signedJWT.serialize()
    }

    fun opaqueToken(): String {
        val bytes = ByteArray(32)
        secureRandom.nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    fun getClaims(
        token: String,
        secret: String,
    ): JWTClaimsSet {
        try {
            val signedJWT = SignedJWT.parse(token)

            val verifier = MACVerifier(secret.toByteArray())
            if (!signedJWT.verify(verifier)) {
                throw DomainException(ErrorType.INVALID_ACCESS_TOKEN)
            }
            val jwtClaimsSet = signedJWT.jwtClaimsSet

            if (jwtClaimsSet.expirationTime.before(Date())) {
                throw DomainException(ErrorType.EXPIRED_ACCESS_TOKEN)
            }

            return jwtClaimsSet
        } catch (_: ParseException) {
            throw DomainException(ErrorType.INVALID_ACCESS_TOKEN)
        }
    }
}