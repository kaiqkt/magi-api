package com.kaiqkt.magiapi.domain.dtos

sealed class ApplicationDto {
    data class Create(val name: String, val description: String?) : ApplicationDto()
}