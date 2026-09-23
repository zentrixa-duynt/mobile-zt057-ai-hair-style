package com.example.aihair.app

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import com.example.aihair.R
import com.example.aihair.BuildConfig
import com.example.aihair.core.data.local.datastore.AppPreferences
import com.example.aihair.core.language.AppLanguageManager
import com.example.aihair.core.ui.base.BaseActivity
import com.example.aihair.core.utils.NetworkUtils
import com.example.aihair.feature.main.MainActivity
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import dagger.hilt.android.HiltAndroidApp
import dev.zentrixa.common.activity.OnboardingNativeFullPageConfig
import dev.zentrixa.common.activity.OnboardingPageConfig
import dev.zentrixa.common.activity.ZTFLLanguageItemLayoutConfig
import dev.zentrixa.common.activity.ZTFLNextLayoutConfig
import dev.zentrixa.common.activity.ZTFirstLanguageConfig
import dev.zentrixa.common.activity.ZTLanguageModel
import dev.zentrixa.common.activity.ZTOnboardingConfig
import dev.zentrixa.common.activity.ZTSplashConfig
import dev.zentrixa.common.utils.ZTAdsConfig
import dev.zentrixa.common.utils.ZTFirstOpenConfig
import dev.zentrixa.common.utils.ZTNativeAdConfig
import dev.zentrixa.common.utils.ZTUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.Locale
import javax.inject.Inject

@HiltAndroidApp
class AIHairApp : Application() {

    @Inject
    lateinit var appPreferences: AppPreferences

    @SuppressLint("ResourceType")
    override fun onCreate() {
        super.onCreate()

        FirebaseApp.initializeApp(this)
        
        val firebaseAppCheck = FirebaseAppCheck.getInstance()
        if (!BuildConfig.isProduction) {
            firebaseAppCheck.installAppCheckProviderFactory(
                DebugAppCheckProviderFactory.getInstance()
            )
        } else {
            firebaseAppCheck.installAppCheckProviderFactory(
                PlayIntegrityAppCheckProviderFactory.getInstance()
            )
        }

        val remoteConfig = FirebaseRemoteConfig.getInstance()
        val configSettings = FirebaseRemoteConfigSettings.Builder()
            .setMinimumFetchIntervalInSeconds(if (!BuildConfig.isProduction) 0 else 3600)
            .build()
        remoteConfig.setConfigSettingsAsync(configSettings)
        remoteConfig.setDefaultsAsync(
            mapOf(
                "generation_model" to "gpt-image-1.5",
                "generation_background" to "opaque",
                "generation_quality" to "low",
                "generation_size" to "1024x1536",
                "analysis_models" to "gpt-5-nano,gpt-4o-mini,gpt-4o"
            )
        )
        remoteConfig.fetchAndActivate()

        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                if (activity !is BaseActivity<*>) {
                    val languageTag = AppLanguageManager.currentLanguageTag()
                    AppLanguageManager.syncAppCompatLanguageIfNeeded(languageTag)

                    val locale = if (languageTag.isBlank()) Locale.getDefault() else Locale.forLanguageTag(languageTag)
                    val config = Configuration(activity.resources.configuration).apply {
                        setLocale(locale)
                        setLayoutDirection(locale)
                    }

                    @Suppress("DEPRECATION")
                    activity.resources.updateConfiguration(config, activity.resources.displayMetrics)
                }
            }

