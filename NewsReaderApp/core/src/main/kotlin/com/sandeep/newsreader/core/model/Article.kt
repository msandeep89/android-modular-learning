package com.sandeep.newsreader.core.model

data class Article(
    val id: String,
    val title: String,
    val description: String?,
    val url: String,
    val imageUrl: String?,
    val publishedAt: String,
    val source: String,
    val category: Category = Category.TOP
)

enum class Category(val label: String) {
    TOP("Top Headlines"),
    SPORTS("Sports"),
    TECH("Technology"),
    BUSINESS("Business"),
    HEALTH("Health"),
    SCIENCE("Science")
}
