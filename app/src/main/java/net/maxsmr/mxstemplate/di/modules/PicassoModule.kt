package net.maxsmr.mxstemplate.di.modules

import android.content.Context
import com.squareup.picasso.OkHttp3Downloader
import com.squareup.picasso.Picasso
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import net.maxsmr.core.di.PicassoOkHttpClient
import okhttp3.OkHttpClient
import javax.inject.Singleton

@[Module
InstallIn(SingletonComponent::class)]
class PicassoModule {

    @[Provides Singleton]
    fun providePicasso(
        @ApplicationContext context: Context,
        @PicassoOkHttpClient okHttpClient: OkHttpClient,
    ): Picasso = Picasso.Builder(context)
        .downloader(OkHttp3Downloader(okHttpClient))
        .build()
}