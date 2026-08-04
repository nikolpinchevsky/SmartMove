package com.example.smartmove.ui.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.smartmove.R
import com.example.smartmove.model.BoxResponse
import com.example.smartmove.util.FormatUtils
import com.example.smartmove.util.PriorityChipHelper

class PriorityBoxAdapter(
    private var boxes: List<BoxResponse>,
    private val onBoxClick: (BoxResponse) -> Unit
) : RecyclerView.Adapter<PriorityBoxAdapter.PriorityBoxViewHolder>() {

    class PriorityBoxViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val viewPriorityLine: View = itemView.findViewById(R.id.viewPriorityLine)
        val ivPriorityBoxIcon: AppCompatImageView = itemView.findViewById(R.id.ivPriorityBoxIcon)
        val tvBoxName: TextView = itemView.findViewById(R.id.tvPriorityBoxName)
        val tvBoxRoom: TextView = itemView.findViewById(R.id.tvPriorityBoxRoom)
        val tvPriorityChip: TextView = itemView.findViewById(R.id.tvPriorityChip)
        val tvPriorityStatus: TextView = itemView.findViewById(R.id.tvPriorityStatus)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PriorityBoxViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_priority_box, parent, false)
        return PriorityBoxViewHolder(view)
    }

    override fun onBindViewHolder(holder: PriorityBoxViewHolder, position: Int) {
        val box = boxes[position]
        val ctx = holder.itemView.context

        holder.tvBoxName.text = box.name
        holder.tvBoxRoom.text = ctx.getString(R.string.details_room, box.destinationRoom)
        val priority = box.priorityColor
        holder.tvPriorityChip.text = FormatUtils.formatPriority(priority)
        holder.tvPriorityStatus.text = FormatUtils.formatStatus(box.status)

        when (priority.lowercase()) {
            "red" -> {
                holder.viewPriorityLine.setBackgroundColor(ContextCompat.getColor(ctx, R.color.priority_accent_red))
                holder.ivPriorityBoxIcon.setImageResource(R.drawable.red_box)
            }
            "yellow" -> {
                holder.viewPriorityLine.setBackgroundColor(ContextCompat.getColor(ctx, R.color.priority_accent_yellow))
                holder.ivPriorityBoxIcon.setImageResource(R.drawable.yellow_box)
            }
            "green" -> {
                holder.viewPriorityLine.setBackgroundColor(ContextCompat.getColor(ctx, R.color.priority_accent_green))
                holder.ivPriorityBoxIcon.setImageResource(R.drawable.green_box)
            }
            else -> {
                holder.viewPriorityLine.setBackgroundColor(ContextCompat.getColor(ctx, R.color.smartmove_divider))
                holder.ivPriorityBoxIcon.setImageResource(R.drawable.red_box)
            }
        }
        PriorityChipHelper.applyChipStyle(holder.tvPriorityChip, priority, ctx)

        holder.itemView.setOnClickListener { onBoxClick(box) }
    }

    override fun getItemCount(): Int = boxes.size

    fun updateData(newBoxes: List<BoxResponse>) {
        val oldSize = boxes.size
        val newSize = newBoxes.size
        boxes = newBoxes
        if (newSize > oldSize) {
            notifyItemRangeChanged(0, oldSize)
            notifyItemRangeInserted(oldSize, newSize - oldSize)
        } else if (newSize < oldSize) {
            notifyItemRangeChanged(0, newSize)
            notifyItemRangeRemoved(newSize, oldSize - newSize)
        } else {
            notifyItemRangeChanged(0, newSize)
        }
    }
}