            override fun onActivityStarted(activity: Activity) {}
            override fun onActivityResumed(activity: Activity) {}
            override fun onActivityPaused(activity: Activity) {}
            override fun onActivityStopped(activity: Activity) {}
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
            override fun onActivityDestroyed(activity: Activity) {}
        })

        AppLanguageManager.applySavedLanguage(appPreferences)
        NetworkUtils.registerNetworkCallback(this)

        ZTUtils.init(
            application = this,
            appId = "ca-app-pub-3940256099942544~3347511713",
            isProduction = BuildConfig.isProduction,
        )

        ZTUtils.setZTAdsConfig(
            config = ZTAdsConfig(
                nativeAdConfig = ZTNativeAdConfig(
                    backgroundAdsContainer = R.drawable.bg_ad_container,
                    backgroundCTARes = R.drawable.bg_ad_cta_button,
                    backgroundAdBadgeRes = R.drawable.bg_ad_badge,
                    textColorPrimaryRes = R.color.ad_primary_text,
                    textColorSecondaryRes = R.color.ad_secondary_text,
                    textColorButtonRes = R.color.ad_btn_text,
                    textColorBadge = R.color.ad_badge_text
                )
            )
        )

        // Tích hợp luồng First Open tự động của Titan SDK
        ZTUtils.setZTFirstOpenConfig(
            config = ZTFirstOpenConfig(
                splashConfig = ZTSplashConfig(layoutRes = R.layout.activity_splash),
                firstLanguageConfig = ZTFirstLanguageConfig(
                    mainLayoutRes = R.layout.activity_language_fo,
                    languageList = listOf(
                        ZTLanguageModel("en", R.drawable.ic_flag_en, "English", localName = getLocalName("English", R.string.language_english)),
                        ZTLanguageModel("vi", R.drawable.ic_flag_vi, "Tiếng Việt", localName = getLocalName("Tiếng Việt", R.string.language_vietnamese)),
                        ZTLanguageModel("es", R.drawable.ic_flag_es, "Español", localName = getLocalName("Español", R.string.language_spanish)),
                        ZTLanguageModel("fr", R.drawable.ic_flag_fr, "Français", localName = getLocalName("Français", R.string.language_french)),
                        ZTLanguageModel("de", R.drawable.ic_flag_de, "Deutsch", localName = getLocalName("Deutsch", R.string.language_german)),
                        ZTLanguageModel("ja", R.drawable.ic_flag_ja, "日本語", localName = getLocalName("日本語", R.string.language_japanese)),
                        ZTLanguageModel("ko", R.drawable.ic_flag_ko, "한국어", localName = getLocalName("한국어", R.string.language_korean)),
                        ZTLanguageModel("ru", R.drawable.ic_flag_ru, "Русский", localName = getLocalName("Русский", R.string.language_russian)),
                        ZTLanguageModel("hi", R.drawable.ic_flag_hi, "हिन्दी", localName = getLocalName("हिन्दी", R.string.language_hindi)),
                        ZTLanguageModel("pt", R.drawable.ic_flag_pt, "Português", localName = getLocalName("Português", R.string.language_portuguese)),
                        ZTLanguageModel("tr", R.drawable.ic_flag_tr, "Türkçe", localName = getLocalName("Türkçe", R.string.language_turkish)),
                        ZTLanguageModel("hr", R.drawable.ic_flag_hr, "Hrvatski", localName = getLocalName("Hrvatski", R.string.language_croatian)),
                        ZTLanguageModel("hu", R.drawable.ic_flag_hu, "Magyar", localName = getLocalName("Magyar", R.string.language_hungarian)),
                        ZTLanguageModel("id", R.drawable.ic_flag_id, "Bahasa Indonesia", localName = getLocalName("Bahasa Indonesia", R.string.language_indonesian)),
                        ZTLanguageModel("it", R.drawable.ic_flag_it, "Italiano", localName = getLocalName("Italiano", R.string.language_italian)),
                        ZTLanguageModel("ne", R.drawable.ic_flag_ne, "नेपाली", localName = getLocalName("नेपाली", R.string.language_nepali)),
                        ZTLanguageModel("th", R.drawable.ic_flag_th, "ไทย", localName = getLocalName("ไทย", R.string.language_thai)),
                        ZTLanguageModel("uk", R.drawable.ic_flag_uk, "Українська", localName = getLocalName("Українська", R.string.language_ukrainian)),
                        ZTLanguageModel("zh", R.drawable.ic_flag_zh, "中文", localName = getLocalName("中文", R.string.language_chinese)),
                        ZTLanguageModel("ar", R.drawable.ic_flag_ar, "العربية", localName = getLocalName("العربية", R.string.language_arabic)),
                        ZTLanguageModel("ur", R.drawable.ic_flag_ur, "اردو", localName = getLocalName("اردو", R.string.language_urdu)),
                        ZTLanguageModel("bn", R.drawable.ic_flag_bn, "বাংলা", localName = getLocalName("বাংলা", R.string.language_bengali)),
                        ZTLanguageModel("fil", R.drawable.ic_flag_fil, "Filipino", localName = getLocalName("Filipino", R.string.language_filipino)),
                        ZTLanguageModel("af", R.drawable.ic_flag_af, "Afrikaans", localName = getLocalName("Afrikaans", R.string.language_afrikaans)),
                        ZTLanguageModel("nl", R.drawable.ic_flag_nl, "Nederlands", localName = getLocalName("Nederlands", R.string.language_dutch))
                    ),
                    nextLayoutConfig = ZTFLNextLayoutConfig(
                        nextLayoutButtonEnableRes = R.layout.layout_btn_next_enable,
                        nextLayoutButtonDisableRes = R.layout.layout_btn_next_disable
                    ),
                    languageItemLayoutConfig = ZTFLLanguageItemLayoutConfig(
                        itemLayoutSelectedRes = R.layout.item_language_selected,
                        itemLayoutUnselectedRes = R.layout.item_language_unselected
                    )
                ),
                onboardingConfig = ZTOnboardingConfig(
                    listOnboardingPages = listOf(
                        OnboardingPageConfig(
                            layoutResId = R.layout.layout_ob_1,
                            nativeAdConfigKey = "native_ob_1",
                            adContainerId = R.id.native_container1
                        ),
                        OnboardingNativeFullPageConfig(
                            nativeAdConfigKey = "native_ob_full_12",
                            adContainerId = dev.zentrixa.common.R.id.native_ob_23
                        ),
                        OnboardingPageConfig(
                            layoutResId = R.layout.layout_ob_2,
                            nativeAdConfigKey = "native_ob_2",
                            adContainerId = R.id.native_container2
                        ),
                        OnboardingNativeFullPageConfig(
                            nativeAdConfigKey = "native_ob_full_23",
                            adContainerId = dev.zentrixa.common.R.id.native_ob_23
                        ),
                        OnboardingPageConfig(
                            layoutResId = R.layout.layout_ob_3,
                            nativeAdConfigKey = "native_ob_3",
                            adContainerId = R.id.native_container3
                        )
                    )
                ),
                onChangeLanguage = { language ->
                    AppLanguageManager.setPendingLanguage(language)
                    AppLanguageManager.syncAppCompatLanguageIfNeeded(language)
                    CoroutineScope(Dispatchers.IO).launch {
                        appPreferences.setLanguageSelected(true, language)
                    }
                },
                onFinishFirstOpen = { activity ->
                    ZTUtils.setDoneFirstOpen()

                    runBlocking {
                        try {
                            appPreferences.setOnboardCompleted(true)
                        } catch (e: Exception) {
                            // Ignored or handle error if needed
                        }
                    }

                    val intent = Intent(activity, MainActivity::class.java)
                    activity.startActivity(intent)
                    activity.finish()
                }
            )
        )
    }

    private fun getLocalName(name: String, resId: Int): String {
        return ""
    }
}