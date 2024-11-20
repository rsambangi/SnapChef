package com.cs407.snapchef

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class MyRecipesActivity : AppCompatActivity()
{
    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_recipes)  // Ensure you have this layout file
    }
}
