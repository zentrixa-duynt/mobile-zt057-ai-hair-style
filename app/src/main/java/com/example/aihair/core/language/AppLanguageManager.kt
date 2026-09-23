package com.example.aihair.core.language

import android.app.LocaleManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import android.os.LocaleList
import android.text.TextUtils
import android.view.View
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.example.aihair.core.data.local.datastore.AppPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.Locale

object AppLanguageManager {
    const val EXTRA_LANGUAGE_TAG = "extra_language_tag"
    @Volatile private var pendingLanguageTag: String? = null

    fun setPendingLanguage(languageTag: String?) {
        pendingLanguageTag = languageTag

    }

    fun languageTagFromIntent(intent: Intent?): String? {
        val intentLanguageTag = intent?.getStringExtra(EXTRA_LANGUAGE_TAG)
        val languageTag = intentLanguageTag ?: pendingLanguageTag

        return languageTag
    }

    fun currentLanguageTag(): String {
        val languageTag = pendingLanguageTag
            ?: AppCompatDelegate.getApplicationLocales().toLanguageTags()

        return languageTag
    }

    fun applyLanguage(languageTag: String?) {
        val locales = if (languageTag.isNullOrBlank()) {
            LocaleListCompat.getEmptyLocaleList()
        } else {
            LocaleListCompat.forLanguageTags(languageTag)
        }
        AppCompatDelegate.setApplicationLocales(locales)
    }

    fun syncAppCompatLanguageIfNeeded(languageTag: String?) {
        val currentLanguageTags = AppCompatDelegate.getApplicationLocales().toLanguageTags()
        val targetLanguageTags = languageTag.orEmpty()
        if (currentLanguageTags == targetLanguageTags) {

            return
        }


        applyLanguage(languageTag)
    }

    fun wrapContext(base: Context, languageTag: String?): Context {
        val locale = if (languageTag.isNullOrBlank()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val localeManager = base.getSystemService(Context.LOCALE_SERVICE) as LocaleManager
                val sysLocales = localeManager.systemLocales
                if (!sysLocales.isEmpty) sysLocales.get(0) else Locale.getDefault()
            } else {
                Resources.getSystem().configuration.locales.get(0)
            }
        } else {
            Locale.forLanguageTag(languageTag)
        }
        
        Locale.setDefault(locale)

        val config = Configuration(base.resources.configuration)
        config.setLocale(locale)
        config.setLocales(LocaleList(locale))
        config.setLayoutDirection(locale)
        
        val appCtx = base.applicationContext
        if (appCtx != null) {
            val appConfig = Configuration(appCtx.resources.configuration)
            appConfig.setLocale(locale)
            appConfig.setLocales(LocaleList(locale))
            appConfig.setLayoutDirection(locale)
            @Suppress("DEPRECATION")
            appCtx.resources.updateConfiguration(appConfig, appCtx.resources.displayMetrics)
        }

        return base.createConfigurationContext(config)
    }

    fun applySavedLanguage(appPreferences: AppPreferences) {
        val launchState = runBlocking {
            appPreferences.launchState.first()
        }
        
        if (launchState.isLanguageSelected && !launchState.isOnboardCompleted) {
            CoroutineScope(Dispatchers.IO).launch {
                appPreferences.setLanguageSelected(false, "")
            }
        }

        val effectiveLanguage = launchState.effectiveLanguage

        setPendingLanguage(effectiveLanguage)
        applyLanguage(effectiveLanguage)
    }

    fun currentLayoutDirection(): Int {
        val locale = pendingLanguageTag
            ?.takeIf { it.isNotBlank() }
            ?.let(Locale::forLanguageTag)
            ?: AppCompatDelegate.getApplicationLocales().get(0)
            ?: Locale.getDefault()
        return TextUtils.getLayoutDirectionFromLocale(locale)
    }

    fun applyCurrentLayoutDirection(view: View) {
        view.layoutDirection = currentLayoutDirection()
    }
}
