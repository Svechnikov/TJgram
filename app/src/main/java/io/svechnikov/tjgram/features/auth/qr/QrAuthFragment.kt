package io.svechnikov.tjgram.features.auth.qr

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
import com.budiyev.android.codescanner.*
import io.svechnikov.tjgram.R
import io.svechnikov.tjgram.features.main.MainEvent
import io.svechnikov.tjgram.features.main.MainViewModel
import io.svechnikov.tjgram.base.di.Injectable
import io.svechnikov.tjgram.databinding.FragmentAuthQrBinding
import pub.devrel.easypermissions.AfterPermissionGranted
import pub.devrel.easypermissions.AppSettingsDialog
import pub.devrel.easypermissions.EasyPermissions
import javax.inject.Inject


class QrAuthFragment : Fragment(), Injectable,
    EasyPermissions.PermissionCallbacks,
    EasyPermissions.RationaleCallbacks {

    private lateinit var viewModel: QrAuthViewModel

    private lateinit var binding: FragmentAuthQrBinding

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory
    
    private lateinit var scanner: CodeScanner

    companion object {
        const val REQUEST_CAMERA = 1
    }

    fun navController() = findNavController()

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        binding = FragmentAuthQrBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        viewModel = ViewModelProviders.of(
            this, viewModelFactory)[QrAuthViewModel::class.java]

        val mainViewModel = ViewModelProviders.of(
            activity!!, viewModelFactory)[MainViewModel::class.java]

        mainViewModel.setBottomBarVisibility(false)
        mainViewModel.setToolbarScrollable(false)

        initScannerView()

        viewModel.event.observe(viewLifecycleOwner, Observer {
            it?.let {
                viewModel.eventHandled()
                when(it) {
                    is QrAuthEvent.Success -> {
                        navController().navigate(
                            QrAuthFragmentDirections.actionQrAuthFragmentToTimelineParentFragment())
                    }
                    is QrAuthEvent.CloseWithError -> {
                        mainViewModel.setEvent(MainEvent.ShowMessage(it.message))

                        navController().popBackStack()
                    }
                    QrAuthEvent.RequestPermissions -> {
                        requestCameraPermissions()
                    }
                }
            }
        })

        viewModel.state.observe(viewLifecycleOwner, Observer {state ->
            when(state) {
                QrAuthState.ShowScanner -> {
                    binding.root.visibility = View.VISIBLE
                    scanner.startPreview()
                }
                QrAuthState.Empty -> {
                    binding.root.visibility = View.GONE
                }
                else -> {}
            }
        })
    }

    private fun initScannerView() {
        val scannerView = binding.scanner

        scanner = CodeScanner(activity?.applicationContext!!, scannerView)

        scanner.camera = CodeScanner.CAMERA_BACK
        scanner.formats = CodeScanner.ALL_FORMATS
        scanner.autoFocusMode = AutoFocusMode.SAFE
        scanner.scanMode = ScanMode.SINGLE
        scanner.isAutoFocusEnabled = true
        scanner.isFlashEnabled = false

        scanner.decodeCallback = DecodeCallback {result ->
            activity?.runOnUiThread {
                viewModel.onQrResult(result.text)
                scanner.releaseResources()
            }
        }
        scanner.errorCallback = ErrorCallback.SUPPRESS
    }

    override fun onPause() {
        scanner.releaseResources()

        super.onPause()
    }

    override fun onResume() {
        super.onResume()

        if (viewModel.state.value == QrAuthState.ShowScanner) {
            scanner.startPreview()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>,
                                            grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        EasyPermissions.onRequestPermissionsResult(requestCode,
            permissions, grantResults, this)
    }

    @AfterPermissionGranted(REQUEST_CAMERA)
    private fun requestCameraPermissions() {
        val permission = Manifest.permission.CAMERA

        if (EasyPermissions.hasPermissions(context!!, permission)) {
            viewModel.onGotPermissions()
        }
        else {
            val rationale = getString(R.string.qr_auth_camera_permission_rationale)

            EasyPermissions.requestPermissions(this,
                rationale, REQUEST_CAMERA, permission)
        }
    }

    override fun onPermissionsDenied(requestCode: Int, perms: MutableList<String>) {
        if (EasyPermissions.somePermissionPermanentlyDenied(this, perms)) {
            AppSettingsDialog.Builder(this)
                .setTitle(R.string.generic_rationale_dialog_title)
                .setRationale(R.string.qr_auth_camera_permission_settings_rationale)
                .setPositiveButton(R.string.generic_ok)
                .setNegativeButton(R.string.generic_cancel)
                .build()
                .show()
        }
        else {
            requestCameraPermissions()
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
        return EasyPermissions.hasPermissions(context!!, Manifest.permission.CAMERA)
    }
}