package com.cs407.snapchef

data class Ingredient(
    val name: String,
    val quantity: Number,
    val unit: String
)

data class IngredientsResponse(
    val success: Boolean,
    val data: IngredientsData
)

data class IngredientsData(
    val ingredients: List<Ingredient>
)