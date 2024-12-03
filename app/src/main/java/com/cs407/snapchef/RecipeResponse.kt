package com.cs407.snapchef

data class RecipeResponse(
    val success: Boolean,
    val data: RecipeData
)

data class RecipeData(
    val recipe: Recipe
)

data class Recipe(
    val name: String,
    val description: String,
    val steps: List<Step>
)

data class Step(
    val ingredients: List<String>,
    val step: String,
    val time: String
)