package com.cs407.snapchef

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
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

        val homeButton = findViewById<Button>(R.id.homeButton)
        homeButton.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish() // if you want to close this activity
        }

        CoroutineScope(Dispatchers.IO).launch {
            recipe = recipeDB.recipeDao().getRecipeById(id)!!

            val newSteps = mutableListOf<Step>()
            recipe.steps.forEach { step ->
                newSteps.add(Step(step.ingredients, step.step, step.time))
            }

            val inputRecipe = Recipe(
                name = recipe.name,
                description = recipe.description,
                steps = newSteps
            )

            withContext(Dispatchers.Main) { displayRecipe(inputRecipe) }
        }
    }


    private fun displayRecipe(recipe: Recipe) {
        val titleTextView = findViewById<TextView>(R.id.recipeTitle)
        val descriptionTextView = findViewById<TextView>(R.id.recipeDescription)
        val recipeLayout = findViewById<LinearLayout>(R.id.recipeLayout)
        val scrollView = findViewById<ScrollView>(R.id.recipeScrollView)

        // Clear any previous step content
        recipeLayout.removeAllViews()

        // Set the title and description
        // (The large font sizes and styling are defined in the XML)
        titleTextView.text = recipe.name
        descriptionTextView.text = recipe.description

        // Add Steps
        recipe.steps.forEachIndexed { index, step ->
            // Step Title
            val stepTitle = TextView(this).apply {
                text = "Step ${index + 1}"
                textSize = 20f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                setPadding(0, 16, 0, 8)
            }
            recipeLayout.addView(stepTitle)

            // Instructions
            val stepInstructions = TextView(this).apply {
                text = step.step
                textSize = 16f
                setPadding(0, 0, 0, 8)
            }
            recipeLayout.addView(stepInstructions)

            // Ingredients
            val ingredientsTextView = TextView(this).apply {
                text = "Ingredients: ${step.ingredients.joinToString(", ")}"
                textSize = 14f
                setPadding(0, 0, 0, 4)
            }
            recipeLayout.addView(ingredientsTextView)

            // Time
            val timeTextView = TextView(this).apply {
                text = "Time: ${step.time}"
                textSize = 14f
                setPadding(0, 0, 0, 16)
            }
            recipeLayout.addView(timeTextView)

            // Optional: Divider line between steps
            if (index < recipe.steps.size - 1) {
                val divider = View(this).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        2
                    )
                    setBackgroundColor(resources.getColor(android.R.color.darker_gray))
                }
                recipeLayout.addView(divider)
            }
        }

        // Ensure the ScrollView is visible
        scrollView.visibility = View.VISIBLE
    }


}