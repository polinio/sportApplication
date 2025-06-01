package com.example.sportapplication.account

import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.sportapplication.databinding.ActivitySettingsBinding

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private val sharedPreferences by lazy { getSharedPreferences("SettingsPrefs",
        Context.MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Инициализация переключателя уведомлений
        val areNotificationsEnabled = sharedPreferences.getBoolean("notifications_enabled", true)
        binding.notificationsSwitch.isChecked = areNotificationsEnabled
        binding.notificationsSwitch.setOnCheckedChangeListener { _, isChecked ->
            sharedPreferences.edit().putBoolean("notifications_enabled", isChecked).apply()
        }
    }
}
