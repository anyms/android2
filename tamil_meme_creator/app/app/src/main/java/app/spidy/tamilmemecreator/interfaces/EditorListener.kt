package app.spidy.tamilmemecreator.interfaces

import android.graphics.Bitmap

interface EditorListener {
    fun onSuccess(bitmap: Bitmap)
    fun onFail()
}