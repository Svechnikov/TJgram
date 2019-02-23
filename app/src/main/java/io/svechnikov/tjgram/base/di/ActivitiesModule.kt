package io.svechnikov.tjgram.base.di

import dagger.Module
import dagger.android.ContributesAndroidInjector
import io.svechnikov.tjgram.features.main.MainActivity
import io.svechnikov.tjgram.features.addpost.selectimage.SelectImageFragment
import io.svechnikov.tjgram.features.addpost.selectimage.pickimage.PickImageFragment
import io.svechnikov.tjgram.features.addpost.selectimage.takephoto.TakePhotoFragment
import io.svechnikov.tjgram.features.addpost.sendimage.SendImageFragment
import io.svechnikov.tjgram.features.auth.qr.QrAuthFragment
import io.svechnikov.tjgram.features.auth.selectmethod.SelectAuthMethodFragment
import io.svechnikov.tjgram.features.timeline.parent.TimelineParentFragment
import io.svechnikov.tjgram.features.timeline.child.TimelineFragment

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
    abstract fun timelineParentFragment(): TimelineParentFragment

    @ContributesAndroidInjector @FragmentScope
    abstract fun timelineFragment(): TimelineFragment

    @ContributesAndroidInjector @FragmentScope
    abstract fun selectImageFragment(): SelectImageFragment

    @ContributesAndroidInjector @FragmentScope
    abstract fun pickImageFragment(): PickImageFragment

    @ContributesAndroidInjector @FragmentScope
    abstract fun takePhotoFragment(): TakePhotoFragment

    @ContributesAndroidInjector @FragmentScope
    abstract fun sendImageFragment(): SendImageFragment
}