package app.spidy.pirum.interfaces

import com.google.android.exoplayer2.offline.Download

interface DownloadListener {
    fun onProgress(downloads: MutableList<Download>)
}