package com.kasiguru.ui.components

import com.kasiguru.R

fun categoryDoodle(category: String): Int = when (category.substringBefore(" &").lowercase()) {
    "food" -> R.drawable.doodle_food
    "animals" -> R.drawable.doodle_animals
    "body parts" -> R.drawable.doodle_health
    "numbers" -> R.drawable.doodle_numbers
    "weather" -> R.drawable.doodle_weather
    "emotions" -> R.drawable.doodle_emotions
    "house" -> R.drawable.doodle_house
    "nature" -> R.drawable.doodle_nature
    "family" -> R.drawable.doodle_family
    "colors" -> R.drawable.doodle_colors
    "occupations" -> R.drawable.doodle_occupations
    else -> R.drawable.doodle_greetings
}
