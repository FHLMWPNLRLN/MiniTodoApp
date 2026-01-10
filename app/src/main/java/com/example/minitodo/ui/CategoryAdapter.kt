package com.example.minitodo.ui

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.minitodo.R
import com.example.minitodo.data.CategoryEntity

class CategoryAdapter(
    private val onSelect: (CategoryEntity) -> Unit,
    private val onDelete: (CategoryEntity) -> Unit
) : ListAdapter<CategoryEntity, CategoryAdapter.CategoryViewHolder>(CategoryDiffCallback()) {

    private var selectedCategoryId: Int? = null

    inner class CategoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val categoryName: TextView = itemView.findViewById(R.id.category_name)
        val categoryColor: View = itemView.findViewById(R.id.category_color)
        val deleteBtn: ImageButton = itemView.findViewById(R.id.category_delete)

        fun bind(category: CategoryEntity, isSelected: Boolean) {
            categoryName.text = category.name
            categoryName.isSelected = isSelected
            
            try {
                categoryColor.setBackgroundColor(Color.parseColor(category.color))
            } catch (e: Exception) {
                categoryColor.setBackgroundColor(Color.parseColor("#FF6200EE"))
            }

            itemView.setOnClickListener {
                onSelect(category)
            }

            // Prevent deleting the pseudo 'Uncategorized' item (we use id == 0)
            if (category.id == 0) {
                deleteBtn.visibility = View.GONE
                deleteBtn.setOnClickListener(null)
            } else {
                deleteBtn.visibility = View.VISIBLE
                deleteBtn.setOnClickListener {
                    onDelete(category)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_category, parent, false)
        return CategoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        val category = getItem(position)
        holder.bind(category, category.id == selectedCategoryId)
    }

    fun setSelectedCategory(categoryId: Int?) {
        val oldId = selectedCategoryId
        selectedCategoryId = categoryId
        
        // Refresh the UI for old and new selected items
        currentList.forEachIndexed { index, category ->
            if (category.id == oldId || category.id == selectedCategoryId) {
                notifyItemChanged(index)
            }
        }
    }

    class CategoryDiffCallback : DiffUtil.ItemCallback<CategoryEntity>() {
        override fun areItemsTheSame(oldItem: CategoryEntity, newItem: CategoryEntity): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: CategoryEntity, newItem: CategoryEntity): Boolean {
            return oldItem == newItem
        }
    }
}
