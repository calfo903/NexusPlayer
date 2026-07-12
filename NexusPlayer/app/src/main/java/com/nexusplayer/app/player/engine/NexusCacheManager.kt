package com.nexusplayer.app.player.engine

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.FileDataSource
import androidx.media3.datasource.cache.CacheDataSink
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.datasource.okhttp.OkHttpDataSource
import okhttp3.OkHttpClient
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * NexusCacheManager: Enterprise-grade streaming caching engine.
 * Wraps ExoPlayer's SimpleCache with LRU eviction (e.g. 2GB max) and OkHttp datasource.
 * Eliminates re-buffering on network video scrubbing (`M3U8`, `MP4`, `DASH`).
 */
@OptIn(UnstableApi::class)
object NexusCacheManager {

    private var simpleCache: SimpleCache? = null
    private const val MAX_CACHE_SIZE_BYTES = 2L * 1024 * 1024 * 1024 // 2 GB limit

    @Synchronized
    fun getSimpleCache(context: Context): SimpleCache {
        return simpleCache ?: synchronized(this) {
            val cacheDir = File(context.cacheDir, "nexus_media_cache").apply { mkdirs() }
            val evictor = LeastRecentlyUsedCacheEvictor(MAX_CACHE_SIZE_BYTES)
            val dbProvider = StandaloneDatabaseProvider(context.applicationContext)
            val cache = SimpleCache(cacheDir, evictor, dbProvider)
            simpleCache = cache
            cache
        }
    }

    fun buildCacheDataSourceFactory(
        context: Context,
        customHeaders: Map<String, String> = emptyMap()
    ): DataSource.Factory {
        val cache = getSimpleCache(context)

        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val requestBuilder = chain.request().newBuilder()
                customHeaders.forEach { (key, valStr) ->
                    requestBuilder.addHeader(key, valStr)
                }
                chain.proceed(requestBuilder.build())
            }
            .build()

        val upstreamFactory = OkHttpDataSource.Factory(okHttpClient)
            .setUserAgent("NexusPlayer/1.0.0 (Android; Media3/1.3.1)")

        val cacheSinkFactory = CacheDataSink.Factory()
            .setCache(cache)
            .setFragmentSize(CacheDataSink.DEFAULT_FRAGMENT_SIZE)

        return CacheDataSource.Factory()
            .setCache(cache)
            .setUpstreamDataSourceFactory(upstreamFactory)
            .setCacheReadDataSourceFactory(FileDataSource.Factory())
            .setCacheWriteDataSinkFactory(cacheSinkFactory)
            .setFlags(CacheDataSource.FLAG_BLOCK_ON_CACHE or CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
    }

    fun clearCache(context: Context) {
        synchronized(this) {
            simpleCache?.release()
            simpleCache = null
            val cacheDir = File(context.cacheDir, "nexus_media_cache")
            cacheDir.deleteRecursively()
        }
    }
}
