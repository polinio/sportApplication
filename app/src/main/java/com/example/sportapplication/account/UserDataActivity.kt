package com.example.sportapplication.account

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.content.Context
import android.net.ConnectivityManager
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.sportapplication.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import java.util.*

class UserDataActivity : AppCompatActivity() {

    private val database = FirebaseDatabase.getInstance().reference
    private val firebaseAuth = FirebaseAuth.getInstance()

    private val sharedPreferences by lazy { getSharedPreferences("UserDataPrefs", MODE_PRIVATE) }

    // Добавляем флаг для отслеживания несинхронизированных изменений
    private var unsyncedChanges: MutableSet<String>
        get() = sharedPreferences.getStringSet("unsyncedChanges", mutableSetOf())?.toMutableSet() ?: mutableSetOf()
        set(value) {
            sharedPreferences.edit().putStringSet("unsyncedChanges", value).apply()
        }


    lateinit var tvName: TextView
    private lateinit var tvGender: TextView
    private lateinit var tvHeight: TextView
    lateinit var tvWeight: TextView
    private lateinit var tvBirthDate: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_data)

        tvName = findViewById(R.id.tvName)
        tvGender = findViewById(R.id.tvGender)
        tvHeight = findViewById(R.id.tvHeight)
        tvWeight = findViewById(R.id.tvWeight)
        tvBirthDate = findViewById(R.id.tvBirthDate)

        // Сначала проверяем интернет, затем загружаем данные
        if (isInternetAvailable()) {
            loadUserDataFromFirebase()
            syncCachedDataWithFirebase()
        } else {
            loadCachedUserData()
        }

        tvName.setOnClickListener { showEditTextDialog("Имя", "name", tvName) }
        tvGender.setOnClickListener { showGenderDialog() }
        tvHeight.setOnClickListener { showEditTextDialog("Рост (см)", "height", tvHeight) }
        tvWeight.setOnClickListener { showEditTextDialog("Вес (кг)", "weight", tvWeight) }
        tvBirthDate.setOnClickListener { showDatePickerDialog() }
    }

    // ! ---
    private fun saveToCache(field: String, value: String, markAsUnsynced: Boolean) {
        sharedPreferences.edit().putString(field, value).apply()
        val updated = unsyncedChanges
        if (markAsUnsynced) {
            updated.add(field)
        } else {
            updated.remove(field)
        }
        unsyncedChanges = updated
    }

    // ! ---
    private fun loadCachedUserData() {
        tvName.text = sharedPreferences.getString("name", "Не указано")
        tvGender.text = sharedPreferences.getString("gender", "Не указано")
        tvHeight.text = sharedPreferences.getString("height", "Не указано")
        tvWeight.text = sharedPreferences.getString("weight", "Не указано")
        tvBirthDate.text = sharedPreferences.getString("birthDate", "Не указано")
    }

    // ! ---
    private fun loadUserDataFromFirebase() {

        // Сначала загружаем из кэша, чтобы сразу показать актуальные данные
        loadCachedUserData()

        // Если есть интернет - загружаем из Firebase, но не перезаписываем несинхронизированные изменения
        if (isInternetAvailable()) {

            val userId = firebaseAuth.currentUser?.uid ?: return

            database.child("users").child(userId)
                .get().addOnSuccessListener { snapshot ->
                    // Получаем данные из Firebase
                    val firebaseName = snapshot.child("name").value?.toString()
                    val firebaseGender = snapshot.child("gender").value?.toString()
                    val firebaseHeight = snapshot.child("height").value?.toString()
                    val firebaseWeight = snapshot.child("weight").value?.toString()
                    val firebaseBirthDate = snapshot.child("birthDate").value?.toString()

                    // Обновляем только те поля, которые не были изменены оффлайн
                    if (!unsyncedChanges.contains("name") && firebaseName != null) {
                        tvName.text = firebaseName
                        saveToCache("name", firebaseName, false) // false - не помечаем как несинхронизированное
                    }
                    if (!unsyncedChanges.contains("gender") && firebaseGender != null) {
                        tvGender.text = firebaseGender
                        saveToCache("gender", firebaseGender, false)
                    }
                    if (!unsyncedChanges.contains("height") && firebaseHeight != null) {
                        tvHeight.text = firebaseHeight
                        saveToCache("height", firebaseHeight, false)
                    }
                    if (!unsyncedChanges.contains("weight") && firebaseWeight != null) {
                        tvWeight.text = firebaseWeight
                        saveToCache("weight", firebaseWeight, false)
                    }
                    if (!unsyncedChanges.contains("birthDate") && firebaseBirthDate != null) {
                        tvBirthDate.text = firebaseBirthDate
                        saveToCache("birthDate", firebaseBirthDate, false)
                    }

                    // Синхронизируем локальные изменения с Firebase
                    syncCachedDataWithFirebase()
                }
        }
    }

    private fun showEditTextDialog(title: String, field: String, textView: TextView) {
        val dialog = AlertDialog.Builder(this)
        val input = EditText(this)
        input.setText(textView.text.toString())

        // Настраиваем клавиатуру
        when (field) {
            "height", "weight" -> input.inputType = InputType.TYPE_CLASS_NUMBER
            "name" -> input.inputType = InputType.TYPE_CLASS_TEXT
        }

        dialog.setTitle(title)
            .setView(input)
            .setPositiveButton("ОК") { _, _ ->
                val newValue = input.text.toString().trim()
                if (validateInput(field, newValue)) {
                    updateUserData(field, newValue, textView)
                    textView.text = newValue
                } else {
                    Toast.makeText(this, "Ошибка: некорректный ввод", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun validateInput(field: String, value: String): Boolean {
        return when (field) {
            "name" -> value.matches(Regex("^[А-Яа-яA-Za-z\\s]{2,50}$"))
            "height" -> value.matches(Regex("^\\d{2,3}$")) && value.toInt() in 50..250
            "weight" -> value.matches(Regex("^\\d{2,3}$")) && value.toInt() in 20..300
            else -> true
        }
    }

    private fun showGenderDialog() {
        val dialog = AlertDialog.Builder(this)
        val inflater = LayoutInflater.from(this)
        val view = inflater.inflate(R.layout.dialog_gender, null)
        val radioGroup = view.findViewById<RadioGroup>(R.id.radioGroupGender)

        dialog.setTitle("Выберите пол")
            .setView(view)
            .setPositiveButton("ОК") { _, _ ->
                val selectedId = radioGroup.checkedRadioButtonId
                val selectedGender = view.findViewById<RadioButton>(selectedId)?.text.toString()
                updateUserData("gender", selectedGender, tvGender)
                tvGender.text = selectedGender
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    @SuppressLint("DefaultLocale")
    private fun showDatePickerDialog() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(this, { _, selectedYear, selectedMonth, selectedDay ->
            val formattedDate = String.format("%02d-%02d-%d", selectedDay, selectedMonth + 1, selectedYear) // Формат: dd-MM-yyyy
            updateUserData("birthDate", formattedDate, tvBirthDate)
            tvBirthDate.text = formattedDate
        }, year, month, day)

        datePickerDialog.show()
    }

    // ! ---
    private fun updateUserData(field: String, value: String, textView: TextView) {
        if (!validateInput(field, value)) {
            Toast.makeText(this, "Ошибка: некорректный ввод", Toast.LENGTH_SHORT).show()
            return
        }

        textView.text = value // Сразу обновляем UI

        // Сохраняем в кэш и помечаем как несинхронизированное
        saveToCache(field, value,true) // Третий аргумент true - помечаем как несинхронизированное

        if (isInternetAvailable()) {
            // Если есть интернет - сразу синхронизируем
            syncSingleFieldWithFirebase(field, value)
        } else {
            Toast.makeText(this, "Данные сохранены локально и будут синхронизированы при подключении", Toast.LENGTH_LONG).show()
        }
    }
    private fun syncSingleFieldWithFirebase(field: String, value: String) {
        val userId = firebaseAuth.currentUser?.uid ?: return

        database.child("users").child(userId).child(field).setValue(value)
            .addOnSuccessListener {
                unsyncedChanges.remove(field)
                Toast.makeText(this, "Данные успешно синхронизированы", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Ошибка синхронизации данных", Toast.LENGTH_SHORT).show()
            }
    }

    // ! ---
    private fun syncCachedDataWithFirebase() {
        val userId = firebaseAuth.currentUser?.uid ?: return

        // Синхронизируем только поля с несинхронизированными изменениями
        unsyncedChanges.toList().forEach { field ->
            val cachedValue = sharedPreferences.getString(field, null)
            if (cachedValue != null) {
                database.child("users").child(userId).child(field).setValue(cachedValue)
                    .addOnSuccessListener {
                        unsyncedChanges.remove(field)
                        // После синхронизации обновляем UI
                        when (field) {
                            "name" -> tvName.text = cachedValue
                            "gender" -> tvGender.text = cachedValue
                            "height" -> tvHeight.text = cachedValue
                            "weight" -> tvWeight.text = cachedValue
                            "birthDate" -> tvBirthDate.text = cachedValue
                        }
                    }
            }
        }
    }

    private fun isInternetAvailable(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo = connectivityManager.activeNetworkInfo
        return networkInfo != null && networkInfo.isConnected
    }
}
