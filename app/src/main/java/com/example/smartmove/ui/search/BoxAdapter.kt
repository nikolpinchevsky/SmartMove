package com.example.smartmove.ui.search

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.smartmove.R
import com.example.smartmove.model.BoxResponse

class BoxAdapter(
    private var boxes: List<BoxResponse>,
    private val onBoxClick: (BoxResponse) -> Unit
) : RecyclerView.Adapter<BoxAdapter.BoxViewHolder>() {

    class BoxViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvBoxName: TextView = itemView.findViewById(R.id.tvBoxName)
        val tvBoxRoom: TextView = itemView.findViewById(R.id.tvBoxRoom)
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

        holder.tvBoxName.text = box.name
        holder.tvBoxRoom.text = "Room: ${box.destination_room}"
        holder.tvBoxPriority.text = box.priority_color.replaceFirstChar { it.uppercase() }
        holder.tvBoxStatus.text = box.status.replaceFirstChar { it.uppercase() }

        when (box.priority_color.lowercase()) {
            "red" -> holder.tvBoxPriority.setBackgroundResource(R.drawable.bg_priority_red)
            "yellow" -> holder.tvBoxPriority.setBackgroundResource(R.drawable.bg_priority_yellow)
            "green" -> holder.tvBoxPriority.setBackgroundResource(R.drawable.bg_priority_green)
            else -> holder.tvBoxPriority.setBackgroundResource(R.drawable.bg_chip_soft)
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