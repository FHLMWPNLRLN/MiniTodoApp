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

class ReminderDialogFragment : DialogFragment() {

    interface OnReminderSetListener {
        fun onReminderSet(dateTime: String)
    }

    private var listener: OnReminderSetListener? = null
    private val calendar = Calendar.getInstance()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA)

    fun setListener(listener: OnReminderSetListener) {
        this.listener = listener
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_reminder, container, false)
    }

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

        dateButton.setOnClickListener {
            showDatePicker(selectedTimeText)
        }

        timeButton.setOnClickListener {
            showTimePicker(selectedTimeText)
        }

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
            listener?.onReminderSet(dateFormat.format(calendar.time))
            dismiss()
        }

        cancelButton.setOnClickListener {
            dismiss()
        }
    }

    private fun showDatePicker(selectedTimeText: TextView) {
        // 获取最小日期（今天）
        val now = Calendar.getInstance()
        val minDate = now.timeInMillis
        
        DatePickerDialog(
            requireContext(),
            { _, year, month, day ->
                calendar.set(Calendar.YEAR, year)
                calendar.set(Calendar.MONTH, month)
                calendar.set(Calendar.DAY_OF_MONTH, day)
                updateTimeDisplay(selectedTimeText)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).apply {
            // 设置最小日期为今天
            datePicker.minDate = minDate
        }.show()
    }

    private fun showTimePicker(selectedTimeText: TextView) {
        TimePickerDialog(
            requireContext(),
            { _, hour, minute ->
                calendar.set(Calendar.HOUR_OF_DAY, hour)
                calendar.set(Calendar.MINUTE, minute)
                updateTimeDisplay(selectedTimeText)
            },
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            true
        ).show()
    }

    private fun updateTimeDisplay(textView: TextView) {
        textView.text = "提醒时间：${dateFormat.format(calendar.time)}"
    }
}
