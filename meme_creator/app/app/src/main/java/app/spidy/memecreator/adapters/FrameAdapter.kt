package app.spidy.memecreator.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import app.spidy.memecreator.R
import app.spidy.memecreator.data.Frame

class FrameAdapter(
    private val context: Context,
    private val frames: List<Frame>,
    private val listener: Listener
): RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private var currentPosition = 0
    private var lastFrameView: ConstraintLayout? = null
    var isSelectMode = false
    val selectedPositions = ArrayList<Int>()
    private val overlays = arrayOfNulls<View>(frames.size)
    var isAllSelected = false

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val v = LayoutInflater.from(context).inflate(R.layout.layout_frame_item, parent, false)
        return MainHolder(v)
    }

    override fun getItemCount(): Int = frames.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val mainHolder = holder as MainHolder
        mainHolder.frameImageView.setImageBitmap(frames[position].bitmap)
        overlays[position] = mainHolder.overlayView
        mainHolder.rootView.setOnClickListener {
            if (isSelectMode) {
                handleSelect(mainHolder, position)
            } else {
                lastFrameView?.setBackgroundColor(
                    ContextCompat.getColor(
                        context,
                        R.color.colorToolbar
                    )
                )
                mainHolder.rootView.setBackgroundColor(
                    ContextCompat.getColor(
                        context,
                        R.color.colorAccent
                    )
                )
                currentPosition = position
                lastFrameView = mainHolder.rootView
                listener.onFrameSelected(currentPosition)
            }
        }

        if (currentPosition == position) {
            mainHolder.rootView.setBackgroundColor(ContextCompat.getColor(context, R.color.colorAccent))
            lastFrameView = mainHolder.rootView
        } else {
            mainHolder.rootView.setBackgroundColor(ContextCompat.getColor(context, R.color.colorToolbar))
        }

        mainHolder.rootView.setOnLongClickListener {
            if (isSelectMode) {
                handleSelect(mainHolder, position)
            } else {
                mainHolder.overlayView.visibility = View.VISIBLE
                enterSelectMode()
                selectedPositions.add(position)
            }
            return@setOnLongClickListener true
        }

        if (position in selectedPositions) {
            mainHolder.overlayView.visibility = View.VISIBLE
        } else {
            mainHolder.overlayView.visibility = View.INVISIBLE
        }
    }

    private fun handleSelect(mainHolder: MainHolder, position: Int) {
        if (position in selectedPositions) {
            mainHolder.overlayView.visibility = View.INVISIBLE
            selectedPositions.remove(position)
        } else {
            mainHolder.overlayView.visibility = View.VISIBLE
            selectedPositions.add(position)
        }

        isAllSelected = selectedPositions.size == frames.size
        listener.onFrameSelected(-1)

        if (selectedPositions.isEmpty()) {
            exitSelectMode()
        }
    }

    private fun enterSelectMode() {
        isSelectMode = true
        listener.onFrameSelected(-1)
    }

    fun exitSelectMode() {
        deselectAll()
        isSelectMode = false
        listener.onExitSelectMode()
    }

    fun selectAll() {
        overlays.forEach {
            it?.visibility = View.VISIBLE
        }
        selectedPositions.clear()
        for (i in frames.indices) {
            selectedPositions.add(i)
        }
    }

    fun deselectAll() {
        overlays.forEach {
            it?.visibility = View.INVISIBLE
        }
        selectedPositions.clear()
    }


    inner class MainHolder(v: View) : RecyclerView.ViewHolder(v) {
        val frameImageView: ImageView = v.findViewById(R.id.frameImageView)
        val rootView: ConstraintLayout = v.findViewById(R.id.rootView)
        val overlayView: View = v.findViewById(R.id.overlayView)
    }

    interface Listener {
        fun onFrameSelected(position: Int)
        fun onExitSelectMode()
    }
}