package com.kaiqkt.magiapi.application.web.security

import com.kaiqkt.magiapi.domain.exceptions.AuthorizationException
import com.kaiqkt.magiapi.utils.TokenUtils
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class AuthenticationFilter(
    private val tokenUtils: TokenUtils
) : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        if (shouldSkipFilter(request)) {
            filterChain.doFilter(request, response)
            return
        }

        try {
            val authorizationHeader = request.getHeader(HttpHeaders.AUTHORIZATION)

            if (!authorizationHeader.isNullOrBlank() &&
                authorizationHeader.startsWith(BEARER, ignoreCase = true)
            ) {
                val token = authorizationHeader.substringAfter(BEARER).trim()

                val authentication = MagiAuthentication(tokenUtils.getInformation(token))
                SecurityContextHolder.getContext().authentication = authentication
            }
        } catch (e: AuthorizationException) {
            logger.error("Exception occurred while authentication filter", e)
            response.status = HttpStatus.UNAUTHORIZED.value()
            return
        }

        filterChain.doFilter(request, response)
    }

    private fun shouldSkipFilter(request: HttpServletRequest): Boolean {
        val route = HttpMethod.valueOf(request.method) to request.servletPath

        return route in publicRoutes
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
