package com.example.minitodo.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.example.minitodo.R
import java.text.SimpleDateFormat
import java.util.*

/**
 * 提醒时间设置对话框
 * 
 * 作用：允许用户为待办项设置提醒时间
 * 
 * 功能：
 * 1. 显示日期选择器（DatePickerDialog）
 * 2. 显示时间选择器（TimePickerDialog）
 * 3. 验证选择的时间是否为未来时间
 * 4. 返回格式化的时间字符串（yyyy-MM-dd HH:mm）
 * 
 * 交互流程：
 * 1. 用户点击待办项的提醒按钮
 * 2. MainActivity打开ReminderDialogFragment对话框
 * 3. 用户点击\"选择日期\"按钮选择日期
 * 4. 用户点击\"选择时间\"按钮选择时间
 * 5. 用户点击\"确定\"按钮，验证时间，回调结果
 * 6. 或点击\"取消\"关闭对话框
 * 
 * 时间验证：
 * - 所选时间必须是未来时间（> 当前时间）
 * - 否则显示提示信息
 * 
 * 默认时间：
 * - 初始化为当前时间 + 1小时
 * - 分钟设为0
 */
class ReminderDialogFragment : DialogFragment() {

    /**
     * 时间选择完成的回调接口
     */
    interface OnReminderSetListener {
        fun onReminderSet(dateTime: String)
    }

    // 回调监听器
    private var listener: OnReminderSetListener? = null
    // 用于存储用户选择的日期和时间
    private val calendar = Calendar.getInstance()
    // 时间格式化器（yyyy-MM-dd HH:mm，例：2026-01-15 14:30）
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA)

    /**
     * 设置时间选择完成的回调监听器
     * 
     * @param listener OnReminderSetListener实例
     */
    fun setListener(listener: OnReminderSetListener) {
        this.listener = listener
    }

    /**
     * 创建对话框视图
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_reminder, container, false)
    }

    /**
     * 视图创建完成后的初始化
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val dateButton = view.findViewById<Button>(R.id.btn_select_date)
        val timeButton = view.findViewById<Button>(R.id.btn_select_time)
        val selectedTimeText = view.findViewById<TextView>(R.id.text_selected_time)
        val confirmButton = view.findViewById<Button>(R.id.btn_confirm)
        val cancelButton = view.findViewById<Button>(R.id.btn_cancel)

        // 初始化为当前时间之后的1小时
        calendar.add(Calendar.HOUR_OF_DAY, 1)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        updateTimeDisplay(selectedTimeText)

        // 日期选择按钮
        dateButton.setOnClickListener {
            showDatePicker(selectedTimeText)
        }

        // 时间选择按钮
        timeButton.setOnClickListener {
            showTimePicker(selectedTimeText)
        }

        // 确定按钮：验证时间并回调
        confirmButton.setOnClickListener {
            // 验证选择的时间是否为未来时间
            if (calendar.timeInMillis <= System.currentTimeMillis()) {
                Toast.makeText(
                    requireContext(),
                    "提醒时间必须是未来时间",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }
            // 回调选择的时间
            listener?.onReminderSet(dateFormat.format(calendar.time))
            dismiss()
        }

        // 取消按钮
        cancelButton.setOnClickListener {
            dismiss()
        }
    }

    /**
     * 显示日期选择器
     * 
     * 使用系统DatePickerDialog让用户选择日期
     */
    private fun showDatePicker(selectedTimeText: TextView) {
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        DatePickerDialog(requireContext(), { _, selectedYear, selectedMonth, selectedDay ->
            calendar.set(Calendar.YEAR, selectedYear)
            calendar.set(Calendar.MONTH, selectedMonth)
            calendar.set(Calendar.DAY_OF_MONTH, selectedDay)
            updateTimeDisplay(selectedTimeText)
        }, year, month, day).show()
    }

    /**
     * 显示时间选择器
     * 
     * 使用系统TimePickerDialog让用户选择小时和分钟
     */
    private fun showTimePicker(selectedTimeText: TextView) {
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)

        TimePickerDialog(requireContext(), { _, selectedHour, selectedMinute ->
            calendar.set(Calendar.HOUR_OF_DAY, selectedHour)
            calendar.set(Calendar.MINUTE, selectedMinute)
            updateTimeDisplay(selectedTimeText)
        }, hour, minute, true).show()
    }

    /**
     * 更新时间显示文本
     * 
     * 将calendar中的日期和时间格式化并显示
     */
    private fun updateTimeDisplay(selectedTimeText: TextView) {
        selectedTimeText.text = dateFormat.format(calendar.time)
    }
}
