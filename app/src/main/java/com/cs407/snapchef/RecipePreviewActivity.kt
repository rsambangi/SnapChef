package com.cs407.snapchef

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.cs407.snapchef.data.RecipeDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RecipePreviewActivity : AppCompatActivity() {

    private lateinit var recipeDB: RecipeDatabase
    private lateinit var recipe: com.cs407.snapchef.data.Recipe

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recipe_preview)

        recipeDB = RecipeDatabase.getDatabase(this@RecipePreviewActivity)

        val id = intent.getIntExtra("recipeId", 0)

        CoroutineScope(Dispatchers.IO).launch {
            recipe = recipeDB.recipeDao().getRecipeById(id)!!

            val newSteps = mutableListOf<Step>()
            recipe.steps.forEach { step ->
                newSteps.add(Step(step.ingredients, step.step, step.time))
            }

            val inputRecipe = Recipe(name = recipe.name,
                description = recipe.description,
                steps = newSteps)

           withContext(Dispatchers.Main) { displayRecipe(inputRecipe)}
        }
    }

    private fun displayRecipe(recipe: Recipe) {
        val recipeLayout = findViewById<LinearLayout>(R.id.recipeLayout)
        recipeLayout.removeAllViews() // Clear any previous content

        // Add Recipe Name
        val nameTextView = TextView(this).apply {
            text = recipe.name
            textSize = 24f
            setPadding(0, 0, 0, 8) // Bottom padding
        }
        recipeLayout.addView(nameTextView)

        // Add Recipe Description
        val descriptionTextView = TextView(this).apply {
            text = recipe.description
            textSize = 16f
            setPadding(0, 0, 0, 16) // Bottom padding
        }
        recipeLayout.addView(descriptionTextView)

        // Add Steps
        recipe.steps.forEachIndexed { index, step ->
            val stepTextView = TextView(this).apply {
                text =
                    "Step ${index + 1}: ${step.step}\nIngredients: ${step.ingredients.joinToString(", ")}\nTime: ${step.time}"
                setPadding(0, 0, 0, 16) // Bottom padding
            }
            recipeLayout.addView(stepTextView)
        }
    }
}