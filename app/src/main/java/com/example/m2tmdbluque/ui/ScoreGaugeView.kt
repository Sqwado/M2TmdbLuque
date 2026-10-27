package com.example.m2tmdbluque.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import androidx.core.content.withStyledAttributes
import com.example.m2tmdbluque.R
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Jauge horizontale personnalisée (TP ScoreGaugeView).
 * UI pure : label, couleur, valeur et max fournis par l'appelant.
 */
class ScoreGaugeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.LEFT
        textSize = sp(12f)
    }
    private val percentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.RIGHT
        textSize = sp(12f)
        isFakeBoldText = true
    }

    private val trackRect = RectF()
    private val fillRect = RectF()

    var scoreLabel: String = ""
        set(value) {
            field = value
            invalidate()
        }

    var scoreColor: Int = Color.GREEN
        set(value) {
            field = value
            fillPaint.color = value
            percentPaint.color = value
            invalidate()
        }

    var scoreValue: Float = 0f
        set(value) {
            field = value.coerceAtLeast(0f)
            invalidate()
        }

    var scoreMax: Float = 1f
        set(value) {
            field = value.coerceAtLeast(0.01f)
            invalidate()
        }

    init {
        val pad = dp(2f).toInt()
        if (paddingLeft == 0 && paddingTop == 0 && paddingRight == 0 && paddingBottom == 0) {
            setPadding(pad, pad, pad, pad)
        }
        trackPaint.color = resolveThemeColor(com.google.android.material.R.attr.colorSurfaceVariant, Color.LTGRAY)
        labelPaint.color = resolveThemeColor(com.google.android.material.R.attr.colorOnSurfaceVariant, Color.GRAY)

        context.withStyledAttributes(attrs, R.styleable.ScoreGaugeView) {
            scoreLabel = getString(R.styleable.ScoreGaugeView_scoreLabel).orEmpty()
            scoreColor = getColor(R.styleable.ScoreGaugeView_scoreColor, Color.GREEN)
            scoreValue = getFloat(R.styleable.ScoreGaugeView_scoreValue, 0f)
            scoreMax = getFloat(R.styleable.ScoreGaugeView_scoreMax, 1f)
        }
    }

    private fun resolveThemeColor(attr: Int, fallback: Int): Int {
        val typed = TypedValue()
        return if (context.theme.resolveAttribute(attr, typed, true)) {
            if (typed.resourceId != 0) {
                context.getColor(typed.resourceId)
            } else {
                typed.data
            }
        } else {
            fallback
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val desiredHeight = paddingTop + paddingBottom + sp(16f).toInt() + dp(8f).toInt() + dp(10f).toInt()
        setMeasuredDimension(
            resolveSize(suggestedMinimumWidth, widthMeasureSpec),
            resolveSize(desiredHeight, heightMeasureSpec),
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val left = paddingLeft.toFloat()
        val right = (width - paddingRight).toFloat()
        var top = paddingTop.toFloat()

        val percent = ((scoreValue / scoreMax) * 100f).coerceIn(0f, 100f)
        val percentText = context.getString(R.string.score_gauge_percent, percent.roundToInt())
        val labelY = top - labelPaint.ascent()
        canvas.drawText(scoreLabel, left, labelY, labelPaint)
        canvas.drawText(percentText, right, labelY, percentPaint)
        top += sp(16f) + dp(8f)

        val barHeight = dp(10f)
        val radius = barHeight / 2f
        trackRect.set(left, top, right, top + barHeight)
        canvas.drawRoundRect(trackRect, radius, radius, trackPaint)

        val fillWidth = trackRect.width() * (percent / 100f)
        if (fillWidth > 0f) {
            fillRect.set(left, top, left + fillWidth.coerceAtLeast(min(radius * 2f, trackRect.width())), top + barHeight)
            if (fillRect.right > trackRect.right) fillRect.right = trackRect.right
            canvas.drawRoundRect(fillRect, radius, radius, fillPaint)
        }
    }

    private fun dp(value: Float): Float = value * resources.displayMetrics.density
    private fun sp(value: Float): Float = value * resources.displayMetrics.scaledDensity
}
