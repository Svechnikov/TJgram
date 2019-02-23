package io.svechnikov.tjgram.features.addpost.selectimage.pickimage

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import io.svechnikov.tjgram.base.di.Injectable
import io.svechnikov.tjgram.databinding.FragmentPickImageBinding
import io.svechnikov.tjgram.features.addpost.selectimage.SelectImageEvent
import io.svechnikov.tjgram.features.addpost.selectimage.SelectImageViewModel
import javax.inject.Inject

class PickImageFragment : Fragment(), Injectable {

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    @Inject
    lateinit var imagesAdapterFactory: ImagesAdapter.Factory

    private lateinit var viewModel: PickImageViewModel

    private lateinit var binding: FragmentPickImageBinding

    private lateinit var parentViewModel: SelectImageViewModel

    companion object {
        fun newInstance(): PickImageFragment {
            return PickImageFragment()
        }
    }

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        binding = FragmentPickImageBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        viewModel = ViewModelProviders.of(
            this, viewModelFactory)[PickImageViewModel::class.java]

        initViews()

        viewModel.images.observe(viewLifecycleOwner, Observer {
            (binding.images.adapter as ImagesAdapter).submitList(it)
        })

        parentViewModel = ViewModelProviders.of(
            parentFragment!!, viewModelFactory)[SelectImageViewModel::class.java]

        viewModel.event.observe(viewLifecycleOwner, Observer {
            when(it) {
                is PickImageEvent.ShowError -> {
                    parentViewModel.onEvent(SelectImageEvent.ShowError(it.message))
                }
            }
        })

        viewModel.state.observe(viewLifecycleOwner, Observer {
            when(it) {
                PickImageState.Idle -> {
                    binding.permissionsRejectedMessage.visibility = View.GONE
                    binding.refresh.visibility = View.GONE
                }
                PickImageState.Loading -> {
                    binding.permissionsRejectedMessage.visibility = View.GONE
                    binding.refresh.visibility = View.VISIBLE
                    binding.refresh.isRefreshing = false
                }
                PickImageState.Refreshing -> {
                    binding.permissionsRejectedMessage.visibility = View.GONE
                    binding.refresh.visibility = View.VISIBLE
                    binding.refresh.isRefreshing = true
                }
                PickImageState.Loaded -> {
                    binding.permissionsRejectedMessage.visibility = View.GONE
                    binding.refresh.visibility = View.VISIBLE
                    binding.refresh.isRefreshing = false
                }
                PickImageState.PermissionsRejected -> {
                    binding.permissionsRejectedMessage.visibility = View.VISIBLE
                    binding.refresh.visibility = View.GONE
                }
            }
        })

        parentViewModel.storagePermissions.observe(viewLifecycleOwner, Observer {
            it?.let {
                viewModel.onPermissionsResult(it)
                parentViewModel.storagePermissionsHandled()
            }
        })
    }

    private fun initViews() {
        binding.images.adapter = imagesAdapterFactory.create {
            parentViewModel.imageSelected(it.id)
        }
        binding.refresh.setOnRefreshListener {
            viewModel.refreshFileImages()
        }
        binding.images.setHasFixedSize(false)
        binding.images.itemAnimator = null
    }
}