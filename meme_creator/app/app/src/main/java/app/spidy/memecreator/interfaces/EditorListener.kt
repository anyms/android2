package app.spidy.memecreator.interfaces

import android.graphics.Bitmap

interface EditorListener {
    fun onSuccess(bitmap: Bitmap)
    fun onFail()
}