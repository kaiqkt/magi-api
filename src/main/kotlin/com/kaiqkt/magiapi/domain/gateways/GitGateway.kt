package com.kaiqkt.magiapi.domain.gateways

import com.kaiqkt.magiapi.domain.dtos.GitUserDto

interface GitGateway {
    fun findUser(accessToken: String): GitUserDto?
    fun createRepository(name: String, accessToken: String): String
}

//repo deve ser privado - private