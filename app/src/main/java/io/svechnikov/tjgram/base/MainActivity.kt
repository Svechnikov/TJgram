package io.svechnikov.tjgram.base

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.findNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.snackbar.Snackbar
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.support.HasSupportFragmentInjector
import io.svechnikov.tjgram.R
import io.svechnikov.tjgram.base.di.Injectable
import io.svechnikov.tjgram.base.di.ViewModelFactory
import io.svechnikov.tjgram.databinding.ActivityMainBinding
import javax.inject.Inject

class MainActivity : AppCompatActivity(),
    HasSupportFragmentInjector, Injectable {

    @Inject
    lateinit var fragmentInjector: DispatchingAndroidInjector<Fragment>

    @Inject
    lateinit var viewModelFactory: ViewModelFactory

    private lateinit var viewModel: MainViewModel

    private lateinit var binding: ActivityMainBinding

    private var snackbar: Snackbar? = null

    fun navController() = findNavController(R.id.fragment)

    override fun supportFragmentInjector(): AndroidInjector<Fragment> {
        return fragmentInjector
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)

        viewModel = ViewModelProviders.of(this,
            viewModelFactory)[MainViewModel::class.java]

        viewModel.event.observe(this, Observer {
            it?.let {
                viewModel.eventHandled()

                when(it) {
                    is MainEvent.ShowMessage -> {
                        showError(it.message)
                    }
                }
            }
        })

        viewModel.bottomBarVisibility.observe(this, Observer {
            binding.navigation.visibility = when(it) {
                true -> View.VISIBLE
                false -> View.GONE
            }
        })

        binding.toolbar.setupWithNavController(navController(), null)
        binding.navigation.setupWithNavController(navController())
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
}