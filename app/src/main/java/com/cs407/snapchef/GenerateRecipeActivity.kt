package com.cs407.snapchef

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
import java.util.concurrent.TimeUnit

class GenerateRecipeActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_generate_recipe)

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
                findViewById<ConstraintLayout>(R.id.loadingGenerateRecipeLayout).visibility = View.VISIBLE
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
            // Placeholder for the "Save" button functionality
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
                        val loadingLayout = findViewById<ConstraintLayout>(R.id.loadingGenerateRecipeLayout)
                        loadingLayout.visibility = View.GONE

                        val recipeLayout = findViewById<LinearLayout>(R.id.recipeLayout)
                        recipeLayout.visibility = View.VISIBLE


                        val gson = Gson()
                        val parsedResponse = gson.fromJson(responseBody, RecipeResponse::class.java)
                        print("Recipe Generated")
                        print(parsedResponse.data.recipe.name)
                        val recipe = parsedResponse.data.recipe

                        displayRecipe(recipe)
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
                text = "Step ${index + 1}: ${step.step}\nIngredients: ${step.ingredients.joinToString(", ")}\nTime: ${step.time}"
                setPadding(0, 0, 0, 16) // Bottom padding
            }
            recipeLayout.addView(stepTextView)
        }

        // Show the ScrollView
        findViewById<ScrollView>(R.id.recipeScrollView).visibility = View.VISIBLE
        val buttonLayout = findViewById<LinearLayout>(R.id.buttonLinearLayout)
        buttonLayout.visibility = View.VISIBLE
    }


}
