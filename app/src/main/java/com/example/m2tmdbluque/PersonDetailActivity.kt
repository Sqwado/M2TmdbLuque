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
import com.bumptech.glide.Glide
import com.example.m2tmdbluque.api.TmdbClient
import com.example.m2tmdbluque.databinding.ActivityPersonDetailBinding
import com.example.m2tmdbluque.model.Person
import com.example.m2tmdbluque.ui.KnownForAdapter

class PersonDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPersonDetailBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = DataBindingUtil.setContentView(this, R.layout.activity_person_detail)

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.updatePadding(top = bars.top, bottom = bars.bottom)
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

        binding.textDepartment.text = person.knownForDepartment ?: "—"
        binding.textGender.text = person.genderLabel(this)

        Glide.with(binding.imageProfile)
            .load(TmdbClient.profileImageUrl(person.profilePath, large = true))
            .placeholder(R.drawable.bg_placeholder_person)
            .error(R.drawable.bg_placeholder_person)
            .centerCrop()
            .into(binding.imageProfile)

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
