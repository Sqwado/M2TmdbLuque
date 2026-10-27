package com.example.m2tmdbluque

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.m2tmdbluque.api.TmdbClient
import com.example.m2tmdbluque.databinding.ActivityPersonDetailBinding
import com.example.m2tmdbluque.model.Person
import com.example.m2tmdbluque.ui.KnownForAdapter
import com.example.m2tmdbluque.ui.ScoreRating
import com.squareup.picasso.Picasso

class PersonDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPersonDetailBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = DataBindingUtil.setContentView(this, R.layout.activity_person_detail)

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout(),
            )
            v.updatePadding(
                left = bars.left,
                top = bars.top,
                right = bars.right,
                bottom = bars.bottom,
            )
            insets
        }

        val person = readPersonExtra()
        if (person == null) {
            finish()
            return
        }

        binding.person = person
        binding.toolbar.title = person.name
        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.toolbar.setNavigationIcon(androidx.appcompat.R.drawable.abc_ic_ab_back_material)

        binding.textDepartment.text =
            person.knownForDepartment ?: getString(R.string.value_unavailable)
        binding.textGender.text = person.genderLabel(this)
        binding.textPopularity.text = getString(R.string.person_popularity, person.popularity)

        bindScoreGauge(person)

        val imageUrl = TmdbClient.profileImageUrl(person.profilePath, large = true)
        if (imageUrl == null) {
            binding.imageProfile.setImageResource(R.drawable.bg_no_person_photo)
        } else {
            Picasso.get()
                .load(imageUrl)
                .placeholder(R.drawable.bg_placeholder_person)
                .error(R.drawable.bg_no_person_photo)
                .fit()
                .centerCrop()
                .into(binding.imageProfile)
        }

        binding.recyclerKnownFor.layoutManager = LinearLayoutManager(this)
        binding.recyclerKnownFor.adapter = KnownForAdapter(person.knownFor)
    }

    private fun bindScoreGauge(person: Person) {
        val maxPopularity = intent.getDoubleExtra(EXTRA_MAX_POPULARITY, 0.0)
            .takeIf { it > 0.0 }
            ?: person.popularity.coerceAtLeast(1.0)
        val percent = ((person.popularity / maxPopularity) * 100.0).toFloat().coerceIn(0f, 100f)

        binding.scoreGauge.scoreValue = person.popularity.toFloat()
        binding.scoreGauge.scoreMax = maxPopularity.toFloat()
        binding.scoreGauge.scoreColor = ScoreRating.getScoreColor(this, percent)
        binding.scoreGauge.scoreLabel = ScoreRating.getRating(this, percent)
    }

    private fun readPersonExtra(): Person? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getSerializableExtra(EXTRA_PERSON, Person::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getSerializableExtra(EXTRA_PERSON) as? Person
        }

    companion object {
        private const val EXTRA_PERSON = "extra_person"
        private const val EXTRA_MAX_POPULARITY = "extra_max_popularity"

        fun newIntent(
            context: Context,
            person: Person,
            maxPopularity: Double = person.popularity.coerceAtLeast(1.0),
        ): Intent =
            Intent(context, PersonDetailActivity::class.java)
                .putExtra(EXTRA_PERSON, person)
                .putExtra(EXTRA_MAX_POPULARITY, maxPopularity)
    }
}
