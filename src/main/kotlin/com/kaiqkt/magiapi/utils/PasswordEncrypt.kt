package com.kaiqkt.magiapi.utils

import org.springframework.security.crypto.argon2.Argon2PasswordEncoder

object PasswordEncrypt {
    val encoder: Argon2PasswordEncoder = Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8()
}