package com.example.sportapplication.workout

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Spinner
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.sportapplication.R
import com.example.sportapplication.data.Interval
import com.example.sportapplication.data.IntervalType
import com.example.sportapplication.databinding.ActivityCreateIntervalWorkoutBinding
import android.text.Editable
import android.text.TextWatcher
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.Toast

class CreateIntervalWorkoutActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCreateIntervalWorkoutBinding
    private val intervals = mutableListOf<Interval>()
    private lateinit var adapter: IntervalAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreateIntervalWorkoutBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Загрузка переданных интервалов
        intent.getParcelableArrayListExtra<Interval>("intervals")?.let {
            intervals.addAll(it)
        }

        // Настройка RecyclerView
        adapter = IntervalAdapter(intervals) { position ->
            intervals.removeAt(position)
            adapter.notifyItemRemoved(position)
        }
        binding.intervalsRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@CreateIntervalWorkoutActivity)
            adapter = this@CreateIntervalWorkoutActivity.adapter
        }

        // Настройка drag-and-drop
        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                val fromPos = viewHolder.adapterPosition
                val toPos = target.adapterPosition
                intervals.add(toPos, intervals.removeAt(fromPos))
                adapter.notifyItemMoved(fromPos, toPos)
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {}
        })
        itemTouchHelper.attachToRecyclerView(binding.intervalsRecyclerView)

        // Кнопка добавления интервала
        binding.addIntervalButton.setOnClickListener {
            intervals.add(Interval(IntervalType.WARMUP, 0.0))
            adapter.notifyItemInserted(intervals.size - 1)
        }

        // Кнопка сохранения тренировки
        binding.saveWorkoutButton.setOnClickListener {
            if (intervals.isEmpty()) {
                Toast.makeText(this, "Добавьте хотя бы один интервал", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (intervals.any { it.targetDistance <= 0.0 }) {
                Toast.makeText(this, "Все расстояния должны быть больше 0", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val intent = Intent().apply {
                putParcelableArrayListExtra("intervals", ArrayList(intervals))
            }
            setResult(RESULT_OK, intent)
            finish()
        }
    }
}

class IntervalAdapter(
    private val intervals: MutableList<Interval>,
    private val onDelete: (Int) -> Unit
) : RecyclerView.Adapter<IntervalAdapter.IntervalViewHolder>() {

    class IntervalViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val typeSpinner: Spinner = itemView.findViewById(R.id.intervalTypeSpinner)
        val distanceEditText: EditText = itemView.findViewById(R.id.distanceEditText)
        val deleteButton: ImageButton = itemView.findViewById(R.id.deleteButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): IntervalViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_interval, parent, false)
        return IntervalViewHolder(view)
    }

    override fun onBindViewHolder(holder: IntervalViewHolder, @SuppressLint("RecyclerView") position: Int) {
        val interval = intervals[position]

        // Настройка Spinner
        val types = IntervalType.values().map { it.name }
        val spinnerAdapter = ArrayAdapter(
            holder.itemView.context,
            android.R.layout.simple_spinner_item,
            types
        )
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        holder.typeSpinner.adapter = spinnerAdapter
        holder.typeSpinner.setSelection(interval.type.ordinal)

        // Обновление типа интервала
        holder.typeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, pos: Int, id: Long) {
                intervals[position] = intervals[position].copy(type = IntervalType.values()[pos])
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        // Настройка расстояния
        holder.distanceEditText.setText(if (interval.targetDistance == 0.0) "" else interval.targetDistance.toString())
        holder.distanceEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val distance = s.toString().toDoubleOrNull() ?: 0.0
                intervals[position] = intervals[position].copy(targetDistance = distance)
            }
        })

        // Кнопка удаления
        holder.deleteButton.setOnClickListener {
            onDelete(position)
        }
    }

    override fun getItemCount(): Int = intervals.size
}