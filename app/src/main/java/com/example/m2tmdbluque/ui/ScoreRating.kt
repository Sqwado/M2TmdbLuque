package com.example.m2tmdbluque.ui

import android.content.Context
import com.example.m2tmdbluque.R
import kotlin.math.min

/** Logique métier des légendes / couleurs de jauge (hors UI ScoreGaugeView). */
object ScoreRating {

    fun popularityPercent(popularity: Double, maxPopularity: Double): Float {
        val max = maxPopularity.coerceAtLeast(1.0)
        return ((popularity / max) * 100.0).toFloat().coerceIn(0f, 100f)
    }

    /** Remplit la jauge : calcul % + libellé/couleur, sans logique dans les écrans. */
    fun bindGauge(
        gauge: ScoreGaugeView,
        context: Context,
        popularity: Double,
        maxPopularity: Double,
    ) {
        val max = maxPopularity.coerceAtLeast(1.0)
        val percent = popularityPercent(popularity, max)
        gauge.scoreValue = popularity.toFloat()
        gauge.scoreMax = max.toFloat()
        gauge.scoreColor = getScoreColor(context, percent)
        gauge.scoreLabel = getRating(context, percent)
    }

    fun getScoreColor(context: Context, percent: Float): Int {
        val colors = context.resources.getStringArray(R.array.rating_colors)
        return colors[ratingIndex(percent, colors.size)].toInt()
    }

    fun getRating(context: Context, percent: Float): String {
        val ratings = context.resources.getStringArray(R.array.score_rating)
        return ratings[ratingIndex(percent, ratings.size)]
    }

    private fun ratingIndex(percent: Float, size: Int): Int {
        if (size <= 1) return 0
        val bucket = (percent.coerceIn(0f, 100f) / 100f * size).toInt()
        return min(bucket, size - 1).coerceAtLeast(0)
    }
}
