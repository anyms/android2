package app.spidy.tamillovevideostatus.fragments

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.app.ShareCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import app.spidy.hiper.controllers.Caller
import app.spidy.kotlinutils.debug
import app.spidy.kotlinutils.newDialog
import app.spidy.kotlinutils.onUiThread
import app.spidy.kotlinutils.toast
import app.spidy.tamillovevideostatus.R
import app.spidy.tamillovevideostatus.data.Video
import app.spidy.tamillovevideostatus.utils.DownloadUtil
import app.spidy.tamillovevideostatus.utils.Http
import app.spidy.tamillovevideostatus.utils.toSlug
import com.google.android.exoplayer2.DefaultRenderersFactory
import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.MergingMediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.source.TrackGroupArray
import com.google.android.exoplayer2.trackselection.TrackSelectionArray
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.upstream.cache.CacheDataSourceFactory
import com.google.android.exoplayer2.util.Util
import com.google.android.exoplayer2.video.VideoListener
import org.json.JSONObject
import java.io.File
import java.io.RandomAccessFile
import java.lang.Exception
import kotlin.concurrent.thread


class VideoFragment : Fragment() {
    private lateinit var player: SimpleExoPlayer
    private lateinit var playerView: PlayerView
    private lateinit var loadingBar: ProgressBar
    private lateinit var overlayView: FrameLayout
    private lateinit var playerErrorView: TextView
    private lateinit var playIcon: ImageView
    private lateinit var video: Video
    private lateinit var titleView: TextView
    private lateinit var viewCountView: TextView
    private lateinit var downloadCountView: TextView
    private lateinit var shareCountView: TextView
    private lateinit var videoData: JSONObject
    private lateinit var downloadView: LinearLayout
    private lateinit var shareView: LinearLayout

    private var isUrlGenerated = false


    @SuppressLint("SetTextI18n")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val v = inflater.inflate(R.layout.fragment_video, container, false)

        playerView = v.findViewById(R.id.playerView)
        loadingBar = v.findViewById(R.id.loadingBar)
        overlayView = v.findViewById(R.id.overlayView)
        playerErrorView = v.findViewById(R.id.playerErrorView)
        playIcon = v.findViewById(R.id.playIcon)
        titleView = v.findViewById(R.id.titleView)
        viewCountView = v.findViewById(R.id.viewCountView)
        shareCountView = v.findViewById(R.id.shareCountView)
        downloadCountView = v.findViewById(R.id.downloadCountView)
        downloadView = v.findViewById(R.id.downloadView)
        shareView = v.findViewById(R.id.shareView)

        titleView.text = video.title
        viewCountView.text = "${video.viewCount} • views"
        downloadCountView.text = "${video.downloadCount}"
        shareCountView.text = "${video.shareCount}"

        player = SimpleExoPlayer.Builder(
            requireContext(),
            DefaultRenderersFactory(requireContext())
        ).build().apply {
            repeatMode = Player.REPEAT_MODE_ONE
        }

//        player.playWhenReady = true

        playerView.player = player
        debug(video)
        try {
            videoData = JSONObject(video.data)
            debug(videoData)
        } catch (e: Exception) {
            requireContext().toast("Video removed", true)
            requireActivity().finish()
            return null
        }

        initPlayer()

