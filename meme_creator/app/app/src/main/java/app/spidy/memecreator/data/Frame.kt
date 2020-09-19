package app.spidy.memecreator.data

import android.graphics.Bitmap
import android.view.View
import android.widget.FrameLayout

data class Frame(
    val bitmap: Bitmap,
    val views: ArrayList<View> = arrayListOf()
)