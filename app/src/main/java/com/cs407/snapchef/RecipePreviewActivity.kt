package com.cs407.snapchef

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
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

        val startButton = findViewById<Button>(R.id.startButton)
        startButton.setOnClickListener {
            val intent = Intent(this, StepByStepPage::class.java)
            intent.putExtra("recipeId", id)
            startActivity(intent)
        }

        val shareButton = findViewById<Button>(R.id.shareButton)
        shareButton.setOnClickListener {
            val formattedRecipe = formatRecipeForSharing(recipe)
            val intent= Intent()
            intent.action=Intent.ACTION_SEND
            intent.putExtra(Intent.EXTRA_TEXT, formattedRecipe)
            intent.type="text/plain"
            startActivity(Intent.createChooser(intent,"Share To:"))
        }
    }

    private fun displayRecipe(recipe: Recipe) {
        val recipeTitle = findViewById<TextView>(R.id.recipeTitle)
        val recipeDescription = findViewById<TextView>(R.id.recipeDescription)
        val recipeDetails = findViewById<TextView>(R.id.recipeDetails)

        recipeTitle.text = recipe.name
        recipeDescription.text = recipe.description

        val totalSteps = recipe.steps.size
        val estimatedTime = recipe.steps.sumOf { step ->
            step.time.split(" ")[0].toInt()
        }

        recipeDetails.text = "Steps: $totalSteps\nEstimated Time: $estimatedTime minutes"
    }

    private fun formatRecipeForSharing(recipe: com.cs407.snapchef.data.Recipe): String {
        val builder = StringBuilder()

        builder.append("Recipe Name: ${recipe.name}\n\n")

        builder.append("Description: ${recipe.description}\n\n")

        builder.append("Steps:\n")
        recipe.steps.forEachIndexed { index, step ->
            builder.append("Step ${index + 1}:\n")
            builder.append("- Description: ${step.step}\n")
            builder.append("- Ingredients: ${step.ingredients.joinToString(", ")}\n")
            builder.append("- Estimated Time: ${step.time}\n\n")
        }

        return builder.toString()
    }


}