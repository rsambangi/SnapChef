package com.cs407.snapchef

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okio.IOException
import org.json.JSONObject
import java.io.File

class RecyclerAdapter(private val itemList: List<Ingredient>) : RecyclerView.Adapter<RecyclerAdapter.MyViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.ingredient_item, parent, false)
        return MyViewHolder(view)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val item = itemList[position]
        holder.nameTextView.text = item.name
        holder.quantityTextView.text = item.quantity.toString()
        holder.unitTextView.text = item.unit
    }

    override fun getItemCount(): Int = itemList.size

    inner class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nameTextView = itemView.findViewById<TextView>(R.id.name)
        val quantityTextView = itemView.findViewById<TextView>(R.id.quantity)
        val unitTextView = itemView.findViewById<TextView>(R.id.unit)
    }
}

class IngredientsConfirmActivity : AppCompatActivity()
{
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: RecyclerAdapter
    private lateinit var ingredientsList: List<Ingredient>
    private lateinit var responseText: String

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ingredients_confirm)

        recyclerView = findViewById<RecyclerView>(R.id.ingredientsRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this@IngredientsConfirmActivity)

        val dataImagePath = intent.getStringExtra("dataImagePath")

        val generateRecipeButton = findViewById<Button>(R.id.generateRecipeButton)
        generateRecipeButton.setOnClickListener {
            val gson = Gson()
            val intent = Intent(this, GenerateRecipeActivity::class.java)
            intent.putExtra("ingredientsResponse", responseText)
            startActivity(intent)
        }

        if (dataImagePath != null) {
            val file = File(dataImagePath)
            val dataImage = file.readText()

            val client = OkHttpClient()

            val jsonData = JSONObject()
            jsonData.put("imageUrl", dataImage)

            val jsonBody = JSONObject()
            jsonBody.put("data", jsonData)

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val body = jsonBody.toString().toRequestBody(mediaType)

            val request = Request.Builder()
                .url(getString(R.string.api_endpoint) + "/identify")
                .addHeader("X-Api-Key", getString(R.string.api_key))
                .post(body)
                .build()

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val response = client.newCall(request).execute()

                    val responseBody = response.body?.string()
                    if (response.isSuccessful && responseBody != null) {
                        withContext(Dispatchers.Main) {
                            val loadingLayout = findViewById<ConstraintLayout>(R.id.loadingIdentifyLayout)
                            loadingLayout.visibility = View.GONE

                            val confirmLayout = findViewById<LinearLayout>(R.id.confirmIngredientsLayout)
                            confirmLayout.visibility = View.VISIBLE

                            val gson = Gson()
                            val parsedResponse = gson.fromJson(responseBody, IngredientsResponse::class.java)

                            ingredientsList = parsedResponse.data.ingredients
                            responseText = responseBody

                            adapter = RecyclerAdapter(ingredientsList)
                            recyclerView.adapter = adapter
                        }
                    } else {
                        Log.i("POST", "Request failed: ${response.body?.string()}")
                    }
                } catch (e: IOException) {
                    Log.i("ERROR", e.stackTrace.toString())
                }
            }
        } else {
            Toast.makeText(this@IngredientsConfirmActivity, "Error, please try a different image!", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, CameraPage::class.java)
            startActivity(intent)
        }
    }
}
