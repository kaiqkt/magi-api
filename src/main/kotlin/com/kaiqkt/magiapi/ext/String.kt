package com.kaiqkt.magiapi.utils

import com.github.slugify.Slugify

private val slugify = Slugify.builder().build()

fun String.slugify(): String = slugify.slugify(this)