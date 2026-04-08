package com.kaiqkt.magiapi.domain.exceptions

class DomainException(
    val type: ErrorType,
) : Exception(type.message)
