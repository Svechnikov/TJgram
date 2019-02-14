package io.svechnikov.tjgram.base

import android.app.Activity
import android.app.Application
import android.os.Bundle
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.multidex.MultiDex
import dagger.android.AndroidInjection
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasActivityInjector
import dagger.android.support.AndroidSupportInjection
import io.svechnikov.tjgram.BuildConfig
import io.svechnikov.tjgram.base.di.ApplicationComponent
import io.svechnikov.tjgram.base.di.DaggerApplicationComponent
import io.svechnikov.tjgram.base.di.Injectable
import io.svechnikov.tjgram.base.di.databinding.DaggerDefaultDataBindingComponent
import timber.log.Timber
import javax.inject.Inject


class BaseApplication : Application(), HasActivityInjector {

    @Inject
    lateinit var injector: DispatchingAndroidInjector<Activity>

    lateinit var component: ApplicationComponent

    override fun activityInjector(): AndroidInjector<Activity> {
        return injector
    }

    override fun onCreate() {
        super.onCreate()

        MultiDex.install(this)

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        component = DaggerApplicationComponent.builder()
            .setApplication(this).build()

        DataBindingUtil.setDefaultComponent(
            DaggerDefaultDataBindingComponent.builder()
                .setComponent(component).build())

        component.inject(this)

        registerActivityLifecycleCallbacks(object: ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity?,
                                           savedInstanceState: Bundle?) {
                if (activity is Injectable) {
                    AndroidInjection.inject(activity)
                }
                if (activity is FragmentActivity) {
                    activity.supportFragmentManager.registerFragmentLifecycleCallbacks(
                        object: FragmentManager.FragmentLifecycleCallbacks() {
                            override fun onFragmentPreCreated(fm: FragmentManager,
                                                              f: Fragment,
                                                              savedInstanceState: Bundle?) {
                                super.onFragmentPreCreated(fm, f, savedInstanceState)

                                if (f is Injectable) {
                                    AndroidSupportInjection.inject(f)
                                }
                            }
                        }, true)
                }
            }

            override fun onActivityPaused(activity: Activity?) {

            }

            override fun onActivityResumed(activity: Activity?) {

            }

            override fun onActivityStarted(activity: Activity?) {

            }

            override fun onActivityDestroyed(activity: Activity?) {

            }

            override fun onActivitySaveInstanceState(activity: Activity?, outState: Bundle?) {

            }

            override fun onActivityStopped(activity: Activity?) {

            }
        })
    }
}