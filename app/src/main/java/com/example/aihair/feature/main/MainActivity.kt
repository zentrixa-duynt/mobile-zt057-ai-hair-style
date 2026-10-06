package com.example.aihair.feature.main

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.animation.OvershootInterpolator
import androidx.activity.OnBackPressedCallback
import androidx.core.view.children
import androidx.fragment.app.Fragment
import com.example.aihair.R
import com.example.aihair.core.ui.click.setDebouncedClickListener
import com.example.aihair.core.ui.base.BaseActivity
import com.example.aihair.databinding.ActivityMainBinding
import com.example.aihair.feature.main.hair_tools.HairToolsFragment
import com.example.aihair.feature.main.history.HistoryFragment
import com.example.aihair.feature.main.home.HomeFragment
import com.example.aihair.feature.settings.SettingsActivity
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import dev.zentrixa.common.firebase.ZTAnalyticsUtils
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : BaseActivity<ActivityMainBinding>(ActivityMainBinding::inflate) {

    private val homeFragment = HomeFragment()
    private val hairToolsFragment = HairToolsFragment()
    private val historyFragment = HistoryFragment()

    private var activeFragment: Fragment = homeFragment
    private var activeTabIndex = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setupFragments()
        setupBottomNav()
        setupTopBar()
    }

    private fun setupTopBar() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                moveTaskToBack(true)
            }
        })

        binding.ivSettings.setDebouncedClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }
    }

    private fun setupFragments() {
        supportFragmentManager.beginTransaction().apply {
            add(R.id.fragment_container, historyFragment, "history").hide(historyFragment)
            add(R.id.fragment_container, hairToolsFragment, "hair_tools").hide(hairToolsFragment)
            add(R.id.fragment_container, homeFragment, "home")
        }.commit()
    }

    private fun setupBottomNav() {
        binding.navHome.setDebouncedClickListener {
            switchFragment(homeFragment, 0)
            updateBottomNavUI(binding.navHome, animate = true)
        }
        binding.navHairTools.setDebouncedClickListener {
            ZTAnalyticsUtils.logEvent("D_action_hair_tool")
            switchFragment(hairToolsFragment, 1)
            updateBottomNavUI(binding.navHairTools, animate = true)
        }
        binding.navHistory.setDebouncedClickListener {
            ZTAnalyticsUtils.logEvent("D_action_history")
            switchFragment(historyFragment, 2)
            updateBottomNavUI(binding.navHistory, animate = true)
        }

        // Default selected (no animation on startup)
        updateBottomNavUI(binding.navHome, animate = false)
    }

    private fun switchFragment(fragment: Fragment, newIndex: Int) {
        if (activeFragment != fragment) {
            val isMovingRight = newIndex > activeTabIndex
            val animIn = if (isMovingRight) R.anim.slide_in_right else R.anim.slide_in_left
            val animOut = if (isMovingRight) R.anim.slide_out_left else R.anim.slide_out_right
            
            supportFragmentManager.beginTransaction()
                .setCustomAnimations(animIn, animOut)
                .hide(activeFragment)
                .show(fragment)
                .commit()
            activeFragment = fragment
            activeTabIndex = newIndex
        }
    }

    private fun updateBottomNavUI(selectedView: View, animate: Boolean) {
        if (animate) {
            selectedView.animate()
                .scaleX(1.15f)
                .scaleY(1.15f)
                .setDuration(120)
                .withEndAction {
                    selectedView.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(350)
                        .setInterpolator(OvershootInterpolator(2.5f))
                        .start()
                }
                .start()
        }

        val navItems = listOf(binding.navHome, binding.navHairTools, binding.navHistory)
        for (navItem in navItems) {
            val isSelected = navItem == selectedView
            navItem.isSelected = isSelected
            navItem.children.forEach { child ->
                child.isSelected = isSelected
            }
        }
    }
}