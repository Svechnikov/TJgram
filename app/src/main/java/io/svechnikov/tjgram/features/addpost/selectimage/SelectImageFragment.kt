package io.svechnikov.tjgram.features.addpost.selectimage

import android.Manifest
import android.content.Intent
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
import io.svechnikov.tjgram.base.MainEvent
import io.svechnikov.tjgram.base.MainViewModel
import io.svechnikov.tjgram.base.di.Injectable
import io.svechnikov.tjgram.databinding.FragmentSelectImageBinding
import pub.devrel.easypermissions.AfterPermissionGranted
import pub.devrel.easypermissions.AppSettingsDialog
import pub.devrel.easypermissions.EasyPermissions
import javax.inject.Inject

class SelectImageFragment : Fragment(), Injectable,
    EasyPermissions.PermissionCallbacks,
    EasyPermissions.RationaleCallbacks {

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    @Inject
    lateinit var imagesAdapterFactory: ImagesAdapter.Factory

    private lateinit var viewModel: SelectImageViewModel

    private lateinit var binding: FragmentSelectImageBinding

    companion object {
        const val REQUEST_EXTERNAL_STORAGE = 1
    }

    fun navController() = findNavController()

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        binding = FragmentSelectImageBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        viewModel = ViewModelProviders.of(
            this, viewModelFactory)[SelectImageViewModel::class.java]

        initViews()

        viewModel.images.observe(viewLifecycleOwner, Observer {
            (binding.images.adapter as ImagesAdapter).submitList(it)
        })

        val mainViewModel = ViewModelProviders.of(
            activity!!, viewModelFactory)[MainViewModel::class.java]

        mainViewModel.setBottomBarVisibility(false)

        viewModel.event.observe(viewLifecycleOwner, Observer {
            when(it) {
                is SelectImageEvent.ShowError -> {
                    mainViewModel.setEvent(MainEvent.ShowMessage(it.message))
                }
                is SelectImageEvent.OpenSendImage -> {
                    val action = SelectImageFragmentDirections
                        .actionSelectImageFragmentToSendImageFragment(it.imageId)
                    navController().navigate(action)
                }
                SelectImageEvent.GoBack -> {
                    navController().popBackStack()
                }
                SelectImageEvent.NavigateToAuth -> {
                    navController().navigate(
                        R.id.action_selectImageFragment_to_selectAuthMethodFragment)
                }
                SelectImageEvent.RequestPermissions -> {
                    requestPermissions()
                }
            }
        })

        viewModel.state.observe(viewLifecycleOwner, Observer {
            when(it) {
                SelectImageState.Idle -> {
                    binding.root.visibility = View.GONE
                }
                SelectImageState.Loading -> {
                    binding.root.visibility = View.VISIBLE
                    binding.refresh.isRefreshing = false
                }
                SelectImageState.Refreshing -> {
                    binding.root.visibility = View.VISIBLE
                    binding.refresh.isRefreshing = true
                }
                SelectImageState.Loaded -> {
                    binding.root.visibility = View.VISIBLE
                    binding.refresh.isRefreshing = false
                }
            }
        })

        viewModel.start()
    }

    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>,
                                            grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        EasyPermissions.onRequestPermissionsResult(requestCode,
            permissions, grantResults, this)
    }

    @AfterPermissionGranted(SelectImageFragment.REQUEST_EXTERNAL_STORAGE)
    private fun requestPermissions() {
        val permission = Manifest.permission.READ_EXTERNAL_STORAGE

        if (EasyPermissions.hasPermissions(context!!, permission)) {
            viewModel.onGotPermissions()
        }
        else {
            val rationale = getString(R.string.select_image_permission_rationale)

            EasyPermissions.requestPermissions(this,
                rationale,
                SelectImageFragment.REQUEST_EXTERNAL_STORAGE, permission)
        }
    }

    override fun onPermissionsDenied(requestCode: Int, perms: MutableList<String>) {
        if (EasyPermissions.somePermissionPermanentlyDenied(this, perms)) {
            AppSettingsDialog.Builder(this)
                .setRationale(R.string.select_image_permission_settings_rationale)
                .setPositiveButton(R.string.generic_ok)
                .setNegativeButton(R.string.generic_cancel)
                .build()
                .show()
        }
        else {
            requestPermissions()
        }
    }

    override fun onPermissionsGranted(requestCode: Int, perms: MutableList<String>) {

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == AppSettingsDialog.DEFAULT_SETTINGS_REQ_CODE) {
            if (!hasPermissions()) {
                navController().popBackStack()
            }
            else {
                viewModel.onGotPermissions()
            }
        }
    }

    override fun onRationaleDenied(requestCode: Int) {
        navController().popBackStack()
    }

    override fun onRationaleAccepted(requestCode: Int) {

    }

    private fun hasPermissions(): Boolean {
        return EasyPermissions.hasPermissions(context!!, Manifest.permission.READ_EXTERNAL_STORAGE)
    }

    private fun initViews() {
        binding.images.adapter = imagesAdapterFactory.create {
            viewModel.imageSelected(it)
        }
        binding.refresh.setOnRefreshListener {
            viewModel.refreshFileImages()
        }
        binding.images.setHasFixedSize(false)
        binding.images.itemAnimator = null
    }
}