package com.cs407.snapchef

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.lifecycle.lifecycleScope
import com.cs407.snapchef.data.RecipeDatabase
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.Calendar
import java.util.concurrent.TimeUnit

class GenerateRecipeActivity : AppCompatActivity() {

    private lateinit var generatedRecipe: Recipe
    private lateinit var recipeDB: RecipeDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_generate_recipe)

        recipeDB = RecipeDatabase.getDatabase(this@GenerateRecipeActivity)

        print("Check Before Intent")
        val ingredientsResponse = intent.getStringExtra("ingredientsResponse")
        print("Check After Intent")

        if (ingredientsResponse != null) {
            print("Generating Recipe")
            generateRecipe(ingredientsResponse)
        }

        // Set up the "Regenerate", "Start", and "Save" button actions
        findViewById<Button>(R.id.regenerateButton).setOnClickListener {
            val ingredients = intent.getStringExtra("ingredientsResponse")
            if (ingredients != null) {
                findViewById<ConstraintLayout>(R.id.loadingGenerateRecipeLayout).visibility =
                    View.VISIBLE
                findViewById<LinearLayout>(R.id.recipeLayout).visibility = View.GONE
                val buttonLayout = findViewById<LinearLayout>(R.id.buttonLinearLayout)
                buttonLayout.visibility = View.GONE
                generateRecipe(ingredients)
            }
        }

        findViewById<Button>(R.id.startButton).setOnClickListener {
            // Placeholder for the "Start" button functionality
        }

        findViewById<Button>(R.id.saveButton).setOnClickListener {
            saveContent()
            val intent = Intent(this, MyRecipesActivity::class.java)
            startActivity(intent)
        }
    }

    private fun generateRecipe(ingredientsResponse: String) {
        val client = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val body = ingredientsResponse.toRequestBody(mediaType)

        val request = Request.Builder()
            .url(getString(R.string.api_endpoint) + "/recipe")
            .addHeader("X-Api-Key", getString(R.string.api_key))
            .post(body)
            .build()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = client.newCall(request).execute()

                val responseBody = response.body?.string()
                print("Client call successful")
                if (response.isSuccessful && responseBody != null) {
                    withContext(Dispatchers.Main) {
                        val loadingLayout =
                            findViewById<ConstraintLayout>(R.id.loadingGenerateRecipeLayout)
                        loadingLayout.visibility = View.GONE

                        val recipeLayout = findViewById<LinearLayout>(R.id.recipeLayout)
                        recipeLayout.visibility = View.VISIBLE


                        val gson = Gson()
                        val parsedResponse = gson.fromJson(responseBody, RecipeResponse::class.java)
                        generatedRecipe = parsedResponse.data.recipe

                        displayRecipe(generatedRecipe)
                        Log.i("POST", responseBody)
                    }
                } else {
                    Log.i("POST", "Request failed: ${response.body?.string()}")
                }
            } catch (e: IOException) {
                Log.i("ERROR", e.stackTrace.toString())
            }
        }
    }

    private fun displayRecipe(recipe: Recipe) {
        val titleTextView = findViewById<TextView>(R.id.recipeTitle)
        val descriptionTextView = findViewById<TextView>(R.id.recipeDescription)
        val recipeLayout = findViewById<LinearLayout>(R.id.recipeLayout)

        // Clear previous content
        recipeLayout.removeAllViews()

        // Set title and description
        titleTextView.text = recipe.name
        descriptionTextView.text = recipe.description

        // Add Steps
        recipe.steps.forEachIndexed { index, step ->
            // Step title
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

            // Optional divider line between steps
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

        // Show the ScrollView and Buttons now that recipe is ready
        findViewById<ScrollView>(R.id.recipeScrollView).visibility = View.VISIBLE
        findViewById<LinearLayout>(R.id.buttonLinearLayout).visibility = View.VISIBLE
        findViewById<ConstraintLayout>(R.id.loadingGenerateRecipeLayout).visibility = View.GONE
    }


    private fun saveContent() {

        // TODO: Launch a coroutine to save the note in the background (non-UI thread)
        lifecycleScope.launch {

            withContext(Dispatchers.IO) {

                val newSteps = mutableListOf<com.cs407.snapchef.data.Step>()
                generatedRecipe.steps.forEach { step ->
                    newSteps.add(com.cs407.snapchef.data.Step(step.step, step.ingredients, step.time))
                }

                val inputRecipe = com.cs407.snapchef.data.Recipe(name = generatedRecipe.name,
                    description = generatedRecipe.description,
                    recipeSummary = generatedRecipe.description.take(43) + "...",
                    steps = newSteps)

                recipeDB.recipeDao().insertRecipe(inputRecipe)
            }
        }
    }
}
