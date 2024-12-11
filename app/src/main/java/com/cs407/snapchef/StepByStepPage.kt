package com.cs407.snapchef

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.cs407.snapchef.data.RecipeDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class StepByStepPage : AppCompatActivity() {

    private lateinit var recipeDB: RecipeDatabase
    private lateinit var recipe: Recipe

    private var currentStepIndex: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.step_by_step)

        recipeDB = RecipeDatabase.getDatabase(this@StepByStepPage)

        val id = intent.getIntExtra("recipeId", 0)

        CoroutineScope(Dispatchers.IO).launch {
            val tempRecipe = recipeDB.recipeDao().getRecipeById(id)!!

            val newSteps = mutableListOf<Step>()
            tempRecipe.steps.forEach { step ->
                newSteps.add(Step(step.ingredients, step.step, step.time))
            }

            recipe = Recipe(name = tempRecipe.name,
                description = tempRecipe.description,
                steps = newSteps)

            withContext(Dispatchers.Main) { displayStep()}
        }

        findViewById<Button>(R.id.nextButton).setOnClickListener {
            currentStepIndex++
            displayStep()
        }

        findViewById<Button>(R.id.prevButton).setOnClickListener {
            currentStepIndex--
            displayStep()
        }

        findViewById<Button>(R.id.returnHomeButton).setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun displayStep() {
        val recipeTitle = findViewById<TextView>(R.id.recipeTitle)
        val stepTitle = findViewById<TextView>(R.id.stepTitle)
        val stepDescription = findViewById<TextView>(R.id.stepDescription)
        val stepIngredients = findViewById<TextView>(R.id.stepIngredients)
        val stepTime = findViewById<TextView>(R.id.stepTime)

        val prevButton = findViewById<Button>(R.id.prevButton)
        val nextButton = findViewById<Button>(R.id.nextButton)
        val returnHomeButton = findViewById<Button>(R.id.returnHomeButton)

        val currentStep = recipe.steps[currentStepIndex]

        // Updating the UI with step data
        recipeTitle.text = recipe.name
        stepTitle.text = "Step ${currentStepIndex + 1} of ${recipe.steps.size}"
        stepDescription.text = currentStep.step
        stepIngredients.text = "Ingredients: ${currentStep.ingredients.joinToString(", ")}"
        stepTime.text = "Estimated Time: ${currentStep.time}"

        // Manage button visibility whenever a user wants to go from page to page
        // Changing it based on currentStepIndea
        prevButton.visibility = if (currentStepIndex == 0) View.GONE else View.VISIBLE
        nextButton.visibility = if (currentStepIndex == recipe.steps.size - 1) View.GONE else View.VISIBLE
        returnHomeButton.visibility = if (currentStepIndex == recipe.steps.size - 1) View.VISIBLE else View.GONE
    }
}
