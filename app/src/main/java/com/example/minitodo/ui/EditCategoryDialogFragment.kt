package com.example.minitodo.ui

import android.app.AlertDialog
import android.os.Bundle
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.example.minitodo.R
import com.example.minitodo.data.CategoryEntity

class EditCategoryDialogFragment : DialogFragment() {

    interface OnCategoryEditListener {
        fun onCategoryRenamed(oldCategory: CategoryEntity, newName: String)
    }

    private var listener: OnCategoryEditListener? = null
    private var category: CategoryEntity? = null

    companion object {
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

    override fun onCreateDialog(savedInstanceState: Bundle?) = AlertDialog.Builder(requireContext())
        .setTitle("编辑分类")
        .setMessage("输入新的分类名称")
        .apply {
            val editText = EditText(requireContext()).apply {
                setText(category?.name)
                selectAll()
            }
            setView(editText)

            setPositiveButton("确定") { _, _ ->
                val newName = editText.text.toString().trim()
                if (newName.isNotEmpty() && category != null) {
                    if (newName != category?.name) {
                        listener?.onCategoryRenamed(category!!, newName)
                    }
                } else {
                    Toast.makeText(context, "分类名称不能为空", Toast.LENGTH_SHORT).show()
                }
            }

            setNegativeButton("取消") { _, _ -> }
        }
        .create()
}
