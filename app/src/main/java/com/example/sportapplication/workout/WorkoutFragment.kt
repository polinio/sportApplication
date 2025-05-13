package com.example.sportapplication.workout

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.example.sportapplication.R
import com.example.sportapplication.data.Interval
import com.example.sportapplication.data.PaceSettings
import com.example.sportapplication.databinding.FragmentWorkoutBinding

class WorkoutFragment : Fragment() {
    private var _binding: FragmentWorkoutBinding? = null
    private val binding get() = _binding!!
    private var goalType: String? = null
    private var goalValue: Float? = null
    private var selectedActivity = "walking"
    private var intervals: List<Interval>? = null
    private var paceSettings: PaceSettings? = null

    // Регистрация Activity Result для получения результата от CreateIntervalWorkoutActivity
    private val createIntervalResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            intervals = result.data?.getParcelableArrayListExtra("intervals")
            binding.settingsIndicator.text = if (intervals.isNullOrEmpty()) {
                "Настройки: не выбрано"
            } else {
                "Интервалы: ${intervals!!.size} этапов"
            }
            binding.createIntervalButton.text = if (intervals.isNullOrEmpty()) {
                "Создать тренировку"
            } else {
                "Редактировать интервалы"
            }
        }
    }

    // Регистрация для темпа
    private val setPaceResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val data = result.data
            paceSettings = PaceSettings(
                distance = data?.getDoubleExtra("distance", 0.0) ?: 0.0,
                pace = data?.getStringExtra("pace") ?: "",
                interval = data?.getStringExtra("interval") ?: "",
                tolerance = data?.getIntExtra("tolerance", 0) ?: 0
            )
            updateSettingsIndicator()
            binding.setPaceButton.text = "Редактировать темп"
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWorkoutBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Установка начального состояния
        binding.activitySelectionGroup.check(R.id.rbWalking)
        binding.modeSelectionGroup.check(R.id.rbNormal)
        binding.settingsIndicator.text = "Настройки: не выбрано"

        // Настройка выбора активности
        binding.activitySelectionGroup.setOnCheckedChangeListener { _, checkedId ->
            selectedActivity = when (checkedId) {
                R.id.rbRunning -> "running"
                R.id.rbWalking -> "walking"
                R.id.rbCycling -> "cycling"
                else -> "walking"
            }
        }

        // Настройка выбора режима
        binding.modeSelectionGroup.setOnCheckedChangeListener { _, checkedId ->
            binding.setGoalButton.visibility = View.GONE
            binding.createIntervalButton.visibility = View.GONE
            binding.setPaceButton.visibility = View.GONE
            binding.settingsIndicator.text = "Настройки: не выбрано"
            goalType = null
            goalValue = null
            intervals = null
            paceSettings = null
            when (checkedId) {
                R.id.rbNormal -> binding.setGoalButton.visibility = View.VISIBLE
                R.id.rbInterval -> binding.createIntervalButton.visibility = View.VISIBLE
                R.id.rbPace -> binding.setPaceButton.visibility = View.VISIBLE
            }
        }

        // Кнопка настройки цели (обычный режим)
        binding.setGoalButton.setOnClickListener {
            showGoalDialog()
        }

        // Кнопка создания интервальной тренировки
        binding.createIntervalButton.setOnClickListener {
            val intent = Intent(requireContext(), CreateIntervalWorkoutActivity::class.java).apply {
                intervals?.let { putParcelableArrayListExtra("intervals", ArrayList(it)) }
            }
            createIntervalResult.launch(intent)
        }

        // Кнопка настройки темпа (заглушка для будущего режима)
        binding.setPaceButton.setOnClickListener {
            val intent = Intent(requireContext(), CreateTempoWorkoutActivity::class.java).apply {
                paceSettings?.let {
                    putExtra("distance", it.distance)
                    putExtra("pace", it.pace)
                    putExtra("interval", it.interval)
                    putExtra("tolerance", it.tolerance)
                }
            }
            setPaceResult.launch(intent)
        }

        // Кнопка старта тренировки
        binding.startButton.setOnClickListener {
            val intent = Intent(requireContext(), WorkoutTrackingActivity::class.java).apply {
                putExtra("ACTIVITY_TYPE", selectedActivity)
            }
            when (binding.modeSelectionGroup.checkedRadioButtonId) {
                R.id.rbNormal -> {
                    goalType?.let { intent.putExtra("GOAL_TYPE", it) }
                    goalValue?.let { intent.putExtra("GOAL_VALUE", it) }
                }
                R.id.rbInterval -> {
                    if (intervals.isNullOrEmpty()) {
                        Toast.makeText(context, "Создайте интервальную тренировку", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }
                    intent.putParcelableArrayListExtra("intervals", ArrayList(intervals))
                }
                R.id.rbPace -> {
                    if (paceSettings == null) {
                        Toast.makeText(context, "Настройте темп тренировки", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }
                    intent.putExtra("distance", paceSettings!!.distance)
                    intent.putExtra("pace", paceSettings!!.pace)
                    intent.putExtra("interval", paceSettings!!.interval)
                    intent.putExtra("tolerance", paceSettings!!.tolerance)
                }
            }
            startActivity(intent)
        }
    }

    private fun updateSettingsIndicator() {
        binding.settingsIndicator.text = when {
            paceSettings != null -> "Темп: ${paceSettings!!.pace} на ${paceSettings!!.distance} км"
            intervals?.isNotEmpty() == true -> "Интервалы: ${intervals!!.size} этапов"
            goalType != null && goalValue != null -> when (goalType) {
                "distance" -> "Цель: $goalValue км"
                "time" -> "Цель: ${goalValue!!.toInt()} мин"
                "calories" -> "Цель: ${goalValue!!.toInt()} ккал"
                else -> "Настройки: не выбрано"
            }
            else -> "Настройки: не выбрано"
        }
    }

    private fun showGoalDialog() {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_set_goal, null)
        val goalRadioGroup: RadioGroup = dialogView.findViewById(R.id.goalRadioGroup)
        val goalInput: EditText = dialogView.findViewById(R.id.goalInput)
        val unitText: TextView = dialogView.findViewById(R.id.unitText)

        // Устанавливаем начальные значения
        goalRadioGroup.check(R.id.radioDistance)
        goalInput.hint = "Введите значение от 0,1 до 100 км"
        goalInput.inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
        unitText.text = "км"

        // Обновляем hint и inputType при выборе цели
        goalRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.radioDistance -> {
                    goalInput.hint = "Введите значение от 0,1 до 100 км"
                    goalInput.inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
                    unitText.text = "км"
                }
                R.id.radioTime -> {
                    goalInput.hint = "Введите значение от 10 до 1440 мин"
                    goalInput.inputType = android.text.InputType.TYPE_CLASS_NUMBER
                    unitText.text = "мин"
                }
                R.id.radioCalories -> {
                    goalInput.hint = "Введите значение от 100 до 5000 ккал"
                    goalInput.inputType = android.text.InputType.TYPE_CLASS_NUMBER
                    unitText.text = "ккал"
                }
            }
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Настроить цель")
            .setView(dialogView)
            .setPositiveButton("Подтвердить") { _, _ ->
                val selectedGoalId = goalRadioGroup.checkedRadioButtonId
                val inputText = goalInput.text.toString()

                if (selectedGoalId == -1 || inputText.isEmpty()) {
                    Toast.makeText(context, "Выберите цель и введите значение", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val inputValue = inputText.toFloatOrNull()
                if (inputValue == null) {
                    Toast.makeText(context, "Введите корректное число", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                when (selectedGoalId) {
                    R.id.radioDistance -> {
                        if (inputValue in 0.1f..100f) {
                            goalType = "distance"
                            goalValue = inputValue
                            binding.setGoalButton.text = "Цель: $inputValue км"
                            binding.settingsIndicator.text = "Цель: $inputValue км"
                        } else {
                            Toast.makeText(context, "Расстояние должно быть от 0,1 до 100 км", Toast.LENGTH_SHORT).show()
                        }
                    }
                    R.id.radioTime -> {
                        if (inputValue in 10f..1440f && inputValue % 1 == 0f) {
                            goalType = "time"
                            goalValue = inputValue
                            binding.setGoalButton.text = "Цель: ${inputValue.toInt()} мин"
                            binding.settingsIndicator.text = "Цель: ${inputValue.toInt()} мин"
                        } else {
                            Toast.makeText(context, "Время должно быть от 10 до 1440 минут (целое число)", Toast.LENGTH_SHORT).show()
                        }
                    }
                    R.id.radioCalories -> {
                        if (inputValue in 100f..5000f && inputValue % 1 == 0f) {
                            goalType = "calories"
                            goalValue = inputValue
                            binding.setGoalButton.text = "Цель: ${inputValue.toInt()} ккал"
                            binding.settingsIndicator.text = "Цель: ${inputValue.toInt()} ккал"
                        } else {
                            Toast.makeText(context, "Калории должны быть от 100 до 5000 (целое число)", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}