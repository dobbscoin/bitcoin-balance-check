package com.jurajkusnier.bitcoinwalletbalance.data.api

import android.content.Context
import android.util.Log
import com.jakewharton.retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import com.jurajkusnier.bitcoinwalletbalance.BuildConfig
import com.squareup.moshi.KotlinJsonAdapterFactory
import com.squareup.moshi.Moshi
import dagger.Module
import dagger.Provides
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import javax.inject.Named
import javax.inject.Singleton


@Module
class NetworkModule {
    private val TAG = NetworkModule::class.simpleName

    @Provides
    @Singleton
    fun provideNetworkInfo(context: Context):NetworkInfo {
        return NetworkInfo(context)
    }

    @Provides
    @Singleton
    @Named("offlineInterceptor")
    fun provideOfflineCheckInterceptor(networkInfo: NetworkInfo):Interceptor{
        return Interceptor { chain ->
            if (networkInfo.isNetworkAvailable()) {
                chain.proceed(chain.request())
            } else {
                throw OfflineException()
            }
        }
    }

    private fun getInterceptorLevel(): HttpLoggingInterceptor.Level {
        return if (BuildConfig.DEBUG)
            HttpLoggingInterceptor.Level.BASIC
        else
            HttpLoggingInterceptor.Level.NONE
    }

    @Provides
    @Singleton
    fun provideHttpLoginInterceptor():HttpLoggingInterceptor {
        val httpLoggingInterceptor = HttpLoggingInterceptor()
        val level = getInterceptorLevel()
        httpLoggingInterceptor.level = level
        return httpLoggingInterceptor
    }

    @Provides
    @Singleton
    @Named("delayInterceptor")
    fun provideDelayInterceptor():Interceptor{
        return Interceptor { chain ->
                try {
                    Thread.sleep(BuildConfig.NETWORK_DELAY)
                } catch (e: Exception) {
                    Log.e(TAG, Log.getStackTraceString(e))
                }
            chain.proceed(chain.request())
        }
    }

    @Provides
    @Singleton
    @Named("BlockchainUrl")
    fun provideBlockchainUrl(): String{
        return "https://blockchain.info/"
    }

    @Provides
    @Singleton
    @Named("CoinmarketcapUrl")
    fun provideCoinmarketcapUrl(): String{
        return "https://api.coinmarketcap.com/"
    }

    @Provides
    @Singleton
    fun provideBlockchainApiService(@Named("Blockchain") retrofit: Retrofit): BlockchainApiService = retrofit.create(BlockchainApiService::class.java)

    @Provides
    @Singleton
    fun provideCoinmarketcapApiService(@Named("Coinmarketcap") retrofit: Retrofit): CoinmarketcapApiService = retrofit.create(CoinmarketcapApiService::class.java)

    @Provides
    @Singleton
    @Named("Blockchain")
    fun provideRetrofitForBlockchainApi(@Named("BlockchainUrl") baseUrl: String, moshi: Moshi, httpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .baseUrl(baseUrl)
                .client(httpClient)
                .build()
    }

    @Provides
    @Singleton
    @Named("Coinmarketcap")
    fun provideRetrofitForCoinmarketcapApi(@Named("CoinmarketcapUrl") baseUrl: String, moshi: Moshi, httpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .baseUrl(baseUrl)
                .client(httpClient)
                .build()
    }

    @Provides
    @Singleton
    fun provideHttpClient(loggingInterceptor: HttpLoggingInterceptor, @Named("delayInterceptor")delayInterceptor: Interceptor, @Named("offlineInterceptor")offlineCheckInterceptor: Interceptor):OkHttpClient {

        val okHttpClient = OkHttpClient.Builder()
                .addInterceptor(loggingInterceptor)
                .addInterceptor(offlineCheckInterceptor)

        if (BuildConfig.DEBUG) {
            okHttpClient.addInterceptor(delayInterceptor)
        }

        return okHttpClient.build()
    }

    @Provides
    @Singleton
    fun provideMoshi() = Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()
}