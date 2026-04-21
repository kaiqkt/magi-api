package com.kaiqkt.magiapi.domain.services

import com.kaiqkt.magiapi.domain.dtos.AuthenticationDto
import com.kaiqkt.magiapi.domain.exceptions.DomainException
import com.kaiqkt.magiapi.domain.exceptions.ErrorType
import com.kaiqkt.magiapi.utils.MetricsUtils
import com.kaiqkt.magiapi.utils.MetricsUtils.Companion.STATUS
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service

@Service
class AuthenticationService(
    private val userService: UserService,
    private val metricsUtils: MetricsUtils,
    private val passwordEncoder: PasswordEncoder,
    private val tokenService: TokenService,
) {
    fun authenticate(email: String, password: String): AuthenticationDto {
        val user = userService.findByEmail(email)

        if (!passwordEncoder.matches(password, user.passwordHash)) {
            metricsUtils.counter(AUTHENTICATION, STATUS, "invalid_credentials")

            throw DomainException(ErrorType.INVALID_CREDENTIALS)
        }

        val authentication  = tokenService.issueTokens(user.id, user.roles)

        metricsUtils.counter(AUTHENTICATION, STATUS, "authenticated")

        return authentication
    }

    companion object {
        private const val AUTHENTICATION = "authentication"
    }
}

