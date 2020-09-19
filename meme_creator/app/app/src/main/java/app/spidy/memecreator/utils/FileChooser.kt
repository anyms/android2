package app.spidy.memecreator.utils

import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import app.spidy.memecreator.activities.FileChooserActivity
import java.io.IOException

class FileChooser(private val activity: AppCompatActivity) {
    companion object {
        const val RESULT_CODE_NEW = 51
        const val RESULT_CODE_UPDATE = 52
        const val RESULT_CODE_UPDATE_EDITOR_IMAGE = 53
        const val RESULT_FILE_CHOOSER = 54
        const val RESULT_GIF_CHOOSER = 55
    }

    fun choose(requestCode: Int) {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = if (requestCode == RESULT_GIF_CHOOSER) {
            "image/gif"
        } else {
            "image/*"
        }
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true)
        activity.startActivityForResult(intent, requestCode)
    }

    fun open(requestCode: Int = RESULT_CODE_NEW) {
        val intent = Intent(activity, FileChooserActivity::class.java)
        activity.startActivityForResult(intent, requestCode)
    }

    fun read(uri: Uri): ByteArray {
        val inputStream = activity.contentResolver.openInputStream(uri)
            ?: throw IOException("Unable to obtain input stream from URI")
        return inputStream.readBytes()
    }
}