package com.genesyx.app.ui.components

/** Shared email validation (mirrors the web zod `string().email()`). */
val EMAIL_REGEX = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")

fun isValidEmail(value: String): Boolean = EMAIL_REGEX.matches(value.trim())
