package app.spidy.oli.utils

import android.content.Context
import app.spidy.oli.R
import com.google.android.exoplayer2.database.ExoDatabaseProvider
import com.google.android.exoplayer2.offline.DownloadManager
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.upstream.cache.Cache
import com.google.android.exoplayer2.upstream.cache.LeastRecentlyUsedCacheEvictor
import com.google.android.exoplayer2.upstream.cache.NoOpCacheEvictor
import com.google.android.exoplayer2.upstream.cache.SimpleCache
import com.google.android.exoplayer2.util.Util
import java.io.File

object DownloadUtil {
    private var downloadedCache: Cache? = null
    private var recentCache: Cache? = null
    private var downloadManager: DownloadManager? = null

    @Synchronized fun getDownloadedCache(context: Context): Cache {
        if (downloadedCache == null) {
            val databaseProvider = ExoDatabaseProvider(context)
            val cacheDirectory = File(context.getExternalFilesDir(null), "downloads")
            downloadedCache = SimpleCache(cacheDirectory, NoOpCacheEvictor(), databaseProvider)
        }
        return downloadedCache!!
    }

    @Synchronized fun getRecentCache(context: Context): Cache {
        if (recentCache == null) {
            val databaseProvider = ExoDatabaseProvider(context)
            val cacheDirectory = File(context.getExternalFilesDir(null), "cache")
            recentCache = SimpleCache(cacheDirectory, LeastRecentlyUsedCacheEvictor(1024 * 1024 * 10), databaseProvider)
        }
        return recentCache!!
    }

    @Synchronized fun getDownloadManager(context: Context): DownloadManager {
        if (downloadManager == null) {
            val databaseProvider = ExoDatabaseProvider(context)
            downloadManager = DownloadManager(
                context,
                databaseProvider,
                getDownloadedCache(context),
                DefaultDataSourceFactory(context, Util.getUserAgent(context, context.getString(R.string.app_name)))
            )
        }
        return downloadManager!!
    }
}