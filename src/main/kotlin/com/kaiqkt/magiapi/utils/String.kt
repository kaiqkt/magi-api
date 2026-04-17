package com.kaiqkt.magiapi.utils

fun String.slugify() = this.replace(" ", "-").lowercase()