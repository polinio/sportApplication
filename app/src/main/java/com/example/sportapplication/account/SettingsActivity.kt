package com.example.sportapplication.account

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.sportapplication.databinding.ActivitySettingsBinding

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private val sharedPreferences by lazy { getSharedPreferences("SettingsPrefs", Context.MODE_PRIVATE) }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Toast.makeText(this, "Уведомления включены", Toast.LENGTH_SHORT).show()
            saveNotificationPreference(true)
        } else {
            Toast.makeText(this, "Разрешение на уведомления отклонено", Toast.LENGTH_SHORT).show()
            saveNotificationPreference(false)
            binding.notificationsSwitch.isChecked = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val areNotificationsEnabled = sharedPreferences.getBoolean("notifications_enabled", true)
        binding.notificationsSwitch.isChecked = areNotificationsEnabled

        binding.notificationsSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                // Проверяем разрешение на уведомления для Android 13+
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    when {
                        ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED -> {
                            Toast.makeText(this, "Уведомления включены", Toast.LENGTH_SHORT).show()
                            saveNotificationPreference(true)
                        }
                        shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {
                            // Можно показать объяснение пользователю перед запросом (опционально)
                            Toast.makeText(this, "Для включения уведомлений нужно разрешение", Toast.LENGTH_LONG).show()
                            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                        else -> {
                            // Запрашиваем разрешение
                            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                } else {
                    // Для версий ниже Android 13 просто включаем уведомления
                    Toast.makeText(this, "Уведомления включены", Toast.LENGTH_SHORT).show()
                    saveNotificationPreference(true)
                }
            } else {
                Toast.makeText(this, "Уведомления выключены", Toast.LENGTH_SHORT).show()
                saveNotificationPreference(false)
            }
        }
    }

    private fun saveNotificationPreference(enabled: Boolean) {
        sharedPreferences.edit().putBoolean("notifications_enabled", enabled).apply()
    }
}


