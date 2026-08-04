package com.example.smartmove.util

import android.content.Context
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.example.smartmove.R

object PriorityChipHelper {

    fun applyChipStyle(textView: TextView, priority: String, context: Context) {
        when (priority.lowercase()) {
            "red" -> {
                textView.setBackgroundResource(R.drawable.bg_priority_red)
                textView.setTextColor(ContextCompat.getColor(context, R.color.priority_red_text))
            }
            "yellow" -> {
                textView.setBackgroundResource(R.drawable.bg_priority_yellow)
                textView.setTextColor(ContextCompat.getColor(context, R.color.priority_yellow_text))
            }
            "green" -> {
                textView.setBackgroundResource(R.drawable.bg_priority_green)
                textView.setTextColor(ContextCompat.getColor(context, R.color.priority_green_text))
            }
            else -> {
                textView.setBackgroundResource(R.drawable.bg_chip_soft)
                textView.setTextColor(ContextCompat.getColor(context, R.color.smartmove_text_primary))
            }
        }
    }
}
