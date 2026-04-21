package com.kaiqkt.magiapi.application.web.security

import org.springframework.security.core.context.SecurityContextHolder

object SecurityContext {
    fun getUserId(): String {
        val context = SecurityContextHolder.getContext()
        val authentication = context.authentication as MagiAuthentication

        return authentication.principal as String
    }
}
