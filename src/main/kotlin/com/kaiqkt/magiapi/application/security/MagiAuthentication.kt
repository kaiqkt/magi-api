package com.kaiqkt.magiapi.application.security

import org.springframework.security.core.Authentication
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority

class MagiAuthentication(
    private val userId: String,
    private val roles: List<String>,
    private val token: String,
) : Authentication {
    private var authenticated: Boolean = true

    override fun getAuthorities(): Collection<GrantedAuthority> = roles.map { SimpleGrantedAuthority(it) }

    override fun getCredentials(): Any = token

    override fun getDetails(): Any =
        mapOf(
            "user_id" to userId,
        )

    override fun getPrincipal(): Any = userId

    override fun isAuthenticated(): Boolean = authenticated

    override fun setAuthenticated(isAuthenticated: Boolean) {
        authenticated = isAuthenticated
    }

    override fun getName(): String = userId
}
