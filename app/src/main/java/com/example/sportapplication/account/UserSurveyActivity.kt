package com.example.sportapplication.account

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.sportapplication.databinding.ActivityUserSurveyBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ktx.database
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase

class UserSurveyActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUserSurveyBinding
    private val firestore = FirebaseFirestore.getInstance()
    private val firebaseAuth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUserSurveyBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnSave.setOnClickListener {
            val name = binding.etName.text.toString().trim()
            val gender = binding.spinnerGender.selectedItem.toString()
            val height = binding.etHeight.text.toString().trim()
            val weight = binding.etWeight.text.toString().trim()
            val birthYear = binding.etBirthYear.text.toString().trim()
            Toast.makeText(this, "Нажата кнопка Готово", Toast.LENGTH_SHORT).show()

            if (name.isNotEmpty() && height.isNotEmpty() && weight.isNotEmpty() && birthYear.isNotEmpty()) {
                saveUserData(name, gender, height, weight, birthYear)
            } else {
                Toast.makeText(this, "Заполните все поля", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun saveUserData(name: String, gender: String, height: String, weight: String, birthYear: String) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            val database = Firebase.database.reference

            val userData = mapOf(
                "name" to name,
                "gender" to gender,
                "height" to height.toInt(),
                "weight" to weight.toDouble(),
                "birthYear" to birthYear.toInt()
            )

            database.child("users").child(userId).setValue(userData)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Log.d("RealtimeDB", "Данные успешно сохранены")
                        Toast.makeText(this, "Данные сохранены", Toast.LENGTH_SHORT).show()
                        finish()
                    } else {
                        Log.e("RealtimeDB", "Ошибка сохранения данных: ${task.exception?.message}")
                        Toast.makeText(this, "Ошибка: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                    }
                }
        } else {
            Log.e("RealtimeDB", "Ошибка: userId == null")
            Toast.makeText(this, "Ошибка авторизации", Toast.LENGTH_LONG).show()
        }
    }
}
