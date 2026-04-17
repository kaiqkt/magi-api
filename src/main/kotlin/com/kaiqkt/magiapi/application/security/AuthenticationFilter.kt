package com.kaiqkt.magiapi.application.security

import com.kaiqkt.magiapi.utils.TokenUtils
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class AuthenticationFilter(
    @param:Value($$"${authentication.access-token-secret}")
    private val secret: String,
) : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        try {
            if (shouldSkipFilter(request)) {
                filterChain.doFilter(request, response)
                return
            }

            val authorizationHeader = request.getHeader(HttpHeaders.AUTHORIZATION)

            if (!authorizationHeader.isNullOrBlank() &&
                authorizationHeader.startsWith(BEARER, ignoreCase = true)
            ) {
                val token = authorizationHeader.substringAfter(BEARER).trim()

                val authentication = handleAccessToken(token)
                SecurityContextHolder.getContext().authentication = authentication
            }

            filterChain.doFilter(request, response)
        } catch (_: Exception) {
            response.status = HttpStatus.UNAUTHORIZED.value()
        }
    }

    private fun shouldSkipFilter(request: HttpServletRequest): Boolean {
        val route = HttpMethod.valueOf(request.method) to request.servletPath

        return route in publicRoutes
    }

    private fun handleAccessToken(token: String): MagiAuthentication {
        val claims = TokenUtils.getClaims(token, secret)
        val userId = claims.subject
        val roles = claims.getStringListClaim("roles")

        return MagiAuthentication(
            userId = userId,
            roles = roles,
            token = token,
        )
    }

    companion object {
        private const val BEARER = "Bearer "
        val publicRoutes =
            listOf(
                HttpMethod.POST to "/v1/auth",
                HttpMethod.POST to "/v1/users",
                HttpMethod.GET to "/ws/agent",
            )
    }
}
