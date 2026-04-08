package com.kaiqkt.magiapi.application.config

import com.kaiqkt.magiapi.utils.Constants
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.MDC
import org.springframework.stereotype.Component
import org.springframework.web.servlet.HandlerInterceptor
import java.util.UUID

@Component
class WebInterceptor : HandlerInterceptor {
    override fun preHandle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any,
    ): Boolean {
        val requestId: String = request.getHeader("X-Request-Id") ?: UUID.randomUUID().toString()

        MDC.put(Constants.Parameters.REQUEST_ID, requestId)

        return true
    }
}
