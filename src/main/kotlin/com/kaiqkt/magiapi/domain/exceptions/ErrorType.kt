package com.kaiqkt.magiapi.domain.exceptions

import java.util.Locale.getDefault

enum class ErrorType {
    EMAIL_ALREADY_EXISTS,
    PROJECT_ALREADY_EXIST,
    USER_NOT_FOUND,
    PROJECT_NOT_FOUND,
    INVALID_CREDENTIALS,
    INVALID_TOKEN,
    INSUFFICIENT_PERMISSION,
    EXPIRED_TOKEN;

    val message: String = toString().lowercase(getDefault()).replace("_", " ")
}
