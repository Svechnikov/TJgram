package io.svechnikov.tjgram.features.addpost.selectimage

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import io.svechnikov.tjgram.R
import io.svechnikov.tjgram.features.main.MainViewModel
import io.svechnikov.tjgram.base.di.Injectable
import io.svechnikov.tjgram.databinding.FragmentSelectImageBinding
import io.svechnikov.tjgram.features.addpost.selectimage.pickimage.PickImageFragment
import io.svechnikov.tjgram.features.addpost.selectimage.takephoto.TakePhotoFragment
import pub.devrel.easypermissions.AfterPermissionGranted
import pub.devrel.easypermissions.AppSettingsDialog
import pub.devrel.easypermissions.EasyPermissions
import timber.log.Timber
import javax.inject.Inject

class SelectImageFragment : Fragment(), Injectable,
    EasyPermissions.PermissionCallbacks,
    EasyPermissions.RationaleCallbacks {

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private lateinit var viewModel: SelectImageViewModel
    private lateinit var binding: FragmentSelectImageBinding

    private var adapter: SelectImagePagerAdapter? = null
    private var snackbar: Snackbar? = null

    fun navController() = findNavController()

    companion object {
        private const val REQUEST_GALLERY_PERMISSIONS = 1
        private const val REQUEST_CAMERA_PERMISSIONS = 2

        private const val POSITION_GALLERY = 0
        private const val POSITION_TAKE_PHOTO = 1

        private val GALLERY_PERMISSIONS = arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE)

        private val CAMERA_PERMISSIONS = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE)
    }

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        binding = FragmentSelectImageBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        val mainViewModel = ViewModelProviders.of(
            activity!!, viewModelFactory)[MainViewModel::class.java]

        mainViewModel.setBottomBarVisibility(false)
        mainViewModel.setToolbarScrollable(false)

        viewModel = ViewModelProviders.of(
            this, viewModelFactory)[SelectImageViewModel::class.java]

        viewModel.start()

        initViews()

        viewModel.event.observe(viewLifecycleOwner, Observer {
            when(it) {
                is SelectImageEvent.ShowError -> {
                    showError(it.error)
                }
                is SelectImageEvent.OpenSendImage -> {
                    val action =
                        SelectImageFragmentDirections.actionSelectImageFragmentToSendImageFragment(
                            it.imageId
                        )
                    navController().navigate(action)
                }
                SelectImageEvent.GoBack -> {
                    navController().popBackStack()
                }
                SelectImageEvent.NavigateToAuth -> {
                    navController().navigate(
                        R.id.action_selectImageFragment_to_selectAuthMethodFragment)
                }
                SelectImageEvent.RequestCameraPermissions -> {
                    requestCameraPermissions()
                }
                SelectImageEvent.RequestGalleryPermissions -> {
                    requestGalleryPermissions()
                }
            }
        })
        viewModel.state.observe(viewLifecycleOwner, Observer {
            when(it) {
                SelectImageState.ShowPickImageScreen -> {
                    onPageSelected(POSITION_GALLERY)
                }
                SelectImageState.ShowTakePhotoScreen -> {
                    onPageSelected(POSITION_TAKE_PHOTO)
                }
            }
        })
    }

    private fun onPageSelected(page: Int) {
        Timber.i("onPageSelected $page")
        binding.pager.currentItem = page

        when (page) {
            POSITION_GALLERY -> {
                viewModel.onPickImageSelected()
            }
            POSITION_TAKE_PHOTO -> {
                viewModel.onCameraSelected()
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>,
                                            grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        EasyPermissions.onRequestPermissionsResult(requestCode,
            permissions, grantResults, this)
    }

    @AfterPermissionGranted(REQUEST_GALLERY_PERMISSIONS)
    private fun requestGalleryPermissions() {
        if (EasyPermissions.hasPermissions(context!!, *GALLERY_PERMISSIONS)) {
            viewModel.onStoragePermissionsResult(true)
        }
        else {
            val rationale = getString(R.string.pick_image_permission_rationale)

            EasyPermissions.requestPermissions(this,
                rationale,
                REQUEST_GALLERY_PERMISSIONS, *GALLERY_PERMISSIONS)
        }
    }

    @AfterPermissionGranted(REQUEST_CAMERA_PERMISSIONS)
    private fun requestCameraPermissions() {
        if (EasyPermissions.hasPermissions(context!!, *CAMERA_PERMISSIONS)) {
            viewModel.onCameraPermissionsResult(true)
        }
        else {
            val rationale = getString(R.string.take_photo_permission_rationale)

            EasyPermissions.requestPermissions(this,
                rationale,
                REQUEST_CAMERA_PERMISSIONS, *CAMERA_PERMISSIONS)
        }
    }

    override fun onPermissionsDenied(requestCode: Int, perms: MutableList<String>) {
        when(requestCode) {
            REQUEST_GALLERY_PERMISSIONS -> {
                if (EasyPermissions.somePermissionPermanentlyDenied(this, perms)) {
                    AppSettingsDialog.Builder(this)
                        .setTitle(R.string.generic_rationale_dialog_title)
                        .setRationale(R.string.pick_image_permission_settings_rationale)
                        .setPositiveButton(R.string.generic_ok)
                        .setNegativeButton(R.string.generic_cancel)
                        .build()
                        .show()
                }
                else {
                    viewModel.onStoragePermissionsResult(false)
                }
            }
            REQUEST_CAMERA_PERMISSIONS -> {
                if (EasyPermissions.somePermissionPermanentlyDenied(this, perms)) {
                    AppSettingsDialog.Builder(this)
                        .setTitle(R.string.generic_rationale_dialog_title)
                        .setRationale(R.string.take_photo_permission_settings_rationale)
                        .setPositiveButton(R.string.generic_ok)
                        .setNegativeButton(R.string.generic_cancel)
                        .build()
                        .show()
                }
                else {
                    viewModel.onCameraPermissionsResult(false)
                }
            }
        }
    }

    override fun onPermissionsGranted(requestCode: Int, perms: MutableList<String>) {

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == AppSettingsDialog.DEFAULT_SETTINGS_REQ_CODE) {
            when (binding.pager.currentItem) {
                POSITION_TAKE_PHOTO -> {
                    viewModel.onCameraPermissionsResult(
                        EasyPermissions.hasPermissions(context!!, *GALLERY_PERMISSIONS))
                }
                POSITION_GALLERY -> {
                    viewModel.onStoragePermissionsResult(
                        EasyPermissions.hasPermissions(context!!, *CAMERA_PERMISSIONS))
                }
            }
        }
    }

    override fun onRationaleDenied(requestCode: Int) {
        when(requestCode) {
            REQUEST_GALLERY_PERMISSIONS -> {
                viewModel.onStoragePermissionsResult(false)
            }
            REQUEST_CAMERA_PERMISSIONS -> {
                viewModel.onCameraPermissionsResult(false)
            }
        }
    }

    override fun onRationaleAccepted(requestCode: Int) {

    }

    override fun onPause() {
        super.onPause()

        snackbar?.dismiss()
    }

    private fun showError(errorMessage: String) {
        snackbar?.dismiss()

        val snackbar = Snackbar.make(binding.coordinator,
            errorMessage, Snackbar.LENGTH_LONG)

        snackbar.show()

        this.snackbar = snackbar
    }

    private fun initViews() {
        adapter = SelectImagePagerAdapter(childFragmentManager)
        binding.pager.adapter = adapter

        val position = when(viewModel.state.value) {
            SelectImageState.ShowTakePhotoScreen -> {
                POSITION_TAKE_PHOTO
            }
            SelectImageState.ShowPickImageScreen -> {
                POSITION_GALLERY
            }
            else -> 0
        }

        binding.pager.post {
            onPageSelected(position)
        }

        Timber.i("addOnTabSelectedListener")
        binding.tabs.addOnTabSelectedListener(object: TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                Timber.i("onTabSelected")
                onPageSelected(tab.position)
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {

            }

            override fun onTabReselected(tab: TabLayout.Tab) {

            }
        })
        binding.pager.addOnPageChangeListener(
            TabLayout.TabLayoutOnPageChangeListener(binding.tabs))
    }

    private inner class SelectImagePagerAdapter(fm: FragmentManager) : FragmentStatePagerAdapter(fm) {
        override fun getItem(position: Int): Fragment {
            return when(position) {
                POSITION_GALLERY -> {
                    PickImageFragment.newInstance()
                }
                POSITION_TAKE_PHOTO -> {
                    TakePhotoFragment.newInstance()
                }
                else -> {
                    throw IllegalArgumentException()
                }
            }
        }

        override fun getCount(): Int {
            if (context?.packageManager?.hasSystemFeature(PackageManager.FEATURE_CAMERA) == true) {
                return 2
            }
            return 1
        }
    }
}