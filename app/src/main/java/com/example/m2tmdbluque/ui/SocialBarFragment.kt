package com.example.m2tmdbluque.ui

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.m2tmdbluque.R
import com.example.m2tmdbluque.databinding.FragmentSocialBarBinding

private const val SBF_KEY = "sbf_key"
private const val ARG_PERSON_NAME = "person_name"

class SocialBarFragment : Fragment() {

    private var _binding: FragmentSocialBarBinding? = null
    private val binding get() = _binding!!

    private var cn: Int = Color.LTGRAY
    private var cs: Int = Color.RED

    /** Clé dans les maps du ViewModel (= id TMDB de la personne). */
    private var mapkey: Int = 0

    private val personName: String
        get() = requireArguments().getString(ARG_PERSON_NAME).orEmpty()

    val personId: Int
        get() = mapkey

    private val viewModel by activityViewModels<SocialBarViewModel>()

    override fun onAttach(context: Context) {
        super.onAttach(context)
        Log.d(TAG, "onAttach mapkey=$mapkey")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mapkey = requireArguments().getInt(SBF_KEY)
        Log.d(TAG, "onCreate mapkey=$mapkey")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        Log.d(TAG, "onCreateView mapkey=$mapkey")
        _binding = FragmentSocialBarBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(TAG, "onViewCreated mapkey=$mapkey")

        cn = requireContext().getColor(R.color.ic_social_normal)
        cs = requireContext().getColor(R.color.ic_social_selected)

        binding.favoriteIv.setOnClickListener { viewModel.toggleFavorite(mapkey) }
        binding.likeIv.setOnClickListener { viewModel.incrementLikes(mapkey) }
        binding.shareIv.setOnClickListener { sharePerson() }

        viewModel.revision.observe(viewLifecycleOwner) { render() }
        render()
    }

    override fun onStart() {
        super.onStart()
        Log.d(TAG, "onStart mapkey=$mapkey")
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume mapkey=$mapkey")
    }

    override fun onPause() {
        Log.d(TAG, "onPause mapkey=$mapkey")
        super.onPause()
    }

    override fun onStop() {
        Log.d(TAG, "onStop mapkey=$mapkey")
        super.onStop()
    }

    override fun onDestroyView() {
        Log.d(TAG, "onDestroyView mapkey=$mapkey")
        _binding = null
        super.onDestroyView()
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy mapkey=$mapkey")
        super.onDestroy()
    }

    override fun onDetach() {
        Log.d(TAG, "onDetach mapkey=$mapkey")
        super.onDetach()
    }

    private fun render() {
        val favorite = viewModel.isFavoriteOf(mapkey)
        val likes = viewModel.likesOf(mapkey)

        binding.favoriteIv.setImageResource(
            if (favorite) R.drawable.ic_favorite else R.drawable.ic_star_border,
        )
        binding.favoriteIv.imageAlpha = if (favorite) 255 else 180
        binding.favoriteIv.setColorFilter(if (favorite) cs else cn)

        binding.likeIv.setImageResource(
            if (likes > 0) R.drawable.ic_like else R.drawable.ic_favorite_border,
        )
        binding.likeIv.imageAlpha = if (likes > 0) 255 else 180
        binding.likeIv.setColorFilter(if (likes > 0) cs else cn)

        binding.shareIv.imageAlpha = 180
        binding.shareIv.setColorFilter(cn)

        binding.nbLikeTv.text = likes.toString()
        binding.nbLikeTv.setTextColor(if (likes > 0) cs else cn)
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

        /** @param mapkey id personne (clé des maps ViewModel) */
        fun newInstance(mapkey: Int, personName: String = ""): SocialBarFragment =
            SocialBarFragment().apply {
                arguments = bundleOf(
                    SBF_KEY to mapkey,
                    ARG_PERSON_NAME to personName,
                )
            }
    }
}
