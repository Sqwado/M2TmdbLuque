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

        val imageUrl = TmdbClient.profileImageUrl(person.profilePath, large = true)
        if (imageUrl.isNullOrBlank()) {
            binding.imageProfile.setImageResource(R.drawable.bg_placeholder_person)
        } else {
            Picasso.get()
                .load(imageUrl)
                .placeholder(R.drawable.bg_placeholder_person)
                .error(R.drawable.bg_placeholder_person)
                .fit()
                .centerCrop()
                .into(binding.imageProfile)
        }

        binding.recyclerKnownFor.layoutManager = LinearLayoutManager(this)
        binding.recyclerKnownFor.adapter = KnownForAdapter(person.knownFor)
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

        fun newIntent(context: Context, person: Person): Intent =
            Intent(context, PersonDetailActivity::class.java).putExtra(EXTRA_PERSON, person)
    }
}
