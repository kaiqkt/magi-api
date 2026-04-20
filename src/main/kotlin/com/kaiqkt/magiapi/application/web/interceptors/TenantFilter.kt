package com.kaiqkt.magiapi.application.web.interceptors

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.util.AntPathMatcher
import org.springframework.web.filter.OncePerRequestFilter

@Component
class TenantFilter : OncePerRequestFilter() {

    private val pathMatcher = AntPathMatcher()

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        try {
            if (requireTenant(request)) {
                val host = request.serverName
                val tenant = extractTenant(host) ?: run {
                    response.status = HttpStatus.BAD_REQUEST.value()
                    return
                }

                TenantContext.setTenant(tenant)
            }
            filterChain.doFilter(request, response)
        } finally {
            TenantContext.clear()
        }
    }

    private fun requireTenant(request: HttpServletRequest): Boolean {
        val method = HttpMethod.valueOf(request.method)
        val path = request.servletPath

        return tenantRoutes.any { (routeMethod, pattern) ->
            routeMethod == method && pathMatcher.match(pattern, path)
        }
    }

    private fun extractTenant(host: String): String? {
        val parts = host.split(".")
        return if (parts.size > 2) parts[0] else null
    }

    companion object {
        private val tenantRoutes = listOf(
            HttpMethod.POST to "/v1/projects/member/*",
            HttpMethod.PUT to "/v1/projects/git",
            HttpMethod.POST to "/v1/applications",
            HttpMethod.PUT to "/v1/applications/*/ci",
            HttpMethod.POST to "/v1/servers",
        )
    }
}
