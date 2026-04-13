package com.kaiqkt.magiapi.application.web.interceptors

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class TenantFilter : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        try {
            val host = request.serverName
            val tenant = extractTenant(host)

            if (tenant != null) {
                TenantContext.setTenant(tenant)
            }

            filterChain.doFilter(request, response)
        } finally {
            TenantContext.clear()
        }
    }

    private fun extractTenant(host: String): String? {
        val parts = host.split(".")
        return if (parts.size > 2) parts[0] else null
    }
}