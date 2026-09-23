package com.example.aihair.feature.settings

import android.content.Intent
import android.os.Bundle
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.aihair.R
import com.example.aihair.core.ui.base.BaseActivity
import com.example.aihair.core.ui.click.setDebouncedClickListener
import com.example.aihair.databinding.ActivitySettingsBinding
import com.example.aihair.feature.language.LanguageActivity
import com.example.aihair.feature.main.MainActivity

class SettingsActivity : BaseActivity<ActivitySettingsBinding>(ActivitySettingsBinding::inflate) {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupUI()
    }

    private fun setupUI() {
        onBackPressedDispatcher.addCallback(this, object : androidx.activity.OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                startActivity(Intent(this@SettingsActivity, MainActivity::class.java))
                finish()
            }
        })

        binding.btnBack.setDebouncedClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        binding.btnLanguage.setDebouncedClickListener {
            val intent = Intent(this, LanguageActivity::class.java).apply {
                putExtra("from_setting", true)
            }
            startActivity(intent)
            finish()
        }
    }
}