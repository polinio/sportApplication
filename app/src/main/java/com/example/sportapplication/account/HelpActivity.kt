package com.example.sportapplication.account

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.sportapplication.R
import com.example.sportapplication.data.FaqAdapter
import com.example.sportapplication.databinding.ActivityHelpBinding

class HelpActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHelpBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHelpBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Настройка RecyclerView
        binding.faqRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.faqRecyclerView.adapter = FaqAdapter(getFaqItems())
    }

    private fun getFaqItems(): List<FaqItem> {
        return listOf(
            FaqItem(
                question = getString(R.string.faq_question_1),
                answer = getString(R.string.faq_answer_1)
            ),
            FaqItem(
                question = getString(R.string.faq_question_2),
                answer = getString(R.string.faq_answer_2)
            ),
            FaqItem(
                question = getString(R.string.faq_question_3),
                answer = getString(R.string.faq_answer_3)
            ),
            FaqItem(
                question = getString(R.string.faq_question_4),
                answer = getString(R.string.faq_answer_4)
            ),
            FaqItem(
                question = getString(R.string.faq_question_5),
                answer = getString(R.string.faq_answer_5)
            )
        )
    }
}

data class FaqItem(
    val question: String,
    val answer: String,
    var isExpanded: Boolean = false
)