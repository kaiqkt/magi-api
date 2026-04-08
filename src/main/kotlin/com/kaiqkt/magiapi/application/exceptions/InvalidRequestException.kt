package com.kaiqkt.magiapi.application.exceptions

class InvalidRequestException(
    val errors: Map<String, Any>,
) : Exception()
