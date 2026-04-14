package com.kaiqkt.magiapi.domain.gateways

import com.kaiqkt.magiapi.domain.dtos.GitUserDto

interface GitGateway {
    fun findUser(accessToken: String): GitUserDto?
}