        player.addListener(object : Player.EventListener {
            override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
                if (playbackState == Player.STATE_BUFFERING) {
                    loadingBar.visibility = View.VISIBLE
                    overlayView.visibility = View.VISIBLE
                } else {
                    if (isUrlGenerated) {
                        loadingBar.visibility = View.INVISIBLE
                        overlayView.visibility = View.INVISIBLE
                    }
                }
            }

            override fun onPlayerError(error: ExoPlaybackException) {
                loadingBar.visibility = View.INVISIBLE
                playerErrorView.visibility = View.VISIBLE
                overlayView.visibility = View.VISIBLE
            }
            override fun onTracksChanged(trackGroups: TrackGroupArray, trackSelections: TrackSelectionArray) {}
        })

        player.addVideoListener(object : VideoListener {
            override fun onVideoSizeChanged(width: Int, height: Int, unappliedRotationDegrees: Int, pixelWidthHeightRatio: Float) {
                if (width >= height) {
                    playerView.scaleY = -1f
                    playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                } else {
                    playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                }
            }
        })

        playerView.setOnClickListener {
            player.playWhenReady = !player.playWhenReady
            updatePlayPauseState()
        }


        shareView.setOnClickListener {
            downloadVideo { filePath ->
                val videoFile = File(filePath)
                val videoURI =
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) FileProvider.getUriForFile(
                        requireContext(),
                        "${requireContext().packageName}.fileprovider",
                        videoFile
                    ) else Uri.fromFile(videoFile)
                ShareCompat.IntentBuilder.from(requireActivity())
                    .setStream(videoURI)
                    .setType("video/mp4")
                    .setChooserTitle("Share video...")
                    .startChooser()
                Http.async.get("${Http.apiUrl}/update/share_count/${video.videoId}?key=${Http.apiKey}").then {
                    debug("download_count: ${video.videoId}")
                    debug("download_count: $it")
                }.catch()
            }
        }

        val downloadManager = requireContext().getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        downloadView.setOnClickListener {
            val view = LayoutInflater.from(requireContext()).inflate(R.layout.layout_download_dialog, null)
            val checkingDialog = requireContext().newDialog().withCustomView(view)
                .withTitle("Checking...")
                .withCancelable(false)
                .withPositiveButton(getString(R.string.cancel)) { dialog ->
                    dialog.dismiss()
                }
                .create()
            checkingDialog.show()
            getFallbackUrl { url ->
                checkingDialog.dismiss()
                val request = DownloadManager.Request(Uri.parse(url))
                request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI or DownloadManager.Request.NETWORK_MOBILE)
                request.setAllowedOverRoaming(false)
                request.setTitle("${video.title.toSlug()}.mp4")
                request.setDestinationInExternalPublicDir(
                    Environment.DIRECTORY_DOWNLOADS,
                    "${File.separator}VidStatus${File.separator}${video.title.toSlug()}.mp4"
                )
                downloadManager.enqueue(request)

                Http.async.get("${Http.apiUrl}/update/download_count/${video.videoId}?key=${Http.apiKey}").then {
                    debug("download_count: ${video.videoId}")
                    debug("download_count: $it")
                }.catch()
            }
            requireContext().toast("Downloading... ${video.title}")
        }

        return v
    }

    @SuppressLint("SetTextI18n")
    private fun downloadVideo(callback: (outPath: String) -> Unit) {
        val view = LayoutInflater.from(requireContext()).inflate(R.layout.layout_download_dialog, null)
        val titleView: TextView = view.findViewById(R.id.titleView)
        val progressBar: ProgressBar = view.findViewById(R.id.progressBar)

        val dir = requireContext().getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)!!.absolutePath
        val videoPath = "$dir${File.separator}v.mp4"

        val checkingDialog = requireContext().newDialog()
            .withCustomView(LayoutInflater.from(requireContext()).inflate(R.layout.layout_download_dialog, null))
            .withTitle("Checking...")
            .withCancelable(false)
            .withPositiveButton(getString(R.string.cancel)) { dialog ->
                dialog.dismiss()
            }
            .create()
        checkingDialog.show()

        getFallbackUrl { videoUrl ->
            checkingDialog.dismiss()
//            val videoUrl = videoData.getString("fallback")
//            val videoPath = "$dir${File.separator}v.webm"
//            val audioPath = "$dir${File.separator}a.m4a"
//            val outPath = "$dir${File.separator}${video.title.toSlug()}.mp4"
//            val videoUrl = getMediumQuality()
//            val audioUrl = videoData.getString("audio")
            val resp: Caller
            var progressDialog: AlertDialog? = null

            File(videoPath).also {
                if (it.exists()) it.delete()
            }

            titleView.text = video.title
            resp = download(videoUrl, videoPath, progressBar) {
//            if (isSave) {
//                IO.copyToSdCard(requireContext(), File(outPath), Environment.DIRECTORY_DOWNLOADS, mimeType = "video/mp4")
//            }
                callback(videoPath)
                progressDialog?.dismiss()
            }

            progressDialog = requireContext().newDialog().withCustomView(view)
                .withTitle(getString(R.string.downloading))
                .withCancelable(false)
                .withPositiveButton(getString(R.string.cancel)) { dialog ->
                    resp.cancel()
                    dialog.dismiss()
                }
                .create()
            progressDialog.show()
        }
    }

    private fun download(videoUrl: String, path: String, progressBar: ProgressBar, callback: () -> Unit): Caller {
        val destination = RandomAccessFile(path, "rw")
        destination.seek(0)
        return Http.async.get(videoUrl, isStream = true)
            .then { res ->
                progressBar.isIndeterminate = false
                val total = res.headers.get("content-length")!!.toLong()
                var current = 0f
                val buffer = ByteArray(1024 * 4)
                var read: Int
                while (res.stream!!.read(buffer).also { read = it } != -1) {
                    destination.write(buffer, 0, read)
                    debug(read)
                    current += read
                    onUiThread {
                        progressBar.progress = (current / total * 100).toInt()
                    }
                }
                destination.close()
                res.stream?.close()

                onUiThread(callback)
            }
            .catch {
                debug("Error: $it")
                onUiThread {
                    requireContext().newDialog().withTitle("Network Error!")
                        .withMessage("Please check your internet connection.")
                        .withPositiveButton(getString(R.string.cancel)) { dialog ->
                            dialog.dismiss()
                        }
                        .withCancelable(false)
                        .create()
                        .show()
                }
            }
    }

    private fun initPlayer() {
//        val dataSource = DefaultHttpDataSourceFactory(
//            Util.getUserAgent(requireContext(), getString(R.string.app_name)),
//            null /* listener */,
//            DefaultHttpDataSource.DEFAULT_CONNECT_TIMEOUT_MILLIS,
//            DefaultHttpDataSource.DEFAULT_READ_TIMEOUT_MILLIS,
//            true /* allowCrossProtocolRedirects */
//        )
        val dataSource = DefaultDataSourceFactory(
            requireContext(),
            Util.getUserAgent(requireContext(), getString(R.string.app_name))
        )
        val cacheDataSourceFactory =
            CacheDataSourceFactory(DownloadUtil.getRecentCache(requireContext()), dataSource)
        isUrlGenerated = false
        getMediumQuality { videoUrl, isNative ->
            isUrlGenerated = true
            debug("URL: $videoUrl")
            debug("isNative: $isNative")
            val source = if (!isNative) {
                val audioUrl = videoData.getString("audio")

                val videoSource = ProgressiveMediaSource.Factory(cacheDataSourceFactory)
                    .createMediaSource(Uri.parse(videoUrl))
                val audioSource = ProgressiveMediaSource.Factory(cacheDataSourceFactory)
                    .createMediaSource(Uri.parse(audioUrl))
                val mergedSource = MergingMediaSource(videoSource, audioSource)
                mergedSource
            } else {
                ProgressiveMediaSource.Factory(cacheDataSourceFactory)
                    .createMediaSource(Uri.parse(videoUrl))
            }

            player.prepare(source)

            thread {
                Thread.sleep(10 * 1000)
                onUiThread {
                    if (player.playWhenReady) {
                        Http.async.get("${Http.apiUrl}/update/view_count/${video.videoId}?key=${Http.apiKey}").then {
                            debug("ViewCount: ${video.videoId}")
                            debug("ViewCount: $it")
                        }.catch()
                    }
                }
            }
        }
    }

    private fun updatePlayPauseState() {
        if (player.playWhenReady) {
            overlayView.visibility = View.INVISIBLE
            playIcon.visibility = View.INVISIBLE
        } else {
            overlayView.visibility = View.VISIBLE
            playIcon.visibility = View.VISIBLE
        }
    }

    private fun getLowQuality(): String {
        val quals = videoData.getJSONArray("qualities")
        var minQual = 999999
        var url = ""
        for (i in 0 until quals.length()) {
            val curQual = quals.getJSONObject(i).getString("label").replace("p", "").toInt()
            if (curQual < minQual) {
                minQual = curQual
                url = quals.getJSONObject(i).getString("url")
            }
        }
        return url
    }

    private fun getFallbackUrl(callback: (url: String) -> Unit) {
        Http.async.head(videoData.getString("fallback")).then {
            onUiThread {
                if (it.isSuccessful) {
                    callback(videoData.getString("fallback"))
                } else {
                    callback("https://www.suyambu.net/api/v1/vidstatus/play/${video.videoId}?key=${Http.apiKey}")
                }
            }
        }.catch {
            onUiThread {
                callback("https://www.suyambu.net/api/v1/vidstatus/play/${video.videoId}?key=${Http.apiKey}")
            }
        }
    }

    private fun getMediumQuality(callback: (url: String, isNative: Boolean) -> Unit) {
        val quals = videoData.getJSONArray("qualities")
        var url = ""
        for (i in 0 until quals.length()) {
            val curQual = quals.getJSONObject(i).getString("label").replace("p", "").toInt()
            if (curQual == 480) {
                url = quals.getJSONObject(i).getString("url")
                break
            }
        }
        if (url == "") {
            for (i in 0 until quals.length()) {
                val curQual = quals.getJSONObject(i).getString("label").replace("p", "").toInt()
                if (curQual == 360) {
                    url = quals.getJSONObject(i).getString("url")
                    break
                }
            }
        }

        if (url == "") {
            url = getLowQuality()
        }

        Http.async.head(url).then {
            onUiThread {
                if (it.isSuccessful) {
                    callback(url, false)
                } else {
                    callback("https://www.suyambu.net/api/v1/vidstatus/play/${video.videoId}?key=${Http.apiKey}", true)
                }
            }
        }.catch {
            callback("https://www.suyambu.net/api/v1/vidstatus/play/${video.videoId}?key=${Http.apiKey}", true)
        }
    }

    private fun getHighQuality(): String {
        val quals = videoData.getJSONArray("qualities")
        var minQual = 0
        var url = ""
        for (i in 0 until quals.length()) {
            val curQual = quals.getJSONObject(i).getString("label").replace("p", "").toInt()
            if (curQual > minQual) {
                minQual = curQual
                url = quals.getJSONObject(i).getString("url")
            }
        }
        return url
    }

    fun setVideo(video: Video) {
        this.video = video
    }

    override fun onDestroy() {
        playerView.player = null
        player.release()
        super.onDestroy()
    }

    override fun onResume() {
        player.playWhenReady = true
        updatePlayPauseState()
        super.onResume()
    }

    override fun onPause() {
        player.playWhenReady = false
        updatePlayPauseState()
        super.onPause()
    }
}