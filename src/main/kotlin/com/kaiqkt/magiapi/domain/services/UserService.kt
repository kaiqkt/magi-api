package com.kaiqkt.magiapi.domain.services

import com.kaiqkt.magiapi.domain.dtos.UserDto
import com.kaiqkt.magiapi.domain.exceptions.DomainException
import com.kaiqkt.magiapi.domain.exceptions.ErrorType
import com.kaiqkt.magiapi.domain.models.User
import com.kaiqkt.magiapi.domain.repositories.UserRepository
import com.kaiqkt.magiapi.utils.MetricsUtils
import com.kaiqkt.magiapi.utils.MetricsUtils.Companion.CREATED
import com.kaiqkt.magiapi.utils.MetricsUtils.Companion.STATUS
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service

@Service
class UserService(
    private val userRepository: UserRepository,
    private val metricsUtils: MetricsUtils,
    private val passwordEncoder: PasswordEncoder
) {
    fun create(userDto: UserDto) {
        if (userRepository.existsByEmail(userDto.email)) {
            throw DomainException(ErrorType.EMAIL_ALREADY_EXISTS)
        }

        val user = User(
            email = userDto.email,
            passwordHash = passwordEncoder.encode(userDto.password),
            name = userDto.name,
        )

        userRepository.save(user)

        metricsUtils.counter(USER, STATUS, CREATED)
    }

    fun findByEmail(email: String): User {
        return userRepository.findByEmail(email) ?: throw DomainException(ErrorType.USER_NOT_FOUND)
    }

    companion object {
        private const val USER = "user"
    }
}