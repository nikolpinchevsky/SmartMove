package com.example.smartmove.ui.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.smartmove.R
import com.example.smartmove.model.BoxResponse

class PriorityBoxAdapter(
    private var boxes: List<BoxResponse>,
    private val onBoxClick: (BoxResponse) -> Unit
) : RecyclerView.Adapter<PriorityBoxAdapter.PriorityBoxViewHolder>() {

    class PriorityBoxViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
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

        holder.tvBoxName.text = box.name
        holder.tvBoxRoom.text = "Room: ${box.destination_room}"
        holder.tvPriorityChip.text = box.priority_color.replaceFirstChar { it.uppercase() }
        holder.tvPriorityStatus.text = box.status.replaceFirstChar { it.uppercase() }

        when (box.priority_color.lowercase()) {
            "red" -> holder.tvPriorityChip.setBackgroundResource(R.drawable.bg_priority_red)
            "yellow" -> holder.tvPriorityChip.setBackgroundResource(R.drawable.bg_priority_yellow)
            "green" -> holder.tvPriorityChip.setBackgroundResource(R.drawable.bg_priority_green)
            else -> holder.tvPriorityChip.setBackgroundResource(R.drawable.bg_chip_soft)
        }

        holder.itemView.setOnClickListener {
            onBoxClick(box)
        }
    }

    override fun getItemCount(): Int = boxes.size

    fun updateData(newBoxes: List<BoxResponse>) {
        boxes = newBoxes
        notifyDataSetChanged()
    }
}