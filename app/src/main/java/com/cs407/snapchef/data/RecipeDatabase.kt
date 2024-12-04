package com.cs407.snapchef.data

import android.content.Context
import androidx.paging.PagingSource
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.Upsert
import com.cs407.snapchef.R
import java.util.Date


@Entity(tableName = "recipes")
data class Recipe(
    @PrimaryKey(autoGenerate = true) val recipeId: Int = 0,
    val name: String,
    val description: String,
    val recipeSummary: String,
    @TypeConverters(StepConverter::class) val steps: List<Step>
)

data class Step(
    val step: String,
    val ingredients: List<String>,
    val time: String
)

class StepConverter {
    @TypeConverter
    fun fromStepList(steps: List<Step>): String {
        val gson = Gson()
        return gson.toJson(steps)
    }

    @TypeConverter
    fun toStepList(data: String): List<Step> {
        val gson = Gson()
        val type = object : TypeToken<List<Step>>() {}.type
        return gson.fromJson(data, type)
    }
}

data class RecipeSummary(
    val recipeId: Int, // Id of the note
    val name: String, // Title of the note
    val recipeSummary: String, // Summary of the note
)

@Dao
interface RecipeDao {
    // Insert a recipe
    @Insert(entity = Recipe::class)
    suspend fun insertRecipe(recipe: Recipe)

    // Get a specific recipe by ID
    @Query("SELECT * FROM recipes WHERE recipeId = :id")
    suspend fun getRecipeById(id: Int): Recipe?

    // Same query but returns a PagingSource for pagination
    @Query("SELECT * FROM recipes")
    fun getPaginatedRecipes(): PagingSource<Int, RecipeSummary>

    // Delete a specific recipe
    @Query("DELETE FROM recipes WHERE recipeId = :recipeId")
    suspend fun deleteRecipe(recipeId: Int)


}

@Database(entities = [Recipe::class], version = 1)
@TypeConverters(StepConverter::class)
abstract class RecipeDatabase : RoomDatabase() {
    abstract fun recipeDao(): RecipeDao

    companion object {
        // Singleton prevents multiple instances of database opening at the same time.
        @Volatile
        private var INSTANCE: RecipeDatabase? = null

        // Get or create the database instance
        fun getDatabase(context: Context): RecipeDatabase {
            // if the INSTANCE is not null, then return it,
            // if it is, then create the database
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    RecipeDatabase::class.java,
                    context.getString(R.string.recipe_database), // Database name from resources
                ).build()
                INSTANCE = instance
                // return instance
                instance
            }
        }
    }
}