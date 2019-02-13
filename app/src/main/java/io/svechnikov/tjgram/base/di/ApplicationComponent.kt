package io.svechnikov.tjgram.base.di

import com.squareup.picasso.Picasso
import dagger.BindsInstance
import dagger.Component
import dagger.android.support.AndroidSupportInjectionModule
import io.svechnikov.tjgram.base.BaseApplication
import javax.inject.Singleton

@Singleton
@Component(modules = [
    ApplicationModule::class,
    AndroidSupportInjectionModule::class,
    ActivitiesModule::class,
    ViewModelModule::class,
    ServicesModule::class
])
interface ApplicationComponent {

    @Component.Builder
    interface Builder {
        @BindsInstance
        fun setApplication(application: BaseApplication): Builder
        fun build(): ApplicationComponent
    }

    fun inject(application: BaseApplication)

    fun picasso(): Picasso
}