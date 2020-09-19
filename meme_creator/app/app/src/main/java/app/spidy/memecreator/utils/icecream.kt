package app.spidy.memecreator.utils

import android.app.AlertDialog
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.MimeTypeMap
import android.widget.TextView
import app.spidy.memecreator.R
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.security.MessageDigest
import java.util.*


fun String.toMD5(): String {
    var result = ""
    val md5 = MessageDigest.getInstance("MD5")
    val hash = md5.digest(this.toByteArray()).toTypedArray()

    for (b in hash) {
        val c = String.format("%2X", b).replace(" ", "0")
        result += (c)
    }
    result.dropLast(1)

    return result.toLowerCase(Locale.ROOT)
}

fun Context.showLoading(s: String): AlertDialog {
    val builder: AlertDialog.Builder = AlertDialog.Builder(this)
    builder.setCancelable(false)
    val viewGroup: ViewGroup? = null
    val view = LayoutInflater.from(this).inflate(R.layout.dialog_loading, viewGroup, false)
    view.findViewById<TextView>(R.id.messageView).text = s
    builder.setView(view)
    val dialog = builder.create()
    dialog.show()
    return dialog
}


fun View.getBitmap(): Bitmap {
    val bitmap = Bitmap.createBitmap(this.width, this.height, Bitmap.Config.ARGB_8888)
    // Give a bit of time to generate the bitmap, if I don't add a delay it throws an IndexOutOfBoundException
    // In this delay time show the user a nice loading dialog, saying that your image is being generated.
    val paint = Paint()
    paint.color = Color.TRANSPARENT
    val canvas = Canvas(bitmap)
    canvas.drawBitmap(bitmap, 0f, 0f, paint)
    this.draw(canvas)
    return bitmap
}

fun getMimeType(fileName: String): String? {
    var type: String? = null
    val extension = fileName.split(".").last()
    return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
}


fun Context.saveFile(content: ByteArray) {
    val fos: OutputStream?

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val resolver = contentResolver;
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, "meme_${UUID.randomUUID()}")
            put(MediaStore.MediaColumns.MIME_TYPE, "image/gif")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) { //this one
                put(MediaStore.MediaColumns.RELATIVE_PATH, "DCIM/")
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
        }
        val imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);
        fos = resolver.openOutputStream(imageUri!!)
    } else {
        val imagesDir = Environment.getExternalStoragePublicDirectory(
            Environment.DIRECTORY_DCIM).toString()
        val file = File(imagesDir)
        if (!file.exists()) {
            file.mkdir()
        }
        val image = File(imagesDir, "meme_${UUID.randomUUID()}.gif");
        fos = FileOutputStream(image)

    }
    fos?.write(content)
    fos?.flush()
    fos?.close()
}


