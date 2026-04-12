package com.kaiqkt.magiapi.domain.exceptions

import java.util.Locale.getDefault

enum class ErrorType() {
    EMAIL_ALREADY_EXISTS,
    USER_NOT_FOUND,
    INVALID_CREDENTIALS,
    INVALID_TOKEN,
    EXPIRED_TOKEN;

    val message: String = toString().lowercase(getDefault()).replace("_", " ")
}
