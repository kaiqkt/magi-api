package com.kaiqkt.magiapi.utils

import com.kaiqkt.magiapi.domain.config.AuthenticationProperties
import com.kaiqkt.magiapi.domain.dtos.AuthenticationDto
import com.kaiqkt.magiapi.domain.dtos.TokenDto
import com.kaiqkt.magiapi.domain.exceptions.AuthorizationException
import com.kaiqkt.magiapi.domain.models.enums.Role
import com.nimbusds.jose.JOSEObjectType
import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.crypto.MACSigner
import com.nimbusds.jose.crypto.MACVerifier
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.security.SecureRandom
import java.text.ParseException
import java.time.Instant
import java.util.Base64
import java.util.Date

@Component
class TokenUtils(
    private val properties: AuthenticationProperties,
    @param:Value("\${spring.application.name}")
    private val applicationName: String,
) {
    private val secureRandom = SecureRandom()

    fun issueTokens(
        userId: String,
        roles: Set<Role>,
    ): AuthenticationDto {
        val accessToken = sign(userId, roles)

        return AuthenticationDto(
            accessToken = accessToken,
            expiresIn = properties.accessTokenTtl.toInt(),
        )
    }

    fun sign(
        subject: String,
        roles: Set<Role>,
    ): String {
        val now = Instant.now()

        val claims =
            JWTClaimsSet
                .Builder()
                .issuer(applicationName)
                .subject(subject)
                .audience(applicationName)
                .issueTime(Date.from(now))
                .expirationTime(Date.from(now.plusSeconds(properties.accessTokenTtl)))
                .claim(ROLES, roles.map(Role::name))
                .build()

        val header =
            JWSHeader
                .Builder(JWSAlgorithm.HS256)
                .type(JOSEObjectType.JWT)
                .build()

        val signedJWT =
            SignedJWT(header, claims).apply {
                sign(MACSigner(properties.accessTokenSecret.toByteArray()))
            }

        return signedJWT.serialize()
    }

    fun getInformation(token: String): TokenDto {
        try {
            val signedJWT = SignedJWT.parse(token)

            val verifier = MACVerifier(properties.accessTokenSecret.toByteArray())
            if (!signedJWT.verify(verifier)) {
                throw AuthorizationException("Invalid access token")
            }
            val jwtClaimsSet = signedJWT.jwtClaimsSet

            if (jwtClaimsSet.expirationTime.before(Date())) {
                throw AuthorizationException("Expired access token")
            }

            return TokenDto(
                userId = jwtClaimsSet.subject,
                roles = jwtClaimsSet.getStringListClaim("roles").map { Role.valueOf(it) },
                expiration = jwtClaimsSet.expirationTime,
                token = token,
            )
        } catch (_: ParseException) {
            throw AuthorizationException("Invalid access token")
        }
    }

    fun opaqueToken(): String {
        val bytes = ByteArray(32)
        secureRandom.nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    companion object {
        private const val ROLES = "roles"
    }
}
