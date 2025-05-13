package com.example.sportapplication

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.example.sportapplication.account.UserSurveyActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SplashActivity : AppCompatActivity() {
    private val firestore = FirebaseFirestore.getInstance()
    private val firebaseAuth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // Задержка для экрана загрузки (например, 2 секунды)
        Handler(Looper.getMainLooper()).postDelayed({
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish() // Закрываем SplashActivity, чтобы нельзя было вернуться назад
        }, 1000)
    }

    private fun checkUserData() {
        val userId = firebaseAuth.currentUser?.uid

        if (userId != null) {
            firestore.collection("users").document(userId).get()
                .addOnSuccessListener { document ->
                    if (document.exists() && document.getString("name") != null) {
                        // Данные есть → переходим в `MainActivity`
                        startActivity(Intent(this, MainActivity::class.java))
                    } else {
                        // Данных нет → отправляем пользователя в анкету
                        startActivity(Intent(this, UserSurveyActivity::class.java))
                    }
                    finish()
                }
                .addOnFailureListener { e ->
                    Log.e("SplashActivity", "Ошибка загрузки данных: ${e.message}")
                    startActivity(Intent(this, UserSurveyActivity::class.java))
                    finish()
                }
        } else {
            // Если пользователь не авторизован, отправляем его на экран логина
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }
}