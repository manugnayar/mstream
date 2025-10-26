package com.streaming

data class Movie(
    val id: Int,
    val title: String,
    val thumbnailUrl: String,
    val description: String = "",
    val year: String = "",
    val movieUrl: String = ""
)