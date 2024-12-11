package com.cs407.snapchef

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
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
import java.io.IOException
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
                generateRecipe(ingredients)
            }
        }

        findViewById<Button>(R.id.myRecipesButton).setOnClickListener {
            val intent = Intent(this, MyRecipesActivity::class.java)
            startActivity(intent)
        }

        findViewById<Button>(R.id.saveButton).setOnClickListener {
            saveContent()
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
        val recipeTitle = findViewById<TextView>(R.id.recipeTitle)
        val recipeDescription = findViewById<TextView>(R.id.recipeDescription)
        val recipeDetails = findViewById<TextView>(R.id.recipeDetails)

        recipeTitle.text = recipe.name
        recipeDescription.text = recipe.description

        val totalSteps = recipe.steps.size
        val estimatedTime = recipe.steps.sumOf { step ->
            step.time.split(" ")[0].toInt() // Pranshu add time format
        }

        recipeDetails.text = "Steps: $totalSteps\nEstimated Time: $estimatedTime minutes"
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
