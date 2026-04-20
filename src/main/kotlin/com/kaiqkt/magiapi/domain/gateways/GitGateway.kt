package com.kaiqkt.magiapi.domain.gateways

import com.kaiqkt.magiapi.domain.dtos.GitContentDto
import com.kaiqkt.magiapi.domain.dtos.GitRepositoryDto
import com.kaiqkt.magiapi.domain.dtos.GitUserDto

interface GitGateway {
    fun findUser(accessToken: String): GitUserDto
    fun createRepository(name: String, accessToken: String): GitRepositoryDto
    fun uploadContent(
        content: GitContentDto.Create
    ): GitContentDto
}