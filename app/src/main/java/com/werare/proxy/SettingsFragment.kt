package com.werare.proxy

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.android.material.slider.Slider
import com.google.android.material.switchmaterial.SwitchMaterial

class SettingsFragment : Fragment(R.layout.fragment_settings) {

    private val presetColors = listOf("#AEEA00", "#FF5252", "#40C4FF", "#FFD740", "#E040FB", "#69F0AE")

    private lateinit var prefs: Prefs

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        prefs = Prefs(requireContext())

        buildColorRow(view)

        bindSlider(view, R.id.sliderSize, R.id.txtSizeValue, prefs.buttonSize, 56f, 240f, "dp") { prefs.buttonSize = it }
        bindSlider(view, R.id.sliderRadius, R.id.txtRadiusValue, prefs.cornerRadius, 0f, 60f, "dp") { prefs.cornerRadius = it }
        bindSlider(view, R.id.sliderOpacity, R.id.txtOpacityValue, prefs.buttonOpacity, 20f, 100f, "%") { prefs.buttonOpacity = it }
        bindSlider(view, R.id.sliderDuration, R.id.txtDurationValue, prefs.durationSec, 10f, 600f, "сек") { prefs.durationSec = it }
        bindSlider(view, R.id.sliderLag, R.id.txtLagValue, prefs.lagMs, 0f, 2000f, "мс") { prefs.lagMs = it }
        bindSlider(view, R.id.sliderLoss, R.id.txtLossValue, prefs.lossPercent, 0f, 100f, "%") { prefs.lossPercent = it }

        val switch = view.findViewById<SwitchMaterial>(R.id.switchOverlay)
        switch.isChecked = prefs.showOverlayButton
        switch.setOnCheckedChangeListener { _, isChecked -> prefs.showOverlayButton = isChecked }
    }

    private fun bindSlider(
        view: View,
        sliderId: Int,
        valueId: Int,
        current: Int,
        from: Float,
        to: Float,
        unit: String,
        save: (Int) -> Unit
    ) {
        val slider = view.findViewById<Slider>(sliderId)
        val label = view.findViewById<TextView>(valueId)
        slider.valueFrom = from
        slider.valueTo = to
        slider.value = current.coerceIn(from.toInt(), to.toInt()).toFloat()
        label.text = "${slider.value.toInt()} $unit"
        slider.addOnChangeListener { _, v, _ ->
            label.text = "${v.toInt()} $unit"
            save(v.toInt())
        }
    }

    private fun buildColorRow(view: View) {
        val row = view.findViewById<LinearLayout>(R.id.rowColors)
        row.removeAllViews()
        presetColors.forEach { hex ->
            val c = Color.parseColor(hex)
            val drawable = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(c)
                setStroke(dp(3), if (c == prefs.buttonColor) Color.WHITE else Color.TRANSPARENT)
            }
            val dot = View(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(dp(40), dp(40)).apply { marginEnd = dp(10) }
                background = drawable
                setOnClickListener {
                    prefs.buttonColor = c
                    for (i in 0 until row.childCount) {
                        (row.getChildAt(i).background as GradientDrawable)
                            .setStroke(dp(3), Color.TRANSPARENT)
                    }
                    drawable.setStroke(dp(3), Color.WHITE)
                }
            }
            row.addView(dot)
        }
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()
}
