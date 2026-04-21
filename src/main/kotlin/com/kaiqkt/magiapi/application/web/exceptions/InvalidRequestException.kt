package com.kaiqkt.magiapi.application.web.exceptions

class InvalidRequestException(
    val errors: Map<String, Any>,
) : Exception()
