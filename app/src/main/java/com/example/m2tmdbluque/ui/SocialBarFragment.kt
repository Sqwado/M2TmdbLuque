package com.example.m2tmdbluque.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.m2tmdbluque.R
import com.example.m2tmdbluque.databinding.FragmentSocialBarBinding

class SocialBarFragment : Fragment() {

    private var _binding: FragmentSocialBarBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SocialBarViewModel by activityViewModels()

    val personId: Int
        get() = requireArguments().getInt(ARG_PERSON_ID)

    private val personName: String
        get() = requireArguments().getString(ARG_PERSON_NAME).orEmpty()

    override fun onAttach(context: Context) {
        super.onAttach(context)
        Log.d(TAG, "onAttach personId=$personId")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate personId=$personId")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        Log.d(TAG, "onCreateView personId=$personId")
        _binding = FragmentSocialBarBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(TAG, "onViewCreated personId=$personId")

        binding.favoriteIv.setOnClickListener { viewModel.toggleFavorite(personId) }
        binding.likeIv.setOnClickListener { viewModel.incrementLikes(personId) }
        binding.shareIv.setOnClickListener { sharePerson() }

        viewModel.uiState(personId).observe(viewLifecycleOwner) { state ->
            render(state)
        }
    }

    override fun onStart() {
        super.onStart()
        Log.d(TAG, "onStart personId=$personId")
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume personId=$personId")
    }

    override fun onPause() {
        Log.d(TAG, "onPause personId=$personId")
        super.onPause()
    }

    override fun onStop() {
        Log.d(TAG, "onStop personId=$personId")
        super.onStop()
    }

    override fun onDestroyView() {
        Log.d(TAG, "onDestroyView personId=$personId")
        _binding = null
        super.onDestroyView()
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy personId=$personId")
        super.onDestroy()
    }

    override fun onDetach() {
        Log.d(TAG, "onDetach personId=$personId")
        super.onDetach()
    }

    private fun render(state: SocialBarUiState) {
        val ctx = requireContext()
        val inactive = ContextCompat.getColor(ctx, R.color.md_theme_outline)
        val favoriteActive = ContextCompat.getColor(ctx, R.color.md_theme_error)
        val likeActive = ContextCompat.getColor(ctx, R.color.md_theme_primary)

        binding.favoriteIv.setImageResource(
            if (state.isFavorite) R.drawable.ic_favorite else R.drawable.ic_star_border,
        )
        binding.favoriteIv.imageAlpha = if (state.isFavorite) 255 else 180
        binding.favoriteIv.setColorFilter(
            if (state.isFavorite) favoriteActive else inactive,
        )
        binding.likeIv.setImageResource(
            if (state.likeCount > 0) R.drawable.ic_like else R.drawable.ic_favorite_border,
        )
        binding.likeIv.imageAlpha = if (state.likeCount > 0) 255 else 180
        binding.likeIv.setColorFilter(
            if (state.likeCount > 0) likeActive else inactive,
        )
        binding.shareIv.imageAlpha = 180
        binding.shareIv.setColorFilter(inactive)
        binding.nbLikeTv.text = state.likeCount.toString()
        binding.nbLikeTv.setTextColor(
            if (state.likeCount > 0) likeActive else inactive,
        )
    }

    private fun sharePerson() {
        val text = getString(R.string.social_share_text, personName.ifBlank { getString(R.string.app_name) })
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        startActivity(Intent.createChooser(intent, getString(R.string.social_share)))
    }

    companion object {
        private const val TAG = "SocialBarFragment"
        private const val ARG_PERSON_ID = "person_id"
        private const val ARG_PERSON_NAME = "person_name"

        fun newInstance(personId: Int, personName: String = ""): SocialBarFragment =
            SocialBarFragment().apply {
                arguments = bundleOf(
                    ARG_PERSON_ID to personId,
                    ARG_PERSON_NAME to personName,
                )
            }
    }
}
