package com.example.aihair.feature.pick_gender

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.example.aihair.R
import com.example.aihair.core.ui.base.BaseActivity
import com.example.aihair.core.ui.click.setDebouncedClickListener
import com.example.aihair.databinding.ActivityPickGenderBinding
import com.example.aihair.core.data.local.datastore.AppPreferences
import com.example.aihair.feature.analysis.AnalysisActivity
import com.example.aihair.feature.face_result.FaceResultActivity
import com.example.aihair.feature.makeup_result.MakeupResultActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class PickGenderActivity : BaseActivity<ActivityPickGenderBinding>(ActivityPickGenderBinding::inflate) {

    @Inject
    lateinit var appPreferences: AppPreferences

    private var selectedGender: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val imageUri = intent.getStringExtra("EXTRA_SELECTED_IMAGE_URI")
        val analysisType = intent.getStringExtra(AnalysisActivity.EXTRA_ANALYSIS_TYPE)

        if (imageUri == null) {
            Toast.makeText(this, getString(R.string.msg_error_no_image_selected), Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        binding.btnBack.setDebouncedClickListener {
            finish()
        }

        // Default selection from DataStore
        lifecycleScope.launch {
            val state = appPreferences.launchState.first()
            val initialGender = state.selectedGender ?: "FEMALE"
            selectGender(initialGender)
        }

        binding.btnGenderFemale.setDebouncedClickListener {
            selectGender("FEMALE")
        }

        binding.btnGenderMale.setDebouncedClickListener {
            selectGender("MALE")
        }

        binding.btnGenderOther.setDebouncedClickListener {
            selectGender("OTHER")
        }

        binding.btnAnalyzeNow.setDebouncedClickListener {
            val targetClass = if (analysisType == AnalysisActivity.TYPE_MAKEUP) {
                MakeupResultActivity::class.java
            } else {
                FaceResultActivity::class.java
            }

            val resultIntent = Intent(this, targetClass).apply {
                putExtra("EXTRA_SELECTED_IMAGE_URI", imageUri)
                putExtra("EXTRA_SELECTED_GENDER", selectedGender)
            }
            startActivity(resultIntent)
        }
    }

    private fun selectGender(gender: String) {
        selectedGender = gender
        binding.btnGenderFemale.isSelected = gender == "FEMALE"
        binding.btnGenderMale.isSelected = gender == "MALE"
        binding.btnGenderOther.isSelected = gender == "OTHER"
        
        lifecycleScope.launch {
            appPreferences.setSelectedGender(gender)
        }
    }
}