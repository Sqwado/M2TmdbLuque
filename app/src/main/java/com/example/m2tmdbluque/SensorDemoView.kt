package com.example.m2tmdbluque

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.text.TextPaint
import android.text.TextUtils
import android.util.AttributeSet
import android.view.View
import androidx.core.content.withStyledAttributes
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Cadran circulaire avec une aiguille colorée par composante.
 * Angle [0..360°] proportionnel à maximumRange — valeurs lissées pour un rendu fluide.
 */
class SensorDemoView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

    private val dialScale = 0.82f
    private val dialNeedleColors: IntArray =
        context.resources.getStringArray(R.array.dial_needle_colors)
            .map { it.toInt() }
            .toIntArray()

    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = dp(10f)
    }
    private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = dp(2.5f)
    }
    private val tickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = dp(1.5f)
        strokeCap = Paint.Cap.ROUND
    }
    private val needlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = dp(3.5f)
        strokeCap = Paint.Cap.ROUND
    }
    private val hubPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val captionPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        textSize = context.resources.getDimension(R.dimen.sensor_view_caption)
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    private val labelPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        textSize = dp(11f)
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }

    private val captionHeight = captionPaint.fontMetrics.let { abs(it.ascent) + abs(it.descent) } * 1.8f

    private var maxRange: Float = 1f
    private var needleCount: Int = 0
    private val targetValues = FloatArray(MAX_NEEDLES)
    private val displayedValues = FloatArray(MAX_NEEDLES)
    private var animating = false

    private val frameCallback = object : Runnable {
        override fun run() {
            var needsMore = false
            for (i in 0 until needleCount) {
                val delta = targetValues[i] - displayedValues[i]
                if (abs(delta) > VALUE_EPSILON) {
                    displayedValues[i] += delta * LERP_FACTOR
                    needsMore = true
                } else {
                    displayedValues[i] = targetValues[i]
                }
            }
            invalidate()
            if (needsMore) {
                postOnAnimation(this)
            } else {
                animating = false
            }
        }
    }

    var dialCaption: String = ""
        set(value) {
            field = value
            invalidate()
        }

    var dialColor: Int = Color.BLUE
        set(value) {
            field = value
            invalidate()
        }

    init {
        context.withStyledAttributes(attrs, R.styleable.SensorDemoView, defStyleAttr, 0) {
            dialCaption = getString(R.styleable.SensorDemoView_dialCaption)
                ?: if (isInEditMode) context.getString(R.string.sensor_unknown) else ""
            dialColor = getColor(R.styleable.SensorDemoView_dialColor, dialColor)
        }
        setLayerType(LAYER_TYPE_HARDWARE, null)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val reqWidth = MeasureSpec.getSize(widthMeasureSpec)
        val reqHeight = MeasureSpec.getSize(heightMeasureSpec)
        val desiredWidth = reqWidth
        val desiredHeight = captionHeight.toInt() + reqHeight

        val width = when (MeasureSpec.getMode(widthMeasureSpec)) {
            MeasureSpec.EXACTLY -> reqWidth
            MeasureSpec.UNSPECIFIED -> desiredWidth
            else -> min(reqWidth, desiredWidth)
        }
        val height = when (MeasureSpec.getMode(heightMeasureSpec)) {
            MeasureSpec.EXACTLY -> reqHeight
            MeasureSpec.UNSPECIFIED -> desiredHeight
            else -> min(reqHeight, desiredHeight)
        }

        if (isInEditMode) {
            setMeasuredDimension(desiredWidth, desiredHeight)
        } else {
            setMeasuredDimension(width, height)
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val cx = measuredWidth * 0.5f
        val cy = (measuredHeight + captionHeight) * 0.5f
        val radius = min(measuredWidth.toFloat(), measuredHeight - captionHeight) * 0.5f * dialScale

        // Piste du cadran
        trackPaint.color = Color.argb(36, Color.red(dialColor), Color.green(dialColor), Color.blue(dialColor))
        canvas.drawCircle(cx, cy, radius, trackPaint)

        ringPaint.color = dialColor
        canvas.drawCircle(cx, cy, radius, ringPaint)

        // Graduations
        tickPaint.color = Color.argb(120, Color.red(dialColor), Color.green(dialColor), Color.blue(dialColor))
        for (tick in 0 until 12) {
            val angle = tick * Math.PI / 6.0
            val inner = radius * 0.88f
            val outer = radius * 0.98f
            canvas.drawLine(
                cx + inner * sin(angle).toFloat(),
                cy - inner * cos(angle).toFloat(),
                cx + outer * sin(angle).toFloat(),
                cy - outer * cos(angle).toFloat(),
                tickPaint,
            )
        }

        val range = maxRange.takeIf { it > 0f } ?: 1f
        for (i in 0 until needleCount) {
            val color = dialNeedleColors[i]
            needlePaint.color = color
            val angleRad = (displayedValues[i] / range) * 2.0 * Math.PI
            val tipX = cx + radius * 0.92f * sin(angleRad).toFloat()
            val tipY = cy - radius * 0.92f * cos(angleRad).toFloat()
            canvas.drawLine(cx, cy, tipX, tipY, needlePaint)

            // Pastille en bout d’aiguille
            hubPaint.color = color
            canvas.drawCircle(tipX, tipY, dp(3.5f), hubPaint)

            // Label composante près du bord
            labelPaint.color = color
            val labelR = radius * 1.12f
            val lx = cx + labelR * sin(angleRad).toFloat()
            val ly = cy - labelR * cos(angleRad).toFloat() + dp(4f)
            canvas.drawText(COMPONENT_LABELS.getOrElse(i) { "v$i" }, lx, ly, labelPaint)
        }

        // Moyeu central
        hubPaint.color = dialColor
        canvas.drawCircle(cx, cy, dp(6f), hubPaint)
        hubPaint.color = Color.WHITE
        canvas.drawCircle(cx, cy, dp(2.5f), hubPaint)

        // Légende tronquée
        captionPaint.color = dialColor
        val maxCaptionWidth = measuredWidth - dp(24f)
        val caption = TextUtils.ellipsize(
            dialCaption,
            captionPaint,
            maxCaptionWidth,
            TextUtils.TruncateAt.END,
        ).toString()
        canvas.drawText(caption, cx, captionHeight * 0.75f, captionPaint)
    }

    override fun onDetachedFromWindow() {
        removeCallbacks(frameCallback)
        animating = false
        super.onDetachedFromWindow()
    }

    fun setCaption(caption: String) {
        dialCaption = caption
    }

    fun clearValues() {
        needleCount = 0
        maxRange = 1f
        targetValues.fill(0f)
        displayedValues.fill(0f)
        removeCallbacks(frameCallback)
        animating = false
        invalidate()
    }

    fun updateValues(values: FloatArray, count: Int, maximumRange: Float) {
        maxRange = maximumRange.takeIf { it > 0f } ?: 1f
        val n = min(count, min(values.size, min(dialNeedleColors.size, MAX_NEEDLES)))
        needleCount = n
        for (i in 0 until n) {
            targetValues[i] = values[i]
        }
        if (!animating) {
            animating = true
            postOnAnimation(frameCallback)
        }
    }

    private fun dp(value: Float): Float = value * resources.displayMetrics.density

    companion object {
        private const val MAX_NEEDLES = 9
        private const val LERP_FACTOR = 0.28f
        private const val VALUE_EPSILON = 0.002f
        private val COMPONENT_LABELS = arrayOf("X", "Y", "Z", "v3", "v4", "v5", "v6", "v7", "v8")
    }
}
