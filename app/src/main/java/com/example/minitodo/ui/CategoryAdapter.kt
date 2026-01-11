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

/**
 * 分类列表适配器
 * 
 * 作用：管理分类RecyclerView的数据展示和交互
 * 
 * 功能：
 * 1. 显示所有分类列表
 * 2. 支持分类选择（高亮显示）
 * 3. 支持删除分类（id=0的未分类除外）
 * 4. 支持编辑分类名称（长按）
 * 5. 显示分类的颜色标记
 * 
 * 设计模式：
 * - 继承ListAdapter：自动使用DiffUtil处理数据更新
 * - ViewHolder模式：高效管理视图
 * - 回调接口：处理用户交互
 * 
 * 交互流程：
 * - 点击分类：调用onSelect()过滤待办项
 * - 删除分类：调用onDelete()删除并级联处理待办项
 * - 长按编辑：打开EditCategoryDialogFragment修改名称
 * 
 * 特殊逻辑：
 * - id=0是\"未分类\"分类，不能删除
 * - 选中状态通过selectedCategoryId跟踪
 * - 支持颜色解析异常处理（使用默认颜色）
 */
class CategoryAdapter(
    // 点击分类时的回调
    private val onSelect: (CategoryEntity) -> Unit,
    // 删除分类时的回调
    private val onDelete: (CategoryEntity) -> Unit,
    // 编辑分类名称时的回调（可选）
    private val onRename: (CategoryEntity, String) -> Unit = { _, _ -> }
) : ListAdapter<CategoryEntity, CategoryAdapter.CategoryViewHolder>(CategoryDiffCallback()) {

    // 当前选中的分类ID
    private var selectedCategoryId: Int? = null

    /**
     * 分类项的ViewHolder
     * 
     * 职责：
     * 1. 管理单个分类项的UI控件
     * 2. 绑定分类数据到UI
     * 3. 处理用户交互（点击、长按、删除）
     */
    inner class CategoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // 分类名称标签
        val categoryName: TextView = itemView.findViewById(R.id.category_name)
        // 分类颜色点
        val categoryColor: View = itemView.findViewById(R.id.category_color)
        // 删除按钮
        val deleteBtn: ImageButton = itemView.findViewById(R.id.category_delete)

        /**
         * 绑定分类数据到UI
         * 
         * @param category 分类数据对象
         * @param isSelected 是否为选中状态
         */
        fun bind(category: CategoryEntity, isSelected: Boolean) {
            // 设置分类名称
            categoryName.text = category.name
            // 设置选中状态（改变UI外观，如文字加粗）
            categoryName.isSelected = isSelected
            
            // 设置分类颜色
            try {
                categoryColor.setBackgroundColor(Color.parseColor(category.color))
            } catch (e: Exception) {
                // 颜色解析失败时使用默认紫色
                categoryColor.setBackgroundColor(Color.parseColor("#FF6200EE"))
            }

            // 点击分类时的处理
            itemView.setOnClickListener {
                onSelect(category)
            }

            // 长按编辑分类名称
            itemView.setOnLongClickListener {
                if (category.id != 0) {  // 不允许编辑\"未分类\"
                    val dialog = EditCategoryDialogFragment.newInstance(
                        category,
                        object : EditCategoryDialogFragment.OnCategoryEditListener {
                            override fun onCategoryRenamed(oldCategory: CategoryEntity, newName: String) {
                                onRename(oldCategory, newName)
                            }
                        }
                    )
                    // 需要从 Fragment 中调用 show()
                    true
                } else {
                    false
                }
            }

            // 删除按钮处理
            // id=0是特殊的\"未分类\"分类，不能删除
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

    /**
     * 创建ViewHolder
     * 
     * 在RecyclerView需要新的ViewHolder时调用
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_category, parent, false)
        return CategoryViewHolder(view)
    }

    /**
     * 绑定数据到ViewHolder
     * 
     * 绑定位置pos处的分类数据
     */
    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        val category = getItem(position)
        holder.bind(category, category.id == selectedCategoryId)
    }

    /**
     * 设置选中的分类
     * 
     * 功能：更新UI中的选中标记
     * 
     * @param categoryId 要选中的分类ID，null表示取消选中
     */
    fun setSelectedCategory(categoryId: Int?) {
        val oldId = selectedCategoryId
        selectedCategoryId = categoryId
        
        // 只刷新受影响的项（旧选中项和新选中项）
        currentList.forEachIndexed { index, category ->
            if (category.id == oldId || category.id == selectedCategoryId) {
                notifyItemChanged(index)
            }
        }
    }

    /**
     * DiffUtil回调，用于计算列表差异
     * 
     * ListAdapter使用此回调来确定：
     * 1. 哪些项新增/删除/移动（areItemsTheSame）
     * 2. 哪些项内容变化（areContentsTheSame）
     * 
     * 然后自动计算最小化的更新操作
     */
    class CategoryDiffCallback : DiffUtil.ItemCallback<CategoryEntity>() {
        /**
         * 检查两个项是否相同（根据ID）
         */
        override fun areItemsTheSame(oldItem: CategoryEntity, newItem: CategoryEntity): Boolean {
            return oldItem.id == newItem.id
        }

        /**
         * 检查两个项的内容是否相同
         */
        override fun areContentsTheSame(oldItem: CategoryEntity, newItem: CategoryEntity): Boolean {
            return oldItem == newItem
        }
    }
}
