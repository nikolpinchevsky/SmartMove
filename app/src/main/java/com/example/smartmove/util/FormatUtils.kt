package com.example.smartmove.util

import android.content.Context
import com.example.smartmove.R

object FormatUtils {

    fun formatStatus(raw: String?): String =
        raw.orEmpty()
            .split("_")
            .joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }

    fun formatPriority(raw: String?): String =
        raw.orEmpty().replaceFirstChar { it.uppercase() }

    fun yesNo(value: Boolean, context: Context): String =
        context.getString(if (value) R.string.label_yes else R.string.label_no)

    fun parseItemsList(text: String): List<String> =
        if (text.isEmpty()) emptyList()
        else text.split(",").map { it.trim() }.filter { it.isNotEmpty() }
}
