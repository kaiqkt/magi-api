package com.kaiqkt.magiapi.application.security

import com.kaiqkt.magiapi.domain.dtos.TokenDto
import org.springframework.security.core.Authentication
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority

class MagiAuthentication(
    private val tokenDto: TokenDto,
) : Authentication {
    private var authenticated: Boolean = true

    override fun getAuthorities(): Collection<GrantedAuthority> = tokenDto.roles.map { SimpleGrantedAuthority(it.name) }

    override fun getCredentials(): Any = tokenDto.token

    override fun getDetails(): Any = mapOf("user_id" to tokenDto.userId)
    override fun getPrincipal(): Any = tokenDto.userId

    override fun isAuthenticated(): Boolean = authenticated

    override fun setAuthenticated(isAuthenticated: Boolean) {
        authenticated = isAuthenticated
    }

    override fun getName(): String = tokenDto.userId
}
