package com.cs407.snapchef

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.cs407.snapchef.data.RecipeSummary
import java.util.*

class RecipeAdapter(
    private val onClick: (Int) -> Unit,
    private val onLongClick: (RecipeSummary) -> Unit
) : androidx.paging.PagingDataAdapter<RecipeSummary, RecipeAdapter.RecipeViewHolder>(RECIPE_COMPARATOR) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecipeViewHolder {
        val itemView =
            LayoutInflater.from(parent.context).inflate(R.layout.recipe_item, parent, false)
        return RecipeViewHolder(itemView, onClick, onLongClick)
    }

    override fun onBindViewHolder(holder: RecipeViewHolder, position: Int) {
        val recipeSummary = getItem(position)
        if (recipeSummary != null) {
            holder.bind(recipeSummary)
        }
    }

    class RecipeViewHolder(
        itemView: View,
        private val onClick: (Int) -> Unit,
        private val onLongClick: (RecipeSummary) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        private val recipeTitle: TextView = itemView.findViewById(R.id.titleTextView)
        private val recipeAbstract: TextView = itemView.findViewById(R.id.abstractTextView)

        fun bind(recipeSummary: RecipeSummary) {
            recipeTitle.text = recipeSummary.name
            recipeAbstract.text = recipeSummary.recipeSummary

            itemView.setOnClickListener {
                onClick(recipeSummary.recipeId)
            }
            itemView.setOnLongClickListener {
                onLongClick(recipeSummary)
                true
            }
        }
    }

    companion object {
        private val RECIPE_COMPARATOR = object : DiffUtil.ItemCallback<RecipeSummary>() {
            override fun areItemsTheSame(oldItem: RecipeSummary, newItem: RecipeSummary): Boolean =
                oldItem.recipeId == newItem.recipeId

            override fun areContentsTheSame(oldItem: RecipeSummary, newItem: RecipeSummary): Boolean =
                oldItem == newItem
        }
    }
}