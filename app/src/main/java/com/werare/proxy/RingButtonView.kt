package com.werare.proxy

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.View
import kotlin.math.min

/**
 * Квадратная кнопка с мягкими краями, секундным счётчиком
 * и полоской-прогрессом вокруг кнопки.
 */
class RingButtonView @JvmOverloads constructor(
    ctx: Context,
    attrs: AttributeSet? = null
) : View(ctx, attrs) {

    /** Основной цвет заливки и кольца. */
    var color: Int = Color.parseColor("#AEEA00")
        set(v) { field = v; refreshPaints(); invalidate() }

    /** Радиус скругления углов, px. */
    var cornerRadius: Float = 40f
        set(v) { field = v; invalidate() }

    /** Остаток времени, 1.0 -> 0.0. */
    var ringProgress: Float = 1f
        set(v) { field = v.coerceIn(0f, 1f); invalidate() }

    /** Текст счётчика секунд (пусто = не показывать). */
    var secondsText: String = ""
        set(v) { field = v; invalidate() }

    /** Основной текст кнопки (GO / STOP). */
    var label: String = "GO"
        set(v) { field = v; invalidate() }

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 12f
        strokeCap = Paint.Cap.ROUND
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
    }

    init { isClickable = true; refreshPaints() }

    private fun refreshPaints() {
        fillPaint.color = color
        ringPaint.color = color
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val size = min(MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.getSize(heightMeasureSpec))
        setMeasuredDimension(size, size)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        val pad = ringPaint.strokeWidth

        // квадрат с мягкими краями
        fillPaint.color = color
        canvas.drawRoundRect(RectF(pad, pad, w - pad, h - pad), cornerRadius, cornerRadius, fillPaint)

        // полоска вокруг кнопки
        val cx = w / 2f
        val cy = h / 2f
        val r = min(w, h) / 2f - pad / 2f
        if (ringProgress < 1f) {
            canvas.drawArc(
                cx - r, cy - r, cx + r, cy + r,
                -90f, 360f * ringProgress, false, ringPaint
            )
        }

        // текст
        val txt = secondsText.ifEmpty { label }
        textPaint.textSize = min(w, h) * (if (secondsText.isNotEmpty()) 0.34f else 0.22f)
        val baseline = cy - (textPaint.descent() + textPaint.ascent()) / 2f
        canvas.drawText(txt, cx, baseline, textPaint)
    }
}
