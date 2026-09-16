package com.werare.proxy

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.google.android.material.textfield.TextInputEditText

class MainActivity : AppCompatActivity() {

    companion object {
        private const val PASSWORD = "t.me/weraremods"
        private const val REQ_NOTIFICATIONS = 42
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS), REQ_NOTIFICATIONS)
        }

        val prefs = Prefs(this)
        if (prefs.unlocked) showMain() else showGate(prefs)
    }

    private fun showGate(prefs: Prefs) {
        findViewById<View>(R.id.gate).visibility = View.VISIBLE
        findViewById<View>(R.id.main).visibility = View.GONE

        val edtPassword = findViewById<TextInputEditText>(R.id.edtPassword)
        val txtError = findViewById<View>(R.id.txtGateError)

        findViewById<View>(R.id.btnEnter).setOnClickListener {
            val entered = edtPassword.text?.toString()?.trim()
            if (entered == PASSWORD) {
                prefs.unlocked = true
                showMain()
            } else {
                txtError.visibility = View.VISIBLE
                edtPassword.text?.clear()
            }
        }
    }

    private fun showMain() {
        findViewById<View>(R.id.gate).visibility = View.GONE
        findViewById<View>(R.id.main).visibility = View.VISIBLE

        val pager = findViewById<androidx.viewpager2.widget.ViewPager2>(R.id.viewPager)
        pager.adapter = TabsAdapter(this)
        val tabs = findViewById<TabLayout>(R.id.tabLayout)
        TabLayoutMediator(tabs, pager) { tab, position ->
            tab.text = listOf("Прокси", "Конфиги", "Настройки")[position]
        }.attach()
    }

    private inner class TabsAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {
        override fun getItemCount(): Int = 3
        override fun createFragment(position: Int): Fragment = when (position) {
            0 -> ProxyFragment()
            1 -> ConfigsFragment()
            else -> SettingsFragment()
        }
    }
}
