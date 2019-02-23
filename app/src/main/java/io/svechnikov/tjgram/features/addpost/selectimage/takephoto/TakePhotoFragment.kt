package io.svechnikov.tjgram.features.addpost.selectimage.takephoto

import android.content.pm.ActivityInfo
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import io.svechnikov.tjgram.R
import io.svechnikov.tjgram.base.di.Injectable
import io.svechnikov.tjgram.databinding.FragmentTakePhotoBinding
import io.svechnikov.tjgram.features.addpost.selectimage.SelectImageEvent
import io.svechnikov.tjgram.features.addpost.selectimage.SelectImageState
import io.svechnikov.tjgram.features.addpost.selectimage.SelectImageViewModel
import io.svechnikov.tjgram.features.addpost.selectimage.takephoto.cameras.BaseCamera
import io.svechnikov.tjgram.features.addpost.selectimage.takephoto.cameras.BaseCamera1
import io.svechnikov.tjgram.features.addpost.selectimage.takephoto.cameras.BaseCamera2
import timber.log.Timber
import javax.inject.Inject

// todo адекватно реагировать на смену ориентации экрана
@Suppress("deprecation")
class TakePhotoFragment : Fragment(), Injectable {

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private lateinit var viewModel: TakePhotoViewModel

    private lateinit var binding: FragmentTakePhotoBinding

    private lateinit var parentViewModel: SelectImageViewModel

    private var camera: BaseCamera? = null

    companion object {
        fun newInstance(): TakePhotoFragment {
            return TakePhotoFragment()
        }
    }

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        binding = FragmentTakePhotoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        viewModel = ViewModelProviders.of(
            this, viewModelFactory)[TakePhotoViewModel::class.java]

        parentViewModel = ViewModelProviders.of(
            parentFragment!!, viewModelFactory)[SelectImageViewModel::class.java]

        initViews()

        viewModel.state.observe(viewLifecycleOwner, Observer {
            when(it) {
                TakePhotoState.PermissionsRejected -> {
                    binding.message.visibility = View.VISIBLE
                    binding.message.text = getString(R.string.take_photo_permissions_rejected)
                    getPreview().visibility = View.GONE
                    binding.takePhoto.visibility = View.GONE
                }
                TakePhotoState.ShowViewfinder -> {
                    binding.message.visibility = View.GONE
                    binding.takePhoto.visibility = View.VISIBLE
                    getPreview().visibility = View.VISIBLE
                    binding.takePhoto.isEnabled = true
                    openCamera()
                }
                TakePhotoState.HideViewfinder -> {
                    binding.message.visibility = View.GONE
                    binding.surfaceView.visibility = View.GONE
                    getPreview().visibility = View.GONE
                    binding.takePhoto.visibility = View.GONE
                    closeCamera()
                }
                TakePhotoState.ShowCameraError -> {
                    binding.message.visibility = View.VISIBLE
                    binding.message.text = getString(R.string.take_photo_camera_error)
                    getPreview().visibility = View.GONE
                    binding.takePhoto.visibility = View.GONE
                }
                TakePhotoState.ProcessingPhoto -> {
                    binding.message.visibility = View.GONE
                    binding.takePhoto.visibility = View.VISIBLE
                    getPreview().visibility = View.VISIBLE
                    binding.takePhoto.isEnabled = false
                }
            }
        })
        viewModel.event.observe(viewLifecycleOwner, Observer {
            when(it) {
                is TakePhotoEvent.NavigateToSendImage -> {
                    parentViewModel.imageSelected(it.imageId)
                }
                is TakePhotoEvent.Error -> {
                    parentViewModel.onEvent(SelectImageEvent.ShowError(it.message))
                    closeCamera()
                    if (viewModel.state.value == TakePhotoState.ShowViewfinder) {
                        openCamera()
                    }
                }
            }
        })

        parentViewModel.cameraPermissions.observe(viewLifecycleOwner, Observer {
            it?.let {
                viewModel.onPermissionsResult(it)
                parentViewModel.cameraPermissionsHandled()
            }
        })

        parentViewModel.state.observe(viewLifecycleOwner, Observer {
            when(it) {
                SelectImageState.ShowTakePhotoScreen -> {
                    viewModel.onActive()
                    activity!!.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                }
                else -> {
                    viewModel.onInactive()
                    activity!!.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR
                }
            }
        })
    }

    override fun onPause() {
        super.onPause()

        if (viewModel.state.value == TakePhotoState.ShowViewfinder) {
            closeCamera()
            activity!!.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR
        }
    }

    override fun onResume() {
        super.onResume()

        if (viewModel.state.value == TakePhotoState.ShowViewfinder) {
            openCamera()
            activity!!.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
    }

    private fun getPreview(): View {
        return if (Build.VERSION.SDK_INT >= 21) {
            binding.textureView
        }
        else {
            binding.surfaceView
        }
    }

    private fun openCamera() {
        if (camera == null) {
            Timber.i("open")
            if (Build.VERSION.SDK_INT >= 21) {
                camera = BaseCamera2(context!!)
            }
            else {
                camera = BaseCamera1()
            }

            // todo сделать так, чтобы при landscape положении фотки имели корректную ориентацию
            // даже в залоченном режиме. Для этого надо использовать SensorManager
            camera?.apply {
                setOnErrorCallback {
                    Timber.e(it)
                    viewModel.onCameraError()
                }
                setOnPhotoTakenCallback {
                    viewModel.saveImage(it)
                }
                setMaxHeight(viewModel.maxPhotoHeight)
                setMaxWidth(viewModel.maxPhotoWidth)
                setPreview(getPreview())

                setDisplay(activity!!.windowManager.defaultDisplay)

                open()
            }

            binding.takePhoto.isEnabled = true
        }
    }

    private fun closeCamera() {
        camera?.close()
        camera = null
    }

    private fun initViews() {
        binding.takePhoto.setOnClickListener {
            Timber.i("take photo")
            viewModel.onTakePhoto()
            camera?.takePhoto()
        }
    }
}