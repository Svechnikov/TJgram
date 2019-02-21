package io.svechnikov.tjgram.features.addpost.sendimage

import android.app.Activity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import io.svechnikov.tjgram.R
import io.svechnikov.tjgram.base.MainEvent
import io.svechnikov.tjgram.base.MainViewModel
import io.svechnikov.tjgram.base.di.Injectable
import io.svechnikov.tjgram.base.di.ViewModelFactory
import io.svechnikov.tjgram.databinding.FragmentSendImageBinding
import javax.inject.Inject


class SendImageFragment : Fragment(), Injectable {

    @Inject
    lateinit var viewModelFactory: ViewModelFactory

    lateinit var viewModel: SendImageViewModel

    private lateinit var binding: FragmentSendImageBinding

    val args by navArgs<SendImageFragmentArgs>()

    fun navController() = findNavController()

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        binding = FragmentSendImageBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        viewModel = ViewModelProviders.of(this,
            viewModelFactory)[SendImageViewModel::class.java]

        initViews()

        viewModel.setImageId(args.imageId)

        viewModel.image.observe(viewLifecycleOwner, Observer {
            binding.image = it
        })

        val mainViewModel = ViewModelProviders.of(
            activity!!, viewModelFactory)[MainViewModel::class.java]

        mainViewModel.setBottomBarVisibility(false)

        viewModel.event.observe(viewLifecycleOwner, Observer {
            when(it) {
                is SendImageEvent.ShowError -> {
                    mainViewModel.setEvent(MainEvent.ShowMessage(it.message))
                }
                is SendImageEvent.Success -> {
                    mainViewModel.setEvent(MainEvent.ShowMessage(it.message))
                    navController().navigate(
                        SendImageFragmentDirections.actionSendImageFragmentToTimelineFragment())
                }
            }
        })

        viewModel.state.observe(viewLifecycleOwner, Observer {
            when(it) {
                SendImageState.UPLOADING -> {
                    binding.send.isEnabled = false
                    binding.send.text = getString(R.string.send_image_in_progress)
                }
                SendImageState.IDLE -> {
                    binding.send.isEnabled = true
                    binding.send.text = getString(R.string.generic_ok)
                }
                else -> {

                }
            }
        })
    }

    private fun initViews() {
        val textWatcher = EmptyTextWatcher()
        binding.text.addTextChangedListener(textWatcher)
        binding.title.addTextChangedListener(textWatcher)
        binding.send.setOnClickListener {
            hideKeyboard()
            send()
        }
        binding.text.setOnEditorActionListener {_,action,_ ->
            if (action == EditorInfo.IME_ACTION_SEND) {
                send()
                true
            }
            false
        }
    }

    private fun hideKeyboard() {
        activity?.let {
            val imm = it.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
            var view = it.currentFocus
            if (view == null) {
                view = View(activity)
            }
            imm.hideSoftInputFromWindow(view.windowToken, 0)
        }
    }

    private fun send() {
        if (dataNotEmpty()) {
            val title = binding.title.text.toString().trim()
            val text = binding.text.text.toString().trim()

            viewModel.send(title, text)
        }
    }

    private fun dataNotEmpty(): Boolean {
        val title = binding.title
        val text = binding.text

        return !title.text.trim().isEmpty() && !text.text.trim().isEmpty()
    }

    inner class EmptyTextWatcher : TextWatcher {
        override fun afterTextChanged(s: Editable?) {

            binding.send.isEnabled = dataNotEmpty()
        }

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

        }

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

        }
    }
}