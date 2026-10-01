package com.kasiguru.domain.preferences

enum class AppearanceMode { SYSTEM, LIGHT, DARK }
object TextSize {
    val steps = listOf(90, 100, 115, 130)
    fun normalize(percent: Int): Int = if (percent in steps) percent else 100
}
