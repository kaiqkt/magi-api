package com.kaiqkt.magiapi.domain.exceptions

import java.util.Locale.getDefault

enum class ErrorType {
    EMAIL_ALREADY_EXISTS,
    PROJECT_ALREADY_EXIST,
    USER_NOT_FOUND,
    MEMBERSHIP_NOT_FOUND,
    PROJECT_NOT_FOUND,
    INVALID_CREDENTIALS,
    INVALID_ACCESS_TOKEN,
    INSUFFICIENT_PERMISSION,
    EXPIRED_ACCESS_TOKEN,
    INVALID_GIT_ACCESS_TOKEN,
    SERVER_ALREADY_EXISTS,
    GIT_ACCOUNT_NOT_FOUND,
    APPLICATION_ALREADY_EXIST,
    APPLICATION_NOT_FOUND;

    val message: String = toString().lowercase(getDefault()).replace("_", " ")
}
