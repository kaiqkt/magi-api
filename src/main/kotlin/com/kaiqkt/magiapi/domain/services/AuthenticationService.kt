package com.kaiqkt.magiapi.domain.services

import com.kaiqkt.magiapi.domain.dtos.AuthenticationDto
import com.kaiqkt.magiapi.domain.exceptions.DomainException
import com.kaiqkt.magiapi.domain.exceptions.ErrorType
import com.kaiqkt.magiapi.utils.TokenUtils
import org.springframework.beans.factory.annotation.Value
import com.kaiqkt.magiapi.utils.MetricsUtils
import com.kaiqkt.magiapi.utils.MetricsUtils.Companion.STATUS
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service

@Service
class AuthenticationService(
    private val userService: UserService,
    private val metricsUtils: MetricsUtils,
    private val passwordEncoder: PasswordEncoder,
    @param:Value($$"${authentication.access-token-ttl}")
    private val ttl: Long,
    @param:Value($$"${authentication.access-token-secret}")
    private val secret: String,
) {
    fun authenticate(email: String, password: String): AuthenticationDto {
        val user = userService.findByEmail(email)

        if (!passwordEncoder.matches(password, user.passwordHash)) {
            metricsUtils.counter(AUTHENTICATION, STATUS, "invalid_credentials")

            throw DomainException(ErrorType.INVALID_CREDENTIALS)
        }

        val authentication  =
            TokenUtils.issueTokens(
                subject = user.id,
                ttl = ttl,
                roles = user.roles,
                secret = secret,
            )

        metricsUtils.counter(AUTHENTICATION, STATUS, "authenticated")

        return authentication
    }

    companion object {
        private const val AUTHENTICATION = "authentication"
    }
}

