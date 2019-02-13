package io.svechnikov.tjgram.base.di

import dagger.Module
import dagger.android.ContributesAndroidInjector
import io.svechnikov.tjgram.base.MainActivity
import io.svechnikov.tjgram.features.addpost.selectimage.SelectImageFragment
import io.svechnikov.tjgram.features.addpost.sendimage.SendImageFragment
import io.svechnikov.tjgram.features.auth.qr.QrAuthFragment
import io.svechnikov.tjgram.features.auth.selectmethod.SelectAuthMethodFragment
import io.svechnikov.tjgram.features.timeline.TimelineFragment

@Module(subcomponents = [])
abstract class ActivitiesModule {
    @ContributesAndroidInjector(modules = [MainActivityFragmentsModule::class])
    abstract fun contributesMainActivity(): MainActivity
}

@Module(subcomponents = [])
abstract class MainActivityFragmentsModule {

    @ContributesAndroidInjector @FragmentScope
    abstract fun selectAuthMethodFragment(): SelectAuthMethodFragment

    @ContributesAndroidInjector @FragmentScope
    abstract fun qrAuthFragment(): QrAuthFragment

    @ContributesAndroidInjector @FragmentScope
    abstract fun timelineFragment(): TimelineFragment

    @ContributesAndroidInjector @FragmentScope
    abstract fun selectImageFragment(): SelectImageFragment

    @ContributesAndroidInjector @FragmentScope
    abstract fun sendImageFragment(): SendImageFragment
}