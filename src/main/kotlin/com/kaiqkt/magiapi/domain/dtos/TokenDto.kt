package com.kaiqkt.magiapi.domain.dtos

import com.kaiqkt.magiapi.domain.models.enums.Role
import java.util.Date

data class TokenDto(
    val userId: String,
    val roles: List<Role>,
    val expiration: Date,
    val token: String
)