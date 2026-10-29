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
 * Jauge horizontale moderne (API TP conservée : scoreLabel / scoreColor / scoreValue / scoreMax).
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
        isFakeBoldText = true
    }
    private val percentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.RIGHT
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
        val padH = dp(0f).toInt()
        val padV = dp(1f).toInt()
        if (paddingLeft == 0 && paddingTop == 0 && paddingRight == 0 && paddingBottom == 0) {
            setPadding(padH, padV, padH, padV)
        }
        trackPaint.color = resolveThemeColor(
            com.google.android.material.R.attr.colorSurfaceVariant,
            Color.LTGRAY,
        )
        labelPaint.color = resolveThemeColor(
            com.google.android.material.R.attr.colorOnSurfaceVariant,
            Color.GRAY,
        )

        context.withStyledAttributes(attrs, R.styleable.ScoreGaugeView) {
            scoreMax = getFloat(R.styleable.ScoreGaugeView_scoreMax, 100f)
            scoreValue = getFloat(R.styleable.ScoreGaugeView_scoreValue, 75f)
            scoreColor = getColor(R.styleable.ScoreGaugeView_scoreColor, Color.GREEN)
            scoreLabel = getString(R.styleable.ScoreGaugeView_scoreLabel)
                ?: if (isInEditMode) context.getString(R.string.no_label) else ""
        }
    }

    private fun resolveThemeColor(attr: Int, fallback: Int): Int {
        val typed = TypedValue()
        return if (context.theme.resolveAttribute(attr, typed, true)) {
            if (typed.resourceId != 0) context.getColor(typed.resourceId) else typed.data
        } else {
            fallback
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val labelSize = resources.getDimension(R.dimen.score_gauge_view_label)
        val desiredHeight = paddingTop + paddingBottom +
            labelSize.toInt() + dp(4f).toInt() + dp(7f).toInt()
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

        val labelSize = resources.getDimension(R.dimen.score_gauge_view_label)
        labelPaint.textSize = labelSize
        percentPaint.textSize = labelSize

        val percent = ((scoreValue / scoreMax) * 100f).coerceIn(0f, 100f)
        val percentText = context.getString(R.string.score_gauge_percent, percent.roundToInt())
        val labelY = top - labelPaint.ascent()
        canvas.drawText(scoreLabel, left, labelY, labelPaint)
        canvas.drawText(percentText, right, labelY, percentPaint)
        top += labelSize + dp(4f)

        val barHeight = dp(7f)
        val radius = barHeight / 2f
        trackRect.set(left, top, right, top + barHeight)
        canvas.drawRoundRect(trackRect, radius, radius, trackPaint)

        val fillWidth = trackRect.width() * (percent / 100f)
        if (fillWidth > 0f) {
            fillRect.set(
                left,
                top,
                left + fillWidth.coerceAtLeast(min(radius * 2f, trackRect.width())),
                top + barHeight,
            )
            if (fillRect.right > trackRect.right) fillRect.right = trackRect.right
            canvas.drawRoundRect(fillRect, radius, radius, fillPaint)
        }
    }

    private fun dp(value: Float): Float = value * resources.displayMetrics.density
}
