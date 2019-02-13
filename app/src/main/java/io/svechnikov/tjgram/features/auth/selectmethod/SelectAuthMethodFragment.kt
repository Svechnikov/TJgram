package io.svechnikov.tjgram.features.auth.selectmethod

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.fragment.findNavController
import io.svechnikov.tjgram.R
import io.svechnikov.tjgram.base.MainViewModel
import io.svechnikov.tjgram.base.di.Injectable
import io.svechnikov.tjgram.databinding.FragmentAuthSelectMethodBinding
import javax.inject.Inject

/**
 * На будущее - возможность добавлять другие способы авторизации
 */
class SelectAuthMethodFragment : Fragment(), Injectable {

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private lateinit var viewModel: SelectAuthMethodViewModel

    private lateinit var binding: FragmentAuthSelectMethodBinding

    fun navController() = findNavController()

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        binding = FragmentAuthSelectMethodBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        viewModel = ViewModelProviders.of(
            this, viewModelFactory)[SelectAuthMethodViewModel::class.java]

        val mainViewModel = ViewModelProviders.of(
            activity!!, viewModelFactory)[MainViewModel::class.java]

        mainViewModel.setBottomBarVisibility(false)

        initViews()

        viewModel.event.observe(viewLifecycleOwner, Observer {
            it?.let {
                viewModel.eventHandled()

                when(it) {
                    SelectAuthMethodEvent.NavigateToQr -> {
                        navController().navigate(
                            R.id.action_selectAuthMethodFragment_to_qrAuthFragment)
                    }
                }
            }
        })
    }

    private fun initViews() {
        binding.authWithQrButton.setOnClickListener {
            viewModel.authWithQr()
        }
    }
}