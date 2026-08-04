package com.example.smartmove.ui.search

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.smartmove.R
import com.example.smartmove.model.BoxResponse
import com.example.smartmove.util.FormatUtils
import com.example.smartmove.util.PriorityChipHelper

class BoxAdapter(
    private var boxes: List<BoxResponse>,
    private val onBoxClick: (BoxResponse) -> Unit
) : RecyclerView.Adapter<BoxAdapter.BoxViewHolder>() {

    class BoxViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvBoxName: TextView = itemView.findViewById(R.id.tvBoxName)
        val tvBoxRoom: TextView = itemView.findViewById(R.id.tvBoxRoom)
        val tvBoxItems: TextView = itemView.findViewById(R.id.tvBoxItems)
        val tvBoxStatus: TextView = itemView.findViewById(R.id.tvBoxStatus)
        val tvBoxPriority: TextView = itemView.findViewById(R.id.tvBoxPriority)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BoxViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_box, parent, false)
        return BoxViewHolder(view)
    }

    override fun onBindViewHolder(holder: BoxViewHolder, position: Int) {
        val box = boxes[position]

        val context = holder.itemView.context
        holder.tvBoxName.text = box.name
        holder.tvBoxRoom.text = context.getString(R.string.details_room, box.destinationRoom)

        val items = box.items.orEmpty()
        val itemsText = if (items.isEmpty()) {
            context.getString(R.string.scan_no_items)
        } else {
            items.take(3).joinToString(", ") + if (items.size > 3) "..." else ""
        }

        holder.tvBoxItems.text = itemsText

        val priority = box.priorityColor
        holder.tvBoxPriority.text = FormatUtils.formatPriority(priority)
        holder.tvBoxStatus.text = FormatUtils.formatStatus(box.status)
        PriorityChipHelper.applyChipStyle(holder.tvBoxPriority, priority, context)

        holder.itemView.setOnClickListener {
            onBoxClick(box)
        }
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