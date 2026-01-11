package com.example.minitodo.ui

import android.app.AlertDialog
import android.os.Bundle
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.example.minitodo.R
import com.example.minitodo.data.CategoryEntity

/**
 * 分类编辑对话框
 * 
 * 作用：允许用户编辑（重命名）分类名称
 * 
 * 功能：
 * 1. 显示当前分类名称
 * 2. 允许用户修改分类名称
 * 3. 验证输入的名称不为空
 * 4. 返回修改后的分类名称
 * 
 * 交互流程：
 * 1. 用户在CategoryAdapter中长按分类项
 * 2. 打开EditCategoryDialogFragment对话框
 * 3. 对话框显示EditText，包含现有分类名称
 * 4. 用户修改名称
 * 5. 点击\"确定\"验证并回调
 * 6. 或点击\"取消\"关闭对话框
 * 
 * 约束条件：
 * - id=0的\"未分类\"不能编辑（在CategoryAdapter中检查）
 * - 分类名称不能为空
 * - 如果新名称与旧名称相同，不执行回调
 */
class EditCategoryDialogFragment : DialogFragment() {

    /**
     * 分类编辑完成的回调接口
     */
    interface OnCategoryEditListener {
        fun onCategoryRenamed(oldCategory: CategoryEntity, newName: String)
    }

    // 回调监听器
    private var listener: OnCategoryEditListener? = null
    // 要编辑的分类对象
    private var category: CategoryEntity? = null

    companion object {
        /**
         * 创建EditCategoryDialogFragment实例
         * 
         * @param category 要编辑的分类对象
         * @param listener 编辑完成的回调监听器
         * @return 新的EditCategoryDialogFragment实例
         */
        fun newInstance(
            category: CategoryEntity,
            listener: OnCategoryEditListener
        ): EditCategoryDialogFragment {
            return EditCategoryDialogFragment().apply {
                this.category = category
                this.listener = listener
            }
        }
    }

    /**
     * 创建对话框
     */
    override fun onCreateDialog(savedInstanceState: Bundle?) = AlertDialog.Builder(requireContext())
        .setTitle("编辑分类")
        .setMessage("输入新的分类名称")
        .apply {
            // 创建输入框，初始化为现有分类名称
            val editText = EditText(requireContext()).apply {
                setText(category?.name)
                selectAll()  // 全选文本便于用户替换
            }
            setView(editText)

            // 确定按钮
            setPositiveButton("确定") { _, _ ->
                val newName = editText.text.toString().trim()
                if (newName.isNotEmpty() && category != null) {
                    // 只有名称确实改变时才回调
                    if (newName != category?.name) {
                        listener?.onCategoryRenamed(category!!, newName)
                    }
                } else {
                    // 名称为空时提示用户
                    Toast.makeText(context, "分类名称不能为空", Toast.LENGTH_SHORT).show()
                }
            }

            // 取消按钮
            setNegativeButton("取消") { _, _ -> }
        }
        .create()
}
