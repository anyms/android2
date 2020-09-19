package app.spidy.oli.utils

import com.google.android.exoplayer2.offline.Download

object DownloadStatus {
    const val STATE_QUEUED = Download.STATE_QUEUED
    const val STATE_STOPPED = Download.STATE_STOPPED
    const val STATE_DOWNLOADING = Download.STATE_DOWNLOADING
    const val STATE_COMPLETED = Download.STATE_COMPLETED
    const val STATE_FAILED = Download.STATE_FAILED
    const val STATE_REMOVING = Download.STATE_REMOVING
    const val STATE_RESTARTING = Download.STATE_RESTARTING
    const val STATE_NONE = 101
//
//    const val FAILURE_REASON_NONE = Download.STATE_RESTARTING
//    const val FAILURE_REASON_UNKNOWN = Download.FAILURE_REASON_UNKNOWN
//    const val STOP_REASON_NONE = Download.STOP_REASON_NONE


}