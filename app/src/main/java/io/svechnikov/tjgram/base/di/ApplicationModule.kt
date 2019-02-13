package io.svechnikov.tjgram.base.di

import android.content.Context
import android.content.Context.DISPLAY_SERVICE
import android.hardware.display.DisplayManager
import android.os.Build
import android.util.DisplayMetrics
import android.view.Display
import androidx.room.Room
import com.google.android.exoplayer2.DefaultRenderersFactory
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.ExoPlayerFactory
import com.google.android.exoplayer2.source.ExtractorMediaSource
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.squareup.picasso.Picasso
import dagger.Binds
import dagger.Module
import dagger.Provides
import io.svechnikov.tjgram.BuildConfig
import io.svechnikov.tjgram.R
import io.svechnikov.tjgram.base.BaseApplication
import io.svechnikov.tjgram.base.data.LikePostResult
import io.svechnikov.tjgram.base.data.UploadedImage
import io.svechnikov.tjgram.base.data.WebSocketMessage
import io.svechnikov.tjgram.base.db.BaseDatabase
import io.svechnikov.tjgram.base.db.entities.Post
import io.svechnikov.tjgram.base.db.entities.User
import io.svechnikov.tjgram.base.exceptions.ServiceException
import io.svechnikov.tjgram.base.network.DeviceToken
import io.svechnikov.tjgram.base.network.HttpService
import io.svechnikov.tjgram.base.network.deserializers.*
import io.svechnikov.tjgram.base.network.socketmessages.WebSocketMessageDeserializer
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

@Module
abstract class ApplicationModule {

    @Binds
    abstract fun context(application: BaseApplication): Context

    @Module
    companion object {

        @Provides @Named("webSocketUrl") @JvmStatic
        fun webSocketUrl(): String {
            return "wss://tjournal.ru/chan/api"
        }

        @Provides @Named("userAgent") @JvmStatic
        fun userAgent(context: Context): String {
            val appName = context.getString(R.string.app_name)
            val appVersion = BuildConfig.VERSION_NAME
            val deviceName = Build.MODEL
            val os = "Android/${Build.VERSION.RELEASE}"
            val locale = Locale.getDefault().language

            var size = ""
            val displayManager = context.getSystemService(DISPLAY_SERVICE) as DisplayManager
            val displays = displayManager.displays
            for (display in displays) {
                if (display.displayId == Display.DEFAULT_DISPLAY) {
                    val displayMetrics = DisplayMetrics()

                    display.getMetrics(displayMetrics)

                    val width = displayMetrics.widthPixels
                    val height = displayMetrics.heightPixels

                    size = "; ${width}x$height"
                }
            }

            return "$appName-app/$appVersion ($deviceName; $os); $locale$size"
        }

        @Provides @Singleton @JvmStatic
        fun okHttpClient(@Named("userAgent") userAgent: String,
                         deviceToken: DeviceToken): OkHttpClient {
            val loggingInterceptor = HttpLoggingInterceptor()
            loggingInterceptor.level = HttpLoggingInterceptor.Level.BASIC
            return OkHttpClient.Builder()
                .connectTimeout(2, TimeUnit.SECONDS)
                .readTimeout(2, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .addInterceptor(loggingInterceptor)
                .addInterceptor{chain ->
                    val requestBuilder = chain.request()
                        .newBuilder()
                        .header("User-Agent", userAgent)

                    deviceToken.value?.let {
                        requestBuilder.addHeader("X-Device-Token", it)
                    }
                    chain.proceed(requestBuilder.build())
                }
                .build()
        }

        @Provides @Singleton @JvmStatic
        fun gson(): Gson {
            val token = object : TypeToken<List<Post>>() {}

            return GsonBuilder()
                .registerTypeAdapter(token.rawType,
                    TimelineDeserializer()
                )
                .registerTypeAdapter(
                    WebSocketMessage::class.java,
                    WebSocketMessageDeserializer()
                )
                .registerTypeAdapter(
                    User::class.java,
                    UserDeserializer()
                )
                .registerTypeAdapter(ServiceException::class.java,
                    ServiceErrorDeserializer()
                )
                .registerTypeAdapter(
                    LikePostResult::class.java,
                    LikePostResultDeserializer()
                )
                .registerTypeAdapter(
                    UploadedImage::class.java,
                    UploadedImageDeserializer()
                )
                .create()
        }

        @Provides @Singleton @JvmStatic
        fun httpService(client: OkHttpClient, gson: Gson): HttpService {
            val retrofit = Retrofit.Builder()
                .baseUrl("https://api.tjournal.ru/v1.4/")
                .addConverterFactory(GsonConverterFactory.create(gson))
                .client(client)
                .build()

            return retrofit.create<HttpService>(HttpService::class.java)
        }

        @Provides @Singleton @JvmStatic
        fun database(context: Context): BaseDatabase {
            return Room.databaseBuilder(context, BaseDatabase::class.java, "tj.db")
                .fallbackToDestructiveMigration()
                .build()
        }

        @Provides @JvmStatic @Named("site_id")
        fun siteId(): Long {
            //todo заменить на 214362, чтобы работать с постами TJgram
            return 214360
            //return 214362
        }

        @Provides @JvmStatic @Singleton
        fun providePicasso(): Picasso {
            return Picasso.get()
        }

        @Provides @JvmStatic @Singleton
        fun provideExoPlayer(context: Context): ExoPlayer {
            val trackSelectionFactory = AdaptiveTrackSelection.Factory()
            val renderersFactory = DefaultRenderersFactory(context)

            val trackSelector = DefaultTrackSelector(trackSelectionFactory)
            trackSelector.parameters = DefaultTrackSelector.ParametersBuilder()
                .build()

            return ExoPlayerFactory.newSimpleInstance(
                context, renderersFactory, trackSelector)
        }

        @Provides @JvmStatic @Singleton
        fun provideExtractorMediaSourceFactory(
            context: Context,
            @Named("userAgent") userAgent: String
        ): ExtractorMediaSource.Factory {
            val dataSourceFactory = DefaultDataSourceFactory(context, userAgent)
            return ExtractorMediaSource.Factory(dataSourceFactory)
        }

    }
}