package io.svechnikov.tjgram.base.di

import dagger.Module
import dagger.android.ContributesAndroidInjector
import io.svechnikov.tjgram.features.likesupdater.LikesUpdaterService

@Module
abstract class ServicesModule {
    @ContributesAndroidInjector
    abstract fun LikesUpdaterService(): LikesUpdaterService
}