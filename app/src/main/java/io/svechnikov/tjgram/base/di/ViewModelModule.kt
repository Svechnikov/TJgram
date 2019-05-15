package io.svechnikov.tjgram.base.di

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Binds
import dagger.MapKey
import dagger.Module
import dagger.multibindings.IntoMap
import io.svechnikov.tjgram.features.main.MainViewModel
import io.svechnikov.tjgram.features.addpost.selectimage.SelectImageViewModel
import io.svechnikov.tjgram.features.addpost.selectimage.pickimage.PickImageViewModel
import io.svechnikov.tjgram.features.addpost.selectimage.takephoto.TakePhotoViewModel
import io.svechnikov.tjgram.features.addpost.sendimage.SendImageViewModel
import io.svechnikov.tjgram.features.auth.qr.QrAuthViewModel
import io.svechnikov.tjgram.features.auth.selectmethod.SelectAuthMethodViewModel
import io.svechnikov.tjgram.features.timeline.parent.TimelineParentViewModel
import io.svechnikov.tjgram.features.timeline.child.TimelineViewModel
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton
import kotlin.reflect.KClass

@Singleton
class ViewModelFactory
@Inject constructor(
    private val viewModels: MutableMap<Class<out ViewModel>,
            Provider<ViewModel>>): ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T = viewModels[modelClass]?.get() as T
}

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
@Retention(AnnotationRetention.RUNTIME)
@MapKey
internal annotation class ViewModelKey(val value: KClass<out ViewModel>)

@Suppress("unused")
@Module
abstract class ViewModelModule {

    @Binds
    internal abstract fun bindViewModelFactory(factory: ViewModelFactory): ViewModelProvider.Factory

    @Binds
    @IntoMap
    @ViewModelKey(SelectAuthMethodViewModel::class)
    internal abstract fun selectAuthMethodViewModel(viewModel: SelectAuthMethodViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(QrAuthViewModel::class)
    internal abstract fun qrAuthViewModel(viewModel: QrAuthViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(TimelineParentViewModel::class)
    internal abstract fun timelineParentViewModel(viewModel: TimelineParentViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(TimelineViewModel::class)
    internal abstract fun timelineViewModel(viewModel: TimelineViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(MainViewModel::class)
    internal abstract fun mainViewModel(viewModel: MainViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(SelectImageViewModel::class)
    internal abstract fun selectImageViewModel(viewModel: SelectImageViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(PickImageViewModel::class)
    internal abstract fun pickImageViewModel(viewModel: PickImageViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(TakePhotoViewModel::class)
    internal abstract fun takePhotoViewModel(viewModel: TakePhotoViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(SendImageViewModel::class)
    internal abstract fun sendImageViewModel(viewModel: SendImageViewModel): ViewModel
}