package com.example.sportapplication.account

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import com.example.sportapplication.LoginActivity
import com.example.sportapplication.R
import com.example.sportapplication.data.AvatarGenerator
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class ProfileFragment : Fragment() {

    private val firebaseAuth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance().reference
    private lateinit var googleSignInClient: GoogleSignInClient

    private val sharedPreferences by lazy { requireContext().getSharedPreferences(
        "UserDataPrefs", Context.MODE_PRIVATE) }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)

        // Инициализация GoogleSignInClient
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(requireContext().getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(requireContext(), gso)

        // Загружаем кэшированное имя
        loadCachedUserName(view)

        // Получаем текущего пользователя
        val userId = firebaseAuth.currentUser?.uid ?: return view

        // Загружаем имя пользователя из Firebase
        database.child("users").child(userId).child("name")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val name = snapshot.value?.toString() ?: "Не указано"
                    view.findViewById<TextView>(R.id.userName).text = name // Устанавливаем имя в TextView
                    generateInitialAvatar(view, name)
                    saveUserNameToCache(name)
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(requireContext(), "Ошибка загрузки данных: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })

        // Обрабатываем нажатие на "Мои данные"
        view.findViewById<TextView>(R.id.healthData).setOnClickListener {
            startActivity(Intent(requireContext(), UserDataActivity::class.java))
        }

        // Обрабатываем нажатие на "Справка"
        view.findViewById<TextView>(R.id.help).setOnClickListener {
            startActivity(Intent(requireContext(), HelpActivity::class.java))
        }

        // Обрабатываем нажатие на "Сведения"
        view.findViewById<TextView>(R.id.about).setOnClickListener {
            startActivity(Intent(requireContext(), AboutActivity::class.java))
        }

        // Обрабатываем нажатие на "Настройки"
        view.findViewById<TextView>(R.id.settings).setOnClickListener {
            startActivity(Intent(requireContext(), SettingsActivity::class.java))
        }

        // Обрабатываем нажатие на "Выйти"
        view.findViewById<TextView>(R.id.logout).setOnClickListener {
            // Выход из Firebase
            firebaseAuth.signOut()
            // Выход из Google Sign-In
            googleSignInClient.signOut()
            // Переход на LoginActivity
            startActivity(Intent(requireContext(), LoginActivity::class.java))
            activity?.finish()
        }

        return view
    }

    private fun loadCachedUserName(view: View) {
        val cachedName = sharedPreferences.getString("name", "Не указано")
        view.findViewById<TextView>(R.id.userName).text = cachedName
        generateInitialAvatar(view, cachedName ?: "Не указано")
    }

    private fun saveUserNameToCache(name: String) {
        sharedPreferences.edit().putString("name", name).apply()
    }

    private fun generateInitialAvatar(view: View, name: String) {
        val userPhoto = view.findViewById<ShapeableImageView>(R.id.userPhoto)
        if (name == "Не указано" || name.isEmpty()) {
            userPhoto.setImageResource(R.drawable.example)
        } else {
            val initials = name.split(" ").mapNotNull { it.firstOrNull()?.toString() }
                .joinToString("")
            val bitmap = AvatarGenerator.generateAvatar(requireContext(), initials, 70)
            userPhoto.setImageBitmap(bitmap)
        }
    }
}