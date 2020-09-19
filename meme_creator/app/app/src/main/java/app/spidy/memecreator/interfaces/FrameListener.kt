package app.spidy.memecreator.interfaces

import android.view.View

interface FrameListener {
    fun onViewAdded(view: View)
    fun onRemoveView(view: View?)
    fun onFrameSelected(position: Int)
}