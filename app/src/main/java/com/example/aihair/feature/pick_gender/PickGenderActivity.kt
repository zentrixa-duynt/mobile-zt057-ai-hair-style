package com.example.aihair.feature.pick_gender

import dev.zentrixa.common.admob.ZTInterstitialAdUtils

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.core.view.isVisible
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
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import dev.zentrixa.common.admob.ZTRewardedAdUtils
import dev.zentrixa.common.firebase.ZTAnalyticsUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
            ZTInterstitialAdUtils.loadAndShowInterstitialAd(this, "inter_back", "p_inter_back") {
                finish()
            }
        }

        // Default selection from DataStore
        lifecycleScope.launch {
            val state = appPreferences.launchState.first()
            val initialGender = state.selectedGender ?: "FEMALE"
            selectGender(initialGender)
        }

        lifecycleScope.launch {
            appPreferences.usageState.collect { usage ->
                val currentDate = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
                val todayUsage = if (usage.lastUsageDate == currentDate) usage.usageCount else 0
                val dailyLimit = com.google.firebase.remoteconfig.FirebaseRemoteConfig.getInstance().getLong("limit_daily").toInt().takeIf { it > 0 } ?: 5
                
                binding.txtTodayRemaining.text = "${dailyLimit - todayUsage}".takeIf { (dailyLimit - todayUsage) >= 0 } ?: "0"
                binding.txtTotalRemaining.text = "/$dailyLimit"
            }
        }

        binding.btnAnalyzeNowAds.visibility = View.VISIBLE

        binding.btnGenderFemale.setDebouncedClickListener {
            selectGender("FEMALE")
        }

        binding.btnGenderMale.setDebouncedClickListener {
            selectGender("MALE")
        }

        binding.btnGenderOther.setDebouncedClickListener {
            selectGender("OTHER")
        }

        val analyzeClickListener: (View) -> Unit = {
            lifecycleScope.launch {
                val usage = appPreferences.usageState.first()
                val currentDate = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
                val todayUsage = if (usage.lastUsageDate == currentDate) usage.usageCount else 0
                val dailyLimit = com.google.firebase.remoteconfig.FirebaseRemoteConfig.getInstance().getLong("limit_daily").toInt().takeIf { it > 0 } ?: 5

                if (todayUsage >= dailyLimit) {
                    Toast.makeText(this@PickGenderActivity, getString(R.string.msg_daily_limit_reached), Toast.LENGTH_SHORT).show()
                    return@launch
                }
                
                val type = intent.getStringExtra(AnalysisActivity.EXTRA_ANALYSIS_TYPE)
                if (type == AnalysisActivity.TYPE_MAKEUP) {
                    ZTAnalyticsUtils.logEvent("D_action_analyze_make")
                } else {
                    ZTAnalyticsUtils.logEvent("D_action_analyze_face")
                }

                var isAdDone = false
                ZTRewardedAdUtils.loadAndShowRewardAd(
                    this@PickGenderActivity,
                    "reward_function_AI",
                    "p_reward_analyze",
                    onAdEarned = {
                        isAdDone = true
                    },
                    onAdClosed = { isEarned ->
                        if (isEarned || isAdDone) {
                            lifecycleScope.launch {
                                proceedToAnalysis(analysisType, imageUri)
                            }
                        }
                    }
                )
            }
        }
        val rewardConfig = dev.zentrixa.common.admob.ZTRewardedAdUtils.getRewardAdsConfig("reward_function_AI")
        val isAdEnabled = rewardConfig?.enabled != false && rewardConfig?.placements?.get("p_reward_analyze") != false
        binding.imgAdIconAnalyze.isVisible = isAdEnabled && !dev.zentrixa.common.utils.ZTUtils.isTurnOffAllAds

        binding.btnAnalyzeNowAds.setDebouncedClickListener { analyzeClickListener(it) }
    }

    private fun proceedToAnalysis(analysisType: String?, imageUri: String) {
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
        finish()
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