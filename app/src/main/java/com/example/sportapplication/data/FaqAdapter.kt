package com.example.sportapplication.data

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.sportapplication.account.FaqItem
import com.example.sportapplication.R
import com.example.sportapplication.databinding.ItemFaqBinding

class FaqAdapter(private val faqItems: List<FaqItem>) :
    RecyclerView.Adapter<FaqAdapter.FaqViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FaqViewHolder {
        val binding = ItemFaqBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return FaqViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FaqViewHolder, position: Int) {
        holder.bind(faqItems[position])
    }

    override fun getItemCount(): Int = faqItems.size

    inner class FaqViewHolder(private val binding: ItemFaqBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: FaqItem) {
            binding.questionText.text = item.question
            binding.answerText.text = item.answer
            binding.answerText.visibility = if (item.isExpanded) View.VISIBLE else View.GONE
            binding.arrowIcon.setImageResource(
                if (item.isExpanded) R.drawable.ic_arrow_up else R.drawable.ic_arrow_down
            )

            binding.root.setOnClickListener {
                item.isExpanded = !item.isExpanded
                notifyItemChanged(adapterPosition)
            }
        }
    }
}