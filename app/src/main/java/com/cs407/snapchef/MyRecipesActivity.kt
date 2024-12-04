package com.cs407.snapchef

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cs407.snapchef.data.RecipeDatabase
import com.cs407.snapchef.data.RecipeSummary
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MyRecipesActivity : AppCompatActivity()
{

    private lateinit var adapter: RecipeAdapter
    private lateinit var recipeRecyclerView: RecyclerView
    private var deleteIt: Boolean = false
    private lateinit var recipeToDelete: RecipeSummary
    private lateinit var recipeDB: RecipeDatabase

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_recipes)  // Ensure you have this layout file
        recipeDB = RecipeDatabase.getDatabase(this@MyRecipesActivity)

        recipeRecyclerView = findViewById(R.id.recipeRecyclerView)

        adapter = RecipeAdapter(
            onClick = { recipeId ->
                // Navigate to recipe preview page with recipeID passed into the intent
                val intent = Intent(this, RecipePreviewActivity::class.java)
                intent.putExtra("recipeId", recipeId)
                startActivity(intent)
            },
            onLongClick = { recipeSummary ->
                deleteIt = true
                recipeToDelete = recipeSummary
                showDeleteBottomSheet()
            }
        )

        recipeRecyclerView.layoutManager = LinearLayoutManager(this)
        recipeRecyclerView.adapter = adapter

        loadRecipes()
    }

    private fun loadRecipes() {

        // TODO: Set up paging configuration with a specified page size and prefetch distance
        val flow = Pager(
            PagingConfig(pageSize = 5, prefetchDistance = 5)
        ) {
            recipeDB.recipeDao().getPaginatedRecipes()
        }.flow
            .cachedIn(lifecycleScope)

        // TODO: Launch a coroutine to collect the paginated flow and submit it to the RecyclerView adapter
        lifecycleScope.launch {
            flow.collectLatest { pagingData ->  // Collect the paginated data
                adapter.submitData(pagingData)  // Submit the collected PagingData to the adapter
            }

        }
    }
    private fun showDeleteBottomSheet() {
        if (deleteIt) {
            val bottomSheetDialog = BottomSheetDialog(this@MyRecipesActivity)
            val bottomSheetView = layoutInflater.inflate(R.layout.bottom_sheet_delete, null)
            bottomSheetDialog.setContentView(bottomSheetView)

            val deleteButton = bottomSheetView.findViewById<Button>(R.id.deleteButton)
            val cancelButton = bottomSheetView.findViewById<Button>(R.id.cancelButton)
            val deletePrompt = bottomSheetView.findViewById<TextView>(R.id.deletePrompt)

            deletePrompt.text = "Delete Recipe: ${recipeToDelete.name}"

            deleteButton.setOnClickListener {
                lifecycleScope.launch {
                    withContext(Dispatchers.IO){
                        recipeDB.recipeDao().deleteRecipe(recipeToDelete.recipeId)
                    }
                }


                // TODO: Reset any flags or variables that control the delete state
                deleteIt = false // Example of resetting a flag after deletion

                // TODO: Dismiss the bottom sheet dialog after the deletion is completed
                bottomSheetDialog.dismiss()

                // TODO: Reload the list of notes to reflect the deleted note (e.g., refresh UI)
                loadRecipes() // Implement the function to refresh or reload the notes
            }

            cancelButton.setOnClickListener {
                deleteIt = false
                bottomSheetDialog.dismiss()
            }

            bottomSheetDialog.setOnDismissListener {
                deleteIt = false
            }

            bottomSheetDialog.show()
        }
    }

}
