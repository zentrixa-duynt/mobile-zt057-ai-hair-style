package com.example.aihair.core.ui.base

import android.Manifest
import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.Rect
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.Window
import android.view.WindowManager
import android.widget.EditText
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AppCompatActivity
import androidx.viewbinding.ViewBinding
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.Lifecycle
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.aihair.R
import com.example.aihair.core.language.AppLanguageManager
import com.example.aihair.core.ui.dialog.NoInternetDialogHelper
import com.example.aihair.core.ui.inset.applyEdgeToEdgeContentInsets
import com.example.aihair.core.utils.NetworkUtils
import com.example.aihair.core.utils.hideSoftKeyboard
import com.example.aihair.core.utils.isTouchInsideClickableView
import kotlinx.coroutines.launch

abstract class BaseActivity<VB : ViewBinding>(
    private val bindingInflater: (LayoutInflater) -> VB
) : AppCompatActivity() {

    protected lateinit var binding: VB

    protected open val applyEdgeToEdgeToRoot: Boolean = false
    protected open val observeNetwork: Boolean = true
    private var attachedLanguageTag: String = ""
    
    private var loadingDialog: Dialog? = null

//    protected open fun getBannerConfig(): Triple<String, String, ZTBannerAdUtils.BannerType>? {
//        return Triple("banner_all", "p_banner_all", ZTBannerAdUtils.BannerType.ADAPTIVE)
//    }

    fun showLoading() {
        if (loadingDialog == null) {
            loadingDialog = Dialog(this, android.R.style.Theme_Translucent_NoTitleBar).apply {
                setContentView(R.layout.dialog_loading)
                setCancelable(false)
                
                window?.let { window ->
                    window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
                    window.statusBarColor = Color.TRANSPARENT
                    window.navigationBarColor = Color.TRANSPARENT
                    WindowCompat.setDecorFitsSystemWindows(window, false)
                    
                    val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
                    windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                    windowInsetsController.hide(WindowInsetsCompat.Type.navigationBars())
                }
            }
        }
        if (loadingDialog?.isShowing == false) {
            loadingDialog?.show()
        }
    }

    fun hideLoading() {
        if (loadingDialog?.isShowing == true) {
            loadingDialog?.dismiss()
        }
    }

    override fun attachBaseContext(newBase: Context) {
        val languageTag = AppLanguageManager.languageTagFromIntent(intent)
        attachedLanguageTag = languageTag.orEmpty()

        AppLanguageManager.syncAppCompatLanguageIfNeeded(languageTag)
        super.attachBaseContext(AppLanguageManager.wrapContext(newBase, languageTag))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        window.requestFeature(Window.FEATURE_ACTIVITY_TRANSITIONS)
        super.onCreate(savedInstanceState)

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )
        binding = bindingInflater(layoutInflater)
        AppLanguageManager.applyCurrentLayoutDirection(window.decorView)
        AppLanguageManager.applyCurrentLayoutDirection(binding.root)
        setContentView(binding.root)
        
//        val bannerConfig = getBannerConfig()
//        if (bannerConfig != null) {
//            val bannerView = findViewById<ZTBannerAdView>(R.id.bannerAdView)
//            bannerView?.setConfig(bannerConfig.first, bannerConfig.second, bannerConfig.third)
//        }

        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        windowInsetsController.hide(WindowInsetsCompat.Type.navigationBars())
        
        if (applyEdgeToEdgeToRoot) {
            binding.root.applyEdgeToEdgeContentInsets()
        }

        if (observeNetwork) {
            lifecycleScope.launch {
                repeatOnLifecycle(Lifecycle.State.STARTED) {
                    NetworkUtils.isConnectedFlow.collect { isConnected ->
                        if (!isConnected) {
                            NoInternetDialogHelper.show(this@BaseActivity)
                        } else {
                            NoInternetDialogHelper.dismiss()
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        recreateIfLanguageChanged()
    }

    private fun recreateIfLanguageChanged() {
        val currentLanguageTag = AppLanguageManager.currentLanguageTag()
        if (attachedLanguageTag == currentLanguageTag) {
            return
        }


        intent.putExtra(AppLanguageManager.EXTRA_LANGUAGE_TAG, currentLanguageTag)
        attachedLanguageTag = currentLanguageTag
        recreate()
    }

    override fun onPause() {
        super.onPause()
    }

    override fun onDestroy() {
        if (observeNetwork) {
            NoInternetDialogHelper.dismiss(this)
        }
        super.onDestroy()
    }

    @RequiresPermission(Manifest.permission.ACCESS_NETWORK_STATE)
    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        if (ev?.action == MotionEvent.ACTION_DOWN) {
            val v = currentFocus
            if (v is EditText) {
                val outRect = Rect()
                v.getGlobalVisibleRect(outRect)
                if (!outRect.contains(ev.rawX.toInt(), ev.rawY.toInt())) {
                    hideSoftKeyboard()
                    v.clearFocus()
                }
            }
        }

        if (observeNetwork && ev?.action == MotionEvent.ACTION_DOWN) {
            if (!NetworkUtils.isNetworkAvailable(this)) {
                if (window.decorView.isTouchInsideClickableView(ev)) {
                    NoInternetDialogHelper.show(this)
                    return true
                }
            }
        }
        return super.dispatchTouchEvent(ev)
    }
}
