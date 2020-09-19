package app.spidy.tamillovevideostatus.adapters

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import app.spidy.kotlinutils.onUiThread
import app.spidy.tamillovevideostatus.data.Video
import app.spidy.tamillovevideostatus.fragments.VideoFragment

class VideoPagerAdapter(fragmentManager: FragmentManager, lifecycle: Lifecycle): FragmentStateAdapter(fragmentManager, lifecycle) {
    private val videos = ArrayList<Video>()
    var fragment: VideoFragment? = null

    override fun getItemCount(): Int = videos.size
    override fun createFragment(position: Int): Fragment {
        fragment = VideoFragment()
        fragment?.setVideo(videos[position])
        return fragment!!
    }

    override fun getItemId(position: Int): Long {
        return videos[position].uId
    }

    override fun containsItem(itemId: Long): Boolean {
        return videos.map { it.uId }.contains(itemId)
    }

    fun add(vid: Video) {
        videos.add(vid)
        onUiThread {
            notifyItemInserted(videos.size - 1)
        }
    }

    fun add(vids: List<Video>) {
        var startPos = videos.size - 1
        if (startPos < 0) startPos = 0
        for (vid in vids) videos.add(vid)
        onUiThread {
            notifyItemRangeChanged(startPos, vids.size)
        }
    }
}