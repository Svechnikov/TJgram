package io.svechnikov.tjgram.base.di.databinding

import androidx.databinding.DataBindingComponent
import dagger.Component
import io.svechnikov.tjgram.base.di.ApplicationComponent

@DataBindingScope
@Component(
    dependencies = [ApplicationComponent::class]
)
interface DefaultDataBindingComponent : DataBindingComponent {

    @Component.Builder
    interface Builder {
        fun setComponent(appComponent: ApplicationComponent): Builder
        fun build(): DefaultDataBindingComponent
    }

    override fun getDataBindingAdapter(): DataBindingAdapter
}
