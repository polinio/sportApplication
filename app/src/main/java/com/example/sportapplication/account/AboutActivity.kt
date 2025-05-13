package com.example.sportapplication.account

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.sportapplication.R
import com.example.sportapplication.databinding.ActivityAboutBinding
import com.example.sportapplication.BuildConfig

class AboutActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAboutBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAboutBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Установка версии приложения
        binding.versionText.text = getString(
            R.string.app_version,
            BuildConfig.VERSION_NAME,
            BuildConfig.VERSION_CODE
        )

        // Обработка клика по политике конфиденциальности
        binding.privacyPolicy.setOnClickListener {
            openUrl(getString(R.string.privacy_policy_url))
        }

        // Обработка клика по условиям использования
        binding.termsOfUse.setOnClickListener {
            openUrl(getString(R.string.terms_of_use_url))
        }

        // Обработка клика по email
        binding.contactEmail.setOnClickListener {
            val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:${getString(R.string.contact_email)}")
            }
            startActivity(Intent.createChooser(emailIntent, "Отправить email"))
        }

        // Обработка клика по кнопке "Поделиться"
        binding.shareApp.setOnClickListener {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, getString(R.string.app_name))
                putExtra(Intent.EXTRA_TEXT, getString(R.string.share_app_text))
            }
            startActivity(Intent.createChooser(shareIntent, "Поделиться приложением"))
        }

        // Обработка клика по лицензиям
        binding.licenses.setOnClickListener {
            // Временный текст лицензий, можно заменить на диалог или отдельный экран
            binding.licensesText.text = getString(R.string.licenses_text)
            binding.licensesText.visibility = if (binding.licensesText.visibility == android.view.View.VISIBLE) {
                android.view.View.GONE
            } else {
                android.view.View.VISIBLE
            }
        }
    }

    private fun openUrl(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        startActivity(intent)
    }
}