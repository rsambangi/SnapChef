package com.cs407.snapchef

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
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


class GenerateRecipeActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_generate_recipe)

        val ingredientsResponse = intent.getStringExtra("ingredientsResponse")

        if (ingredientsResponse != null) {
            val client = OkHttpClient()

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
                    if (response.isSuccessful && responseBody != null) {
                        withContext(Dispatchers.Main) {
                            val loadingLayout = findViewById<ConstraintLayout>(R.id.loadingGenerateRecipeLayout)
                            loadingLayout.visibility = View.GONE

                            val recipeLayout = findViewById<LinearLayout>(R.id.recipeLayout)
                            recipeLayout.visibility = View.VISIBLE

                            val gson = Gson()
                            val parsedResponse = gson.fromJson(responseBody, RecipeResponse::class.java)

                            // TODO: DO SOMETHING WITH PARSED RECIPE OBJECT (parsedResponse.data.recipe)

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
    }
}