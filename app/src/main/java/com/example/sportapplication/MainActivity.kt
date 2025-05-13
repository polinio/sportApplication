package com.example.sportapplication

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.sportapplication.account.ProfileFragment
import com.example.sportapplication.workout.WorkoutFragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.yandex.mapkit.MapKitFactory

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            MapKitFactory.setApiKey("b63fe3ef-6715-4945-876d-dfa328e906e2")
        } catch (e: AssertionError) {

        }
        setContentView(R.layout.activity_main)

        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation)

        bottomNavigationView.setOnItemSelectedListener{ item ->
            val fragmentTransaction = supportFragmentManager.beginTransaction()
            when (item.itemId) {
                R.id.navigation_home -> {
                    val mainMenuFragment = MainMenuFragment()
                    fragmentTransaction.replace(R.id.frameLayout, mainMenuFragment)
                    true
                }

                R.id.navigation_profile -> {
                    val profileFragment = ProfileFragment()
                    fragmentTransaction.replace(R.id.frameLayout, profileFragment)
                    true
                }

                R.id.navigation_start -> {
                    // Переход на экран тренировки
                    val workoutFragment = WorkoutFragment()
                    fragmentTransaction.replace(R.id.frameLayout, workoutFragment)
                    true
                }

                else -> return@setOnItemSelectedListener false
            }
            fragmentTransaction.commit() // Коммит транзакции
            true
        }
        // Отобразим главный фрагмент при запуске
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.frameLayout, MainMenuFragment())
                .commit()
        }
    }
